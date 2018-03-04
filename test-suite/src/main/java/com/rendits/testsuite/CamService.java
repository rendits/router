/* Copyright 2016 Albin Severinson
 * Service for periodically sending CAM messages
 */

package com.rendits.testsuite;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import com.rendits.router.SimpleCam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamService {
    private final static Logger logger = LoggerFactory.getLogger(CamService.class);

    private int period;
    private DatagramSocket socket;
    private InetAddress routerAddress;
    private int routerPort;

    private Vehicle vehicle;

    private SimpleCam simpleCam;
    private byte[] buffer;

    private ConcurrentHashMap highResTimestamps;

    CamService(int period,
               DatagramSocket socket,
               InetAddress routerAddress,
               int routerPort,
               Vehicle vehicle,
               ScheduledExecutorService scheduler,
               ConcurrentHashMap highResTimestamps) {

        this.period = period;
        this.socket = socket;
        this.routerAddress = routerAddress;
        this.routerPort = routerPort;
        this.vehicle = vehicle;
        this.highResTimestamps = highResTimestamps;

        /* Add the runner to the thread pool */
        scheduler.scheduleAtFixedRate(runner, period, period, TimeUnit.MILLISECONDS);

        logger.info("Starting CAM service...");
    }

    private Runnable runner = new Runnable() {
            @Override
            public void run() {
                try{

                    /* Record a high-res timestamp and map it to
                     * the timestamp included in the message. */
                    long nanoTime = System.nanoTime();
                    int generationDeltaTime = vehicle.getGenerationDeltaTime();
                    highResTimestamps.put(generationDeltaTime, nanoTime);

                    /* Create a simple CAM manually */
                    SimpleCam simpleCam = new SimpleCam(vehicle.getStationID(),
                                                        generationDeltaTime,
                                                        (byte) 128, //containerMask
                                                        5, //stationType
                                                        2, //latitude
                                                        48, //longitude
                                                        0, //semiMajorConfidence
                                                        0, //semiMinorConfidence
                                                        0, //semiMajorOrientation
                                                        400, //altitude
                                                        1, //heading value
                                                        1, //headingConfidence
                                                        0, //speedValue
                                                        1, //speedConfidence
                                                        40, //vehicleLength
                                                        20, //vehicleWidth
                                                        159, //longitudinalAcc
                                                        1, //longitudinalAccConf
                                                        2, //yawRateValue
                                                        1, //yawRateConfidence
                                                        0); //vehicleRole

                    /* Format it as a byte array */
                    byte[] buffer = simpleCam.asByteArray();

                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    packet.setPort(routerPort);
                    packet.setAddress(routerAddress);
                    socket.send(packet);
                } catch(IOException e) {
                    logger.error("IOException in CAM Service.");
                }
            }
        };
}
