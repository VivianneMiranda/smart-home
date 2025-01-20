package org.example.devices;

import java.net.*;
import java.io.*;

public class Device {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java Device <id> <type> <state>");
            return;
        }

        String id = args[0];
        String type = args[1];
        String state = args[2];

        try (MulticastSocket socket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName("230.0.0.1");
            int port = 6001;

            System.out.println("Device initializing: ID=" + id + ", Type=" + type + ", State=" + state);

            Smarthome.Device device = Smarthome.Device.newBuilder()
                    .setId(id)
                    .setType(type)
                    .setState(state)
                    .build();

            byte[] data = device.toByteArray();
            System.out.println("Serialized Device data: " + data.length + " bytes");

            DatagramPacket packet = new DatagramPacket(data, data.length, group, port);
            socket.send(packet);

            System.out.println("Device broadcasted: " + id);
        } catch (IOException e) {
            System.err.println("Device error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}