/* Copyright 2016 Albin Severinson
 * Test suite for the Rendits vehicle router. Includes test for
 * latency, packet loss, duplicates, etc.
 */

package com.rendits.testsuite;

/* Standard Java */
import java.io.IOException;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ConcurrentHashMap;

/* Logging framework */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Simple message classes */
import com.rendits.router.SimpleCam;
import com.rendits.router.SimpleDenm;
import com.rendits.router.SimpleIclcm;

public class TestSuite {
        private final static Logger logger = LoggerFactory.getLogger(TestSuite.class);
        private final DatagramSocket rcvSocket;
        private static final ExecutorService executor = Executors.newCachedThreadPool();
        private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

        private final int MAX_UDP_SIZE = 300;
        private int stationID = 100;

    TestSuite(Properties props) throws IOException {
        logger.info("Starting test suite...");

        int portSendCam = Integer.parseInt(props.getProperty("portSendCam"));
        int portSendDenm = Integer.parseInt(props.getProperty("portSendDenm"));
        int portSendIclcm = Integer.parseInt(props.getProperty("portSendIclcm"));
        int portRcvFromRouter = Integer.parseInt(props.getProperty("portRcvFromRouter"));
        InetAddress routerAddress = InetAddress.getByName(props.getProperty("routerAddress"));

        int configPort = Integer.parseInt(props.getProperty("routerConfigPort"));

        rcvSocket = new DatagramSocket(portRcvFromRouter);

        Vehicle vehicle = new Vehicle(stationID);

        ConcurrentHashMap highResTimestamps = new ConcurrentHashMap();
        CamService camService = new CamService(10, rcvSocket, routerAddress,
                                               portSendCam, vehicle, scheduler, highResTimestamps);

        //IclcmService iclcmService = new IclcmService(1000, rcvSocket, routerAddress, portSendIclcm, scheduler);

        ReceiveService receiveService = new ReceiveService(rcvSocket);
        LatencyTest latencyTest = new LatencyTest(receiveService, executor, highResTimestamps);
        latencyTest.start();

        ConfigService configService = new ConfigService(routerAddress, configPort);

        /*
        Properties newProps = new Properties();
        newProps.setProperty("foo", "1");
        newProps.setProperty("bar", "2");
        newProps.setProperty("portSendIclcm", "6001");
        configService.send(newProps);
        */
    }

    /* Service handling connecting to a router and sending config
     * updates.
     */
    private class ConfigService {
        InetAddress routerAddress;
        int configPort;

        ConfigService(InetAddress routerAddress, int configPort) {
            this.routerAddress = routerAddress;
            this.configPort = configPort;
        }

        /* Send a properties object to the router */
        public void send(Properties props) throws IOException {

            /* Open a socket to the router */
            Socket socket = new Socket(routerAddress, configPort);

            /* Create a PrintWriter and send the properties object
             * over it.
             */
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            props.list(out);

            /* Close the socket */
            socket.close();
        }
    }

    /* Service handling receiving packets from the router */
    private class ReceiveService {
        private DatagramSocket rcvSocket;
        private boolean running = true;

        ReceiveService(DatagramSocket rcvSocket) {
            this.rcvSocket = rcvSocket;
        }

        /* Receive a packet from the router */
        public DatagramPacket receive() {
            DatagramPacket packet = new DatagramPacket(new byte[MAX_UDP_SIZE], MAX_UDP_SIZE);
            try{
                rcvSocket.receive(packet);
            }catch(IOException e){
                logger.warn("IOException when receiving packet.");
                return null;
            }
            return packet;
        }
    }

    private class LatencyTest {
        private ReceiveService receiveService;
        private ExecutorService executor;
        private ConcurrentHashMap highResTimestamps;

        private boolean running = true;
        private final int WARMUP_COUNT = 100;
        private final int TEST_LENGTH = 1000;

        LatencyTest(ReceiveService receiveService,
                    ExecutorService executor,
                    ConcurrentHashMap highResTimestamps) {

            this.receiveService = receiveService;
            this.executor = executor;
            this.highResTimestamps = highResTimestamps;
        }

        /* Start the latency test */
        public void start() {

            /* Let the JIT warm up */
            logger.info("Starting latency test warm-up.");
            DatagramPacket packet;
            for(int i = 0; i < WARMUP_COUNT;i++) {
                packet = receiveService.receive();
            }

            /* Start the test proper */
            logger.info("Starting latency test...");
            executor.submit(runner);
        }

        private Runnable runner = new Runnable() {
                @Override
                public void run() {
                    ByteBuffer buffer;
                    long totalDelay = 0;
                    long totalReceived = 0;

                    for(int i = 0; i < TEST_LENGTH; i++) {
                        DatagramPacket packet = receiveService.receive();
                        buffer = ByteBuffer.wrap(packet.getData());

                        switch(packet.getData()[0]) {
                        case 2:
                            if(totalReceived % 100 == 0) {
                                logger.info(totalReceived / (double) TEST_LENGTH * 100 + "% completed...");
                            }

                            SimpleCam simpleCam  = new SimpleCam(packet.getData());

                            /* Calculate high-resolution delay */
                            int lowResTimestamp = simpleCam.getGenerationDeltaTime();
                            if(highResTimestamps.containsKey(lowResTimestamp)) {
                                long nanoTimestamp = (long) highResTimestamps.get(simpleCam.getGenerationDeltaTime());
                                long delay = System.nanoTime() - nanoTimestamp;
                                highResTimestamps.remove(simpleCam.getGenerationDeltaTime());
                                totalDelay += delay;
                                totalReceived++;
                            }
                        }
                    }
                    long averageDelay = totalDelay / totalReceived;
                    double averageDelayMilliSeconds = (double) averageDelay / 1e6;
                    logger.info("Delay test completed. Received: " + TEST_LENGTH +
                                " messages. Average delay: " + averageDelayMilliSeconds + "ms.");
                }
            };
    }

    /* Service for periodically sending iCLCM messages */
    private class IclcmService {
        private int period;
        private DatagramSocket socket;
        private InetAddress routerAddress;
        private int routerPort;

        private SimpleIclcm simpleIclcm;
        private byte[] buffer;

        IclcmService(int period,
                     DatagramSocket socket,
                     InetAddress routerAddress,
                     int routerPort,
                     ScheduledExecutorService scheduler) {

            this.period = period;
            this.socket = socket;
            this.routerAddress = routerAddress;
            this.routerPort = routerPort;

            /* Create a simple iCLCM manually */
            simpleIclcm = new SimpleIclcm(stationID,
                                          (byte) 128, //containerMask
                                          100, //rearAxleLocation
                                          0, //controllerType
                                          1001, //responseTimeConstant
                                          1001, //responseTimeDelay
                                          10, //targetLongAcc
                                          1, //timeHeadway
                                          3, //cruiseSpeed
                                          (byte) 128, //lowFrequencyMask
                                          1, //participantsReady
                                          0, //startPlatoon
                                          1, //endOfScenario
                                          255, //mioID
                                          10, //mioRange
                                          11, //mioBearing
                                          12, //mioRangeRate
                                          3, //lane
                                          0, //forwardID
                                          0, //backwardID
                                          0, //mergeRequest
                                          0, //safeToMerge
                                          1, //flag
                                          0, //flagTail
                                          1, //flagHead
                                          254, //platoonID
                                          100, //distanceTravelledCz
                                          2, //intention
                                          2); //counter

            /* Format it as a byte array */
            this.buffer = simpleIclcm.asByteArray();

            /* Add the runner to the thread pool */
            scheduler.scheduleAtFixedRate(runner, period, period, TimeUnit.MILLISECONDS);

            logger.info("Starting iCLCM service...");
        }

        private Runnable runner = new Runnable() {

                @Override
                public void run() {
                    try{
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        packet.setPort(routerPort);
                        packet.setAddress(routerAddress);
                        socket.send(packet);
                    } catch(IOException e) {
                        logger.error("IOException in iCLCM Service.");
                    }
                }
            };
    }

    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        FileInputStream in = new FileInputStream("testsuite.properties");
        props.load(in);
        in.close();
        TestSuite ts = new TestSuite(props);
    }
}
