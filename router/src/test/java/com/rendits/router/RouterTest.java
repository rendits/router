package com.rendits.router;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Properties;
import java.util.Random;
import net.gcdc.UdpDuplicator;
import org.junit.Test;

public class RouterTest {
  private static final int MAX_UDP_LENGTH = 600;
  private static final byte CUSTOM_MESSAGE_ID = 20;
  private static final int numCustomPorts = 10;

  private void sendMessages(DatagramSocket socket,
                            InetAddress routerAddress,
                            int portRcvFromVehicle,
                            int portSendIts) throws IOException {
    byte[] buffer;
    DatagramPacket packet;
    DatagramPacket rcvPacket;
    int messagesToSend = 100;
    SimpleCam simpleCam = SampleMessages.getSampleCam();
    SimpleDenm simpleDenm = SampleMessages.getSampleDenm();
    SimpleIclcm simpleIclcm = SampleMessages.getSampleIclcm();
    Random random = new Random();
    for (int i = 0; i < messagesToSend; i++) {

      // CAM
      buffer = simpleCam.asByteArray();
      packet = new DatagramPacket(buffer, buffer.length);
      rcvPacket = new DatagramPacket(new byte[buffer.length], buffer.length);
      packet.setPort(portRcvFromVehicle);
      packet.setAddress(routerAddress);
      socket.send(packet);
      socket.receive(rcvPacket);
      assertArrayEquals(buffer, rcvPacket.getData());
      assertEquals(simpleCam, new SimpleCam(rcvPacket.getData()));

      // DENM
      buffer = simpleDenm.asByteArray();
      packet = new DatagramPacket(buffer, buffer.length);
      rcvPacket = new DatagramPacket(new byte[buffer.length], buffer.length);
      packet.setPort(portRcvFromVehicle);
      packet.setAddress(routerAddress);
      socket.send(packet);
      socket.receive(rcvPacket);
      assertArrayEquals(buffer, rcvPacket.getData());
      assertEquals(simpleDenm, new SimpleDenm(rcvPacket.getData()));

      // iCLCM
      buffer = simpleIclcm.asByteArray();
      packet = new DatagramPacket(buffer, buffer.length);
      rcvPacket = new DatagramPacket(new byte[buffer.length], buffer.length);
      packet.setPort(portRcvFromVehicle);
      packet.setAddress(routerAddress);
      socket.send(packet);
      socket.receive(rcvPacket);
      assertArrayEquals(buffer, rcvPacket.getData());
      assertEquals(simpleIclcm, new SimpleIclcm(rcvPacket.getData()));

      // custom messages
      int r = random.nextInt(numCustomPorts);
      // int r = 0;
      byte messageId = (byte) (CUSTOM_MESSAGE_ID + r);
      int customPort = portSendIts + r;
      buffer = new byte[MAX_UDP_LENGTH];
      random.nextBytes(buffer);
      // buffer[0] = CUSTOM_MESSAGE_ID;
      buffer[0] = messageId;
      packet = new DatagramPacket(buffer, buffer.length);
      rcvPacket = new DatagramPacket(new byte[buffer.length], buffer.length);
      packet.setPort(portRcvFromVehicle);
      packet.setAddress(routerAddress);
      socket.send(packet);
      if (r == 0) {
        socket.receive(rcvPacket);
      } else {
        DatagramSocket customSocket = new DatagramSocket(customPort);
        customSocket.receive(rcvPacket);
        customSocket.close();
      }
      assertArrayEquals(buffer, rcvPacket.getData());
    }
    return;
  }

  @Test
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
    props.setProperty("portSendCustom", "" + portSendIts);
    props.setProperty("receiveThreads", "1");
    props.setProperty("sendThreads", "3");
    props.setProperty("vehicleAddress", vehicleAddress);
    props.setProperty("localPortForUdpLinkLayer", "" + localPortForUdpLinkLayer);
    props.setProperty(
        "remoteAddressForUdpLinkLayer",
        remoteAddressForUdpLinkLayer + ":" + remotePortForUdpLinkLayer);
    props.setProperty("macAddress", "00:00:00:00:00:00");
    props.setProperty("countryCode", "46");

    /* Setup the UDP duplicator */
    UdpDuplicator udpDuplicator = new UdpDuplicator();
    SocketAddress remoteAddress = new InetSocketAddress(vehicleAddress, localPortForUdpLinkLayer);
    udpDuplicator.add(remotePortForUdpLinkLayer, remoteAddress);

    /* Start the router */
    Router router = new Router(props);

    /* Setup sockets */
    DatagramSocket socket = new DatagramSocket(portSendIts);
    socket.setSoTimeout(1000);
    InetAddress routerAddress = InetAddress.getByName("127.0.0.1");

    /* Send some messages and make sure we get the same thing back. */
    sendMessages(socket, routerAddress, portRcvFromVehicle, portSendIts);

    /* Stop the router */
    router.close();
  }
}
