/* Copyright 2016 Albin Severinson
 * Rendits vehicle router
 * Broadcasts incoming local messages
 * Forwards incoming GeoNetworking messages to all subscribers.
 */

package com.rendits.router;

/* Standard Java */
import java.io.IOException;
import java.io.FileInputStream;
import java.nio.BufferOverflowException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    public final static int MAX_UDP_LENGTH = 600;

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
    public static int vehicle_cam_port = 5000;
    public static int vehicle_denm_port = 5000;
    public static int vehicle_iclcm_port = 5000;
    public static InetAddress vehicle_address;

    /* Thread pool for all workers handling incoming/outgoing messages */
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /* True while the threads should be running */
    private static volatile boolean running = true;

    public Router(Properties props) throws IOException {

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

        /*
         * TODO: Race conditions in the beaconing service is causing it to send
         * too many beacons. Turn off until it's fixed. We don't need the
         * beaconing service for anything we're using it for anyway as it's
         * supposed to be quiet when sending other traffic.
         */
        /*
         * TODO: Thread crashes when attempting to send when the beaconing
         * service isn't running.
         */
        station.startBecon();

        /* Start the BTP socket */
        btpSocket = BtpSocket.on(station);

        /* Start the loops that handle sending and receiving messages */
        // TODO: Run a few threads of each. First investigate if
        // BtpSocket.receive() if thread-safe.
        executor.submit(receiveFromVehicle);
        executor.submit(receiveFromVehicle);
        executor.submit(receiveFromVehicle);        
        executor.submit(sendToVehicle);

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

        /* Give the threads 5 seconds before shutting down forcefully. */
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch(InterruptedException e){
            logger.error("Router interrupted during shutdown:", e);
        }
        executor.shutdownNow();

        logger.info("Router closed");
    }

    /* For keeping track of the current vehicle position. Used for the
     * broadcasting service and for generating Geonetworking addresses.
     */
    public static VehiclePositionProvider vehiclePositionProvider;
    
    /* Statistics logging class */
    private StatsLogger statsLogger;    
    private class StatsLogger {
        private int txCam = 0;
        private int rxCam = 0;
        private int txDenm = 0;
        private int rxDenm = 0;
        private int txIclcm = 0;
        private int rxIclcm = 0;
        private int txCustom = 0;
        private int rxCustom = 0;

        StatsLogger(ExecutorService executor) {
            executor.submit(logStats);
        }

        public void incTxCam(){
            this.txCam++;
        }
        
        public void incRxCam(){
            this.rxCam++;
        }
        
        public void incTxDenm(){
            this.txDenm++;
        }
        
        public void incRxDenm(){
            this.rxDenm++;
        }
        
        public void incTxIclcm(){
            this.txIclcm++;
        }
        
        public void incRxIclcm(){
            this.rxIclcm++;
        }

        public void incTxCustom(){
            this.txCustom++;
        }
        
        public void incRxCustom(){
            this.rxCustom++;
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
                    System.out.println("#### Rendits Vehicle Router ####" + "\nListening on port " + rcvSocket.getLocalPort()
                                       + "\nVehicle Control System IP is " + vehicle_address + "\nSending incoming CAM to port "
                                       + vehicle_cam_port + "\nSending incoming DENM to port " + vehicle_denm_port
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
                        logger.info("#CAM (Tx/Rx): {}/{} | #DENM (Tx/Rx): {}/{} | #iCLCM (Tx/Rx): {}/{} | #Custom (Tx/Rx): {}/{}",
                                    txCam, rxCam,
                                    txDenm, rxDenm,
                                    txIclcm, rxIclcm,
                                    txCustom, rxCustom);
                    }
                }
             };
    }
        
    /* Parse and forward a local simple message.
     */
    // TODO: Replace local with simple
    private void properFromSimple(byte[] buffer) {
        switch(buffer[0]){
        case MessageId.cam: {
            try{                
                LocalCam localCam = new LocalCam(buffer);
                Cam cam = localCam.asCam();
                send(cam);
                statsLogger.incTxCam();

                /* Use the data in the CAM to update the locally
                 * stored vehicle position. Used when receiving
                 * messages and generating adresses.
                 */
                double latitude = (double) localCam.latitude;
                latitude /= 1e7;

                double longitude = (double) localCam.longitude;
                longitude /= 1e7;

                vehiclePositionProvider.update(latitude, longitude);
            } catch(IllegalArgumentException e) {
                logger.error("Irrecoverable error when creating CAM. Ignoring message.", e);
            }
            break;
        }

        case MessageId.denm: {
            try {
                LocalDenm localDenm = new LocalDenm(buffer);
                Denm denm = localDenm.asDenm();

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
                LocalIclcm localIclcm = new LocalIclcm(buffer);
                IgameCooperativeLaneChangeMessage iclcm = localIclcm.asIclcm();
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
    
    /* Receive local messages from the control system, parse them into
     * the proper message (CAM/DENM/iCLCM/custom) and forward to the
     * link layer.
     */
    private Runnable receiveFromVehicle = new Runnable() {
            private final byte[] buffer = new byte[MAX_UDP_LENGTH];
            private final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            @Override
            public void run() {
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
            }
        };

    private void simpleFromProper(BtpPacket btpPacket, DatagramPacket packet){
        switch(btpPacket.destinationPort()) {
        case PORT_CAM: {
            try {
                Cam cam = UperEncoder.decode(btpPacket.payload(), Cam.class);
                LocalCam localCam = new LocalCam(cam);
                byte[] buffer = localCam.asByteArray();
                packet.setData(buffer, 0, buffer.length);
                packet.setPort(vehicle_cam_port);                

                try {
                    rcvSocket.send(packet);
                    statsLogger.incRxCam();
                } catch(IOException e) {
                    logger.warn("Failed to send CAM to vehicle", e);
                }
            } catch(NullPointerException | IllegalArgumentException | UnsupportedOperationException | BufferOverflowException  e) {
                logger.warn("Couldn't decode CAM:", e);
            }
            break;
        }

        case PORT_DENM: {
            try {
                Denm denm = UperEncoder.decode(btpPacket.payload(), Denm.class);
                LocalDenm localDenm = new LocalDenm(denm);
                byte[] buffer = localDenm.asByteArray();
                packet.setData(buffer, 0, buffer.length);
                packet.setPort(vehicle_denm_port);

                try {
                    rcvSocket.send(packet);
                    statsLogger.incRxDenm();
                } catch(IOException e) {
                    logger.warn("Failed to send DENM to vehicle", e);
                }
            } catch(NullPointerException | IllegalArgumentException | UnsupportedOperationException | BufferOverflowException e) {
                logger.warn("Couldn't decode DENM:", e);
            }
            break;
        }

        case PORT_ICLCM: {
            try {
                IgameCooperativeLaneChangeMessage iclcm = UperEncoder.decode(btpPacket.payload(),
                                                                             IgameCooperativeLaneChangeMessage.class);
                LocalIclcm localIclcm = new LocalIclcm(iclcm);
                byte[] buffer = localIclcm.asByteArray();
                packet.setData(buffer, 0, buffer.length);
                packet.setPort(vehicle_iclcm_port);

                try {
                    rcvSocket.send(packet);
                    statsLogger.incRxIclcm();
                } catch(IOException e) {
                    logger.warn("Failed to send iCLCM to vehicle", e);
                }
            } catch(NullPointerException | IllegalArgumentException | UnsupportedOperationException | BufferOverflowException e) {
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
                packet.setAddress(vehicle_address);
                try{
                    while(running) {
                        BtpPacket btpPacket = btpSocket.receive();
                        simpleFromProper(btpPacket, packet);
                    }
                } catch(InterruptedException e) {
                    logger.warn("BTP socket interrupted during receive");
                }
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
            return new LongPositionVector(address, Instant.now(), position, isPositionConfident, speedMetersPerSecond,
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

    /* Time to get the ball rolling! */
    // TODO: Allow loading custom config via cli
    public static void main( String[] args) throws IOException {

        /* Load properties from file */
        Properties props = new Properties();
        FileInputStream in = new FileInputStream("router.properties");
        props.load(in);
        in.close();        

        /* Start the router */
        Router router = new Router(props);

        /* Start a thread waiting for configuration changes over the
         * network.
         */

    }
}
/* That's all folks! */
