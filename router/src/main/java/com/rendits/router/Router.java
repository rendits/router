/* Copyright 2016 Albin Severinson
 * Rendits vehicle router
 * Broadcasts incoming messages
 * Forwards incoming GeoNetworking messages to all subscribers.
 */

package com.rendits.router;

/* Standard Java */
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.BufferOverflowException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Properties;
import java.lang.IllegalArgumentException;

/* ETSI GeoNetworking library */
import net.gcdc.geonetworking.Area;
import net.gcdc.geonetworking.BtpPacket;
import net.gcdc.geonetworking.BtpSocket;
import net.gcdc.geonetworking.Destination;
import net.gcdc.geonetworking.Destination.Geobroadcast;
import net.gcdc.geonetworking.GeonetStation;
import net.gcdc.geonetworking.LinkLayer;
import net.gcdc.geonetworking.MacAddress;
import net.gcdc.geonetworking.Position;
import net.gcdc.geonetworking.PositionProvider;
import net.gcdc.geonetworking.StationConfig;
import net.gcdc.geonetworking.LinkLayerUdpToEthernet;
import net.gcdc.geonetworking.LongPositionVector;
import net.gcdc.geonetworking.Address;
import net.gcdc.geonetworking.StationType;

/* UPER encoder/decoder for ASN.1 */
import net.gcdc.asn1.uper.UperEncoder;

/* CAM/DENM/iCLCM message definitions */
import net.gcdc.camdenm.CoopIts.Cam;
import net.gcdc.camdenm.CoopIts.Denm;
import net.gcdc.camdenm.Iclcm.*;
import net.gcdc.camdenm.CoopIts.ItsPduHeader.MessageId;

/* JSR310 time framework */
import org.threeten.bp.Instant;

/* Logging framework */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Router {
    private final static Logger logger = LoggerFactory.getLogger(Router.class);
    private final Thread stationThread;
    private final GeonetStation station;

    /* Incoming UDP messages */
    private final DatagramSocket rcvSocket;
    private final static int MAX_UDP_LENGTH = 600;

    /* Incoming/outgoing BTP messages */
    private final BtpSocket btpSocket;

    /* BTP ports for CAM/DENM/iCLCM */
    private final static short PORT_CAM = 2001;
    private final static short PORT_DENM = 2002;
    private final static short PORT_ICLCM = 2010;

    /* Message lifetime */
    private final static double CAM_LIFETIME_SECONDS = 0.9;
    private final static double iCLCM_LIFETIME_SECONDS = 0.9;

    /* Default ports */
    private int vehicle_cam_port = 5000;
    private int vehicle_denm_port = 5000;
    private int vehicle_iclcm_port = 5000;
    private InetAddress vehicle_address;

    /* Thread pool for all workers handling incoming/outgoing messages */
    private ExecutorService executor;

    /* For keeping track of the current vehicle position. Used for the
     * broadcasting service and for generating Geonetworking addresses.
     */
    private VehiclePositionProvider vehiclePositionProvider;

    /* True while the threads should be running */
    private volatile boolean running;

    public Router(Properties props) throws IOException {

        /* Set running status to true */
        running = true;

        /* Start the thread pool */
        executor = Executors.newCachedThreadPool();

        /* Create a new config */
        StationConfig config = new StationConfig();

        /* Configure the link layer */
        int localPortForUdpLinkLayer = Integer.parseInt(props.getProperty("localPortForUdpLinkLayer"));
        InetSocketAddress remoteAddressForUdpLinkLayer =
            new SocketAddressFromString(props.getProperty("remoteAddressForUdpLinkLayer")).asInetSocketAddress();
        LinkLayer linkLayer = new LinkLayerUdpToEthernet(localPortForUdpLinkLayer,
                                                         remoteAddressForUdpLinkLayer,
                                                         true);

        /* Configure vehicle address */
        String vehicleAddress = props.getProperty("vehicleAddress");
        vehicle_address = InetAddress.getByName(vehicleAddress);

        /* Router mac address */
        MacAddress senderMac = new MacAddress(props.getProperty("macAddress"));

        /* Configure router address */
        int countryCode = Integer.parseInt(props.getProperty("countryCode"));
        Address address = new Address(true, // isManual,
                                      StationType.values()[5], // 5 for passenger car
                                      countryCode,
                                      senderMac.value());

        /* Create a vehicle position provider */
        vehiclePositionProvider = new VehiclePositionProvider(address);

        /* Set the specified ports */
        vehicle_cam_port = Integer.parseInt(props.getProperty("portSendCam"));
        vehicle_denm_port =  Integer.parseInt(props.getProperty("portSendDenm"));
        vehicle_iclcm_port =  Integer.parseInt(props.getProperty("portSendIclcm"));

        /* Open the receive socket */
        int portRcvFromVehicle = Integer.parseInt(props.getProperty("portRcvFromVehicle"));

        /* Start the GeoNet station */
        rcvSocket = new DatagramSocket(portRcvFromVehicle);
        station = new GeonetStation(config, linkLayer, vehiclePositionProvider, senderMac);
        stationThread = new Thread(station);
        stationThread.start();

        /* Turn on the beaconing service. It transmits beacons while
         * nothing else is transmitting.
         */
        station.startBecon();

        /* Start the BTP socket */
        btpSocket = BtpSocket.on(station);

        /* Start the loops that handle sending and receiving messages */
        int numReceiveThreads = Integer.parseInt(props.getProperty("receiveThreads", "1"));
        assert numReceiveThreads == 1 : "Only a single receive thread is allowed for now.";
        for(int i = 0; i < numReceiveThreads; i++){
            executor.submit(receiveFromVehicle);
        }

        int numSendThreads = Integer.parseInt(props.getProperty("sendThreads", "1"));
        assert numSendThreads > 0;
        for(int i = 0; i < numSendThreads; i++){
            executor.submit(sendToVehicle);
        }

        /* Start thread that handles printing statistics to the log */
        statsLogger = new StatsLogger(executor);
    }

    /* Stop the router */
    public void close() {

        /* Notify all threads to stop running */
        running = false;

        /* Shutdown the GeoNet station */
        station.close();
        stationThread.interrupt();

        /* Close the sockets */
        rcvSocket.close();
        btpSocket.close();

        /* Shutdown the thread pool */
        executor.shutdown();

        /* Give the threads 1 second before shutting down forcefully. */
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch(InterruptedException e){
            logger.error("Router interrupted during shutdown:", e);
        }
        executor.shutdownNow();

        logger.info("Router closed");
    }

    /* Statistics logging class */
    private StatsLogger statsLogger;
    private class StatsLogger {
        private AtomicInteger txCam = new AtomicInteger();
        private AtomicInteger rxCam = new AtomicInteger();
        private AtomicInteger txDenm = new AtomicInteger();
        private AtomicInteger rxDenm = new AtomicInteger();
        private AtomicInteger txIclcm = new AtomicInteger();
        private AtomicInteger rxIclcm = new AtomicInteger();
        private AtomicInteger txCustom = new AtomicInteger();
        private AtomicInteger rxCustom = new AtomicInteger();

        StatsLogger(ExecutorService executor) {
            executor.submit(logStats);
        }

        public void incTxCam(){
            this.txCam.incrementAndGet();
        }

        public void incRxCam(){
            this.rxCam.incrementAndGet();
        }

        public void incTxDenm(){
            this.txDenm.incrementAndGet();
        }

        public void incRxDenm(){
            this.rxDenm.incrementAndGet();
        }

        public void incTxIclcm(){
            this.txIclcm.incrementAndGet();
        }

        public void incRxIclcm(){
            this.rxIclcm.incrementAndGet();
        }

        public void incTxCustom(){
            this.txCustom.incrementAndGet();
        }

        public void incRxCustom(){
            this.rxCustom.incrementAndGet();
        }

        /* Dedicated thread for periodically logging statistics */
        private Runnable logStats = new Runnable() {
                @Override
                public void run() {

                    /* Chill out for a bit to let everything else start
                     * before logging anything.
                     */
                    try{
                        Thread.sleep(1000);
                    } catch(InterruptedException e) {
                        logger.warn("Statistics logger interrupted during sleep.");
                    }

                    /* Print startup message */
                    System.out.println("#### Rendits Vehicle Router ####" + "\nListening on port "
									   + rcvSocket.getLocalPort()
                                       + "\nVehicle Control System IP is " + vehicle_address
									   + "\nSending incoming CAM to port " + vehicle_cam_port
									   + "\nSending incoming DENM to port " + vehicle_denm_port
                                       + "\nSending incoming iCLCM to port " + vehicle_iclcm_port
                                       + "\nCopyright: Albin Severinson (albin@severinson.org)");

                    /* Log statistics every second */
                    while(running) {
                        try{
                            Thread.sleep(1000);
                        } catch(InterruptedException e) {
                            logger.warn("Statistics logger interrupted during sleep.");
                        }

                        /* Log stats */
                        logger.info("#CAM (Tx/Rx): {}/{} "
									+ "| #DENM (Tx/Rx): {}/{} "
									+ "| #iCLCM (Tx/Rx): {}/{} "
									+ "| #Custom (Tx/Rx): {}/{}",
                                    txCam, rxCam,
                                    txDenm, rxDenm,
                                    txIclcm, rxIclcm,
                                    txCustom, rxCustom);
                    }
                }
             };
    }

    /* Parse and forward a simple simple message.
     */
    private void properFromSimple(byte[] buffer) {
        switch(buffer[0]){
        case MessageId.cam: {
            try{
                SimpleCam simpleCam = new SimpleCam(buffer);
                Cam cam = simpleCam.asCam();
                send(cam);
                statsLogger.incTxCam();

                /* Use the data in the CAM to update the locally
                 * stored vehicle position. Used when receiving
                 * messages and generating adresses.
                 */
                double latitude = (double) simpleCam.getLatitude();
                latitude /= 1e7;

                double longitude = (double) simpleCam.getLongitude();
                longitude /= 1e7;

                vehiclePositionProvider.update(latitude, longitude);
            } catch(IllegalArgumentException e) {
                logger.error("Irrecoverable error when creating CAM. Ignoring message.", e);
            }
            break;
        }

        case MessageId.denm: {
            try {
                SimpleDenm simpleDenm = new SimpleDenm(buffer);
                Denm denm = simpleDenm.asDenm();

                /* Simple messages are sent to everyone within range. */
                Position position = vehiclePositionProvider.getPosition();
                Area target = Area.circle(position, Double.MAX_VALUE);
                send(denm, Geobroadcast.geobroadcast(target));
                statsLogger.incTxDenm();

            } catch(IllegalArgumentException e) {
                logger.error("Irrecoverable error when creating DENM. Ignoring message.", e);
            }
            break;
        }

        case net.gcdc.camdenm.Iclcm.MessageID_iCLCM: {
            try {
                SimpleIclcm simpleIclcm = new SimpleIclcm(buffer);
                IgameCooperativeLaneChangeMessage iclcm = simpleIclcm.asIclcm();
                send(iclcm);
                statsLogger.incTxIclcm();

            } catch(IllegalArgumentException e) {
                logger.error("Irrecoverable error when creating iCLCM. Ignoring message.", e);
            }
            break;
        }

        default:
            logger.warn("Received incorrectly formatted message. First byte: {}", buffer[0]);
        }
    }

    /* Receive simple messages from the control system, parse them into
     * the proper message (CAM/DENM/iCLCM/custom) and forward to the
     * link layer.
     */
    private Runnable receiveFromVehicle = new Runnable() {
            private final byte[] buffer = new byte[MAX_UDP_LENGTH];
            private final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            @Override
            public void run() {
                logger.info("Receive thread starting...");
                while(running) {
                    try {
                        rcvSocket.receive(packet);
                        byte[] receivedData = Arrays.copyOfRange(packet.getData(), packet.getOffset(),
                                                                 packet.getOffset() + packet.getLength());

                        // TODO: Replace with checks
                        assert(receivedData.length == packet.getLength());

                        /* Parse data and send forward message */
                        properFromSimple(receivedData);

                    } catch(IOException e) {
                        logger.error("Exception when receiving message from vehicle");

                        /* Sleep for a short time whenever an
                         * IO exception occurs.
                         */
                        try{
                            Thread.sleep(100);
                        } catch(InterruptedException ee) {
                            logger.warn("Interrupted during sleep");
                        }
                    }
                }
                logger.info("Receive thread closing!");
            }
        };

    private void simpleFromProper(byte[] payload, int destinationPort,
								  DatagramPacket packet, DatagramSocket toVehicleSocket){
        switch(destinationPort) {
        case PORT_CAM: {
            try {
                Cam cam = UperEncoder.decode(payload, Cam.class);
                SimpleCam simpleCam = new SimpleCam(cam);
                byte[] buffer = simpleCam.asByteArray();
                packet.setData(buffer, 0, buffer.length);
                packet.setPort(vehicle_cam_port);

                try {
                    toVehicleSocket.send(packet);
                    statsLogger.incRxCam();
                } catch(IOException e) {
                    logger.warn("Failed to send CAM to vehicle", e);
                }
            } catch(NullPointerException
					| IllegalArgumentException
					| UnsupportedOperationException
					| BufferOverflowException  e) {
                logger.warn("Couldn't decode CAM:", e);
            }
            break;
        }

        case PORT_DENM: {
            try {
                Denm denm = UperEncoder.decode(payload, Denm.class);
                SimpleDenm simpleDenm = new SimpleDenm(denm);
                byte[] buffer = simpleDenm.asByteArray();
                packet.setData(buffer, 0, buffer.length);
                packet.setPort(vehicle_denm_port);

                try {
                    toVehicleSocket.send(packet);
                    statsLogger.incRxDenm();
                } catch(IOException e) {
                    logger.warn("Failed to send DENM to vehicle", e);
                }
            } catch(NullPointerException
					| IllegalArgumentException
					| UnsupportedOperationException
					| BufferOverflowException e) {
                logger.warn("Couldn't decode DENM:", e);
            }
            break;
        }

        case PORT_ICLCM: {
            try {
                IgameCooperativeLaneChangeMessage iclcm =
					UperEncoder.decode(payload, IgameCooperativeLaneChangeMessage.class);
                SimpleIclcm simpleIclcm = new SimpleIclcm(iclcm);
                byte[] buffer = simpleIclcm.asByteArray();
                packet.setData(buffer, 0, buffer.length);
                packet.setPort(vehicle_iclcm_port);

                try {
                    toVehicleSocket.send(packet);
                    statsLogger.incRxIclcm();
                } catch(IOException e) {
                    logger.warn("Failed to send iCLCM to vehicle", e);
                }
            } catch(NullPointerException
					| IllegalArgumentException
					| UnsupportedOperationException
					| BufferOverflowException e) {
                logger.warn("Couldn't decode iCLCM:", e);
            }
            break;
        }

        default:
            // fallthrough
        }
    }

    /* Receive incoming CAM/DENM/iCLCM, parse them and foward to
     * vehicle.
     */
    private Runnable sendToVehicle = new Runnable() {
            private final byte[] buffer = new byte[MAX_UDP_LENGTH];
            private final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            @Override
            public void run() {
                logger.info("Send thread starting...");
                packet.setAddress(vehicle_address);
                try{
                    while(running) {
                        BtpPacket btpPacket = btpSocket.receive();
						byte[] payload = btpPacket.payload();
						int destinationPort = btpPacket.destinationPort();
                        simpleFromProper(payload, destinationPort, packet, rcvSocket);
                    }
                } catch(InterruptedException e) {
                    logger.warn("BTP socket interrupted during receive");
                }
                logger.info("Send thread closing!");
            }
        };

    /* Send CAM/DENM/iCLCM. */
    public void send(Cam cam) {
        byte[] bytes;
        try {
            bytes = UperEncoder.encode(cam);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            logger.warn("Failed to encode CAM {}, ignoring", cam, e);
            return;
        }
        BtpPacket packet = BtpPacket.singleHop(bytes, PORT_CAM, CAM_LIFETIME_SECONDS);
        try {
            btpSocket.send(packet);
        } catch (IOException e) {
            logger.warn("failed to send cam", e);
        }
    }

    private void send(Denm denm, Geobroadcast destination) {
        byte[] bytes;
        try {
            bytes = UperEncoder.encode(denm);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            logger.error("Failed to encode DENM {}, ignoring", denm, e);
            return;
        }
        BtpPacket packet = BtpPacket.customDestination(bytes, PORT_DENM, destination);
        try {
            btpSocket.send(packet);
        } catch (IOException e) {
            logger.warn("failed to send denm", e);
        }
    }

    private void send(IgameCooperativeLaneChangeMessage iclcm) {
        byte[] bytes;
        try {
            bytes = UperEncoder.encode(iclcm);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            logger.error("Failed to encode iCLCM {}, ignoring", iclcm, e);
            return;
        }
        BtpPacket packet = BtpPacket.singleHop(bytes, PORT_ICLCM, iCLCM_LIFETIME_SECONDS);
        try {
            btpSocket.send(packet);
        } catch (IOException e) {
            logger.warn("Failed to send iclcm", e);
        }
    }


    /* PositionProvider is used by the beaconing service and for creating the
     * Geobroadcast address used for DENM messages.
     */
    public static class VehiclePositionProvider implements PositionProvider {
        public Address address;
        public Position position;
        public boolean isPositionConfident;
        public double speedMetersPerSecond;
        public double headingDegreesFromNorth;

        VehiclePositionProvider(Address address) {
            this.address = address;
            this.position = new Position(0, 0);
            this.isPositionConfident = false;
            this.speedMetersPerSecond = 0;
            this.headingDegreesFromNorth = 0;
        }

        public void update(double latitude, double longitude) {
            this.position = new Position(latitude, longitude);
            logger.debug("VehiclePositionProvider position updated: {}", this.position);
        }

        public Position getPosition() {
            return position;
        }

        public LongPositionVector getLatestPosition() {
            return new LongPositionVector(address, Instant.now(), position,
										  isPositionConfident, speedMetersPerSecond,
                                          headingDegreesFromNorth);
        }
    }

    /* Create a socket address from a string */
    private static class SocketAddressFromString {
        private final InetSocketAddress address;

        public SocketAddressFromString(final String addressStr) {
            String[] hostAndPort = addressStr.split(":");
            if (hostAndPort.length != 2) {
                throw new IllegalArgumentException("Expected host:port, got " + addressStr);
            }
            String hostname = hostAndPort[0];
            int port = Integer.parseInt(hostAndPort[1]);
            this.address = new InetSocketAddress(hostname, port);
        }

        public InetSocketAddress asInetSocketAddress() {
            return address;
        }
    }

    /* Class dedicated to running the router and listening to
     * configuration changes over the network. Whenever a config
     * change is received, the router is restarted with the new
     * properties.
     */
    private static class RouterRunner implements Runnable {
        Properties props;
        ServerSocket configSocket;

        RouterRunner(Properties props) throws IOException {
            this.props = props;

            /* Setup the config socket used to push config changes
             * over the network
             */
            int configPort = Integer.parseInt(props.getProperty("configPort"));
            configSocket = new ServerSocket(configPort);
        }

        @Override
        public void run() {
            while(true) {
                Router router = null;

                /* Start the router */
                while(router == null){
                    try{
                        router = new Router(this.props);
                    } catch(IOException e) {
                        logger.error("IOException occurred when starting the router:", e);

                        /* Sleep for a while before trying again */
                        try{
                            Thread.sleep(5000);
                        } catch(InterruptedException ie) {
                            logger.warn("RouterRunner interrupted when trying to start router.");
                        }
                    }
                }

                /* Wait for a config change to arrive */
                while(true) {
                    try{
                        Socket clientSocket = configSocket.accept();
                        BufferedReader in = new BufferedReader(
							new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));



                        props.load(in);
                        logger.info("Loaded props.." + props);

                        in.close();
                        clientSocket.close();

                        break;

                    } catch(IOException e) {

                        /* Sleep for a while before trying again */
                        try{
                            Thread.sleep(2000);
                        } catch(InterruptedException ie) {
                            logger.warn("RouterRunner interrupted when waiting for config changes.");
                        }
                    }
                }

                /* Close the router. It will be restarted with the
                 * updated properties in the next iteration. */
                router.close();
            }
        }
    }

    // TODO: Allow loading custom config via cli
    public static void main( String[] args) throws IOException {

        /* Load properties from file */
        Properties props = new Properties();
        FileInputStream in = new FileInputStream("router.properties");
        props.load(in);
        in.close();

        /* Time to get the ball rolling! */
        RouterRunner r = new RouterRunner(props);
        Thread t = new Thread(r);
        t.start();
    }
}
/* That's all folks! */
