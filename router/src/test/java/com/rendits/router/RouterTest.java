package com.rendits.router;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.Properties;
import java.io.IOException;
import net.gcdc.UdpDuplicator;

public class RouterTest extends TestCase {

        public RouterTest(String testName) {
                super(testName);
        }

        public void testIntegrity() throws IOException {
                int portRcvFromVehicle = 5000;
                int portSendIts = 5001;
                int localPortForUdpLinkLayer = 4000;
                int remotePortForUdpLinkLayer = 4001;
                String remoteAddressForUdpLinkLayer = "127.0.0.1";
                String vehicleAddress = "127.0.0.1";

                /* Setup properties */
                Properties props = new Properties();
                props.setProperty("portRcvFromVehicle", "" + portRcvFromVehicle);
                props.setProperty("portSendCam", "" + portSendIts);
                props.setProperty("portSendDenm", "" + portSendIts);
                props.setProperty("portSendIclcm", "" + portSendIts);
                props.setProperty("receiveThreads", "1");
                props.setProperty("sendThreads", "1");
                props.setProperty("vehicleAddress", vehicleAddress);
                props.setProperty("localPortForUdpLinkLayer", "" + localPortForUdpLinkLayer);
                props.setProperty("remoteAddressForUdpLinkLayer", remoteAddressForUdpLinkLayer + ":" + remotePortForUdpLinkLayer);
                props.setProperty("macAddress", "00:00:00:00:00:00");
                props.setProperty("countryCode", "46");

                /* Setup the UDP duplicator */
                UdpDuplicator udpDuplicator = new UdpDuplicator();
                SocketAddress remoteAddress =
                        new InetSocketAddress(vehicleAddress, localPortForUdpLinkLayer);
                udpDuplicator.add(remotePortForUdpLinkLayer, remoteAddress);

                /* Start the router */
                Router router = new Router(props);

                /* Setup sockets */
                DatagramSocket socket = new DatagramSocket(portSendIts);
                socket.setSoTimeout(10000);
                byte[] buffer;
                DatagramPacket packet;
                DatagramPacket rcvPacket;
                InetAddress routerAddress = InetAddress.getByName("127.0.0.1");

                /* Send some messages and make sure we get the same
                 * thing back. */
                for (int i = 0; i < 100; i++) {
                        SimpleCam simpleCam = SampleMessages.getSampleCam();
                        buffer = simpleCam.asByteArray();
                        packet = new DatagramPacket(buffer, buffer.length);
                        rcvPacket = new DatagramPacket(new byte[buffer.length], buffer.length);
                        packet.setPort(portRcvFromVehicle);
                        packet.setAddress(routerAddress);
                        socket.send(packet);
                        socket.receive(rcvPacket);
                        assertEquals(simpleCam, new SimpleCam(rcvPacket.getData()));

                        SimpleDenm simpleDenm = SampleMessages.getSampleDenm();
                        buffer = simpleDenm.asByteArray();
                        packet = new DatagramPacket(buffer, buffer.length);
                        rcvPacket = new DatagramPacket(new byte[buffer.length], buffer.length);
                        packet.setPort(portRcvFromVehicle);
                        packet.setAddress(routerAddress);
                        socket.send(packet);
                        socket.receive(rcvPacket);
                        assertEquals(simpleDenm, new SimpleDenm(rcvPacket.getData()));

                        SimpleIclcm simpleIclcm = SampleMessages.getSampleIclcm();
                        buffer = simpleIclcm.asByteArray();
                        packet = new DatagramPacket(buffer, buffer.length);
                        rcvPacket = new DatagramPacket(new byte[buffer.length], buffer.length);
                        packet.setPort(portRcvFromVehicle);
                        packet.setAddress(routerAddress);
                        socket.send(packet);
                        socket.receive(rcvPacket);
                        assertEquals(simpleIclcm, new SimpleIclcm(rcvPacket.getData()));
                }

                /* Stop the router */
                router.close();

                /* Start it again and make sure everything still works. */
                router = new Router(props);
                for (int i = 0; i < 100; i++) {
                        SimpleCam simpleCam = SampleMessages.getSampleCam();
                        buffer = simpleCam.asByteArray();
                        packet = new DatagramPacket(buffer, buffer.length);
                        rcvPacket = new DatagramPacket(new byte[buffer.length], buffer.length);
                        packet.setPort(portRcvFromVehicle);
                        packet.setAddress(routerAddress);
                        socket.send(packet);
                        socket.receive(rcvPacket);
                        assertEquals(simpleCam, new SimpleCam(rcvPacket.getData()));

                        SimpleDenm simpleDenm = SampleMessages.getSampleDenm();
                        buffer = simpleDenm.asByteArray();
                        packet = new DatagramPacket(buffer, buffer.length);
                        rcvPacket = new DatagramPacket(new byte[buffer.length], buffer.length);
                        packet.setPort(portRcvFromVehicle);
                        packet.setAddress(routerAddress);
                        socket.send(packet);
                        socket.receive(rcvPacket);
                        assertEquals(simpleDenm, new SimpleDenm(rcvPacket.getData()));

                        SimpleIclcm simpleIclcm = SampleMessages.getSampleIclcm();
                        buffer = simpleIclcm.asByteArray();
                        packet = new DatagramPacket(buffer, buffer.length);
                        rcvPacket = new DatagramPacket(new byte[buffer.length], buffer.length);
                        packet.setPort(portRcvFromVehicle);
                        packet.setAddress(routerAddress);
                        socket.send(packet);
                        socket.receive(rcvPacket);
                        assertEquals(simpleIclcm, new SimpleIclcm(rcvPacket.getData()));
                }

                router.close();
        }
}
