package org.example.gateway;

import org.example.devices.Smarthome;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Gateway {
    private static final int TCP_PORT = 6000;
    private static final int MULTICAST_PORT = 6001;
    private static final String MULTICAST_GROUP = "230.0.0.1";

    private static Map<String, Smarthome.Device> devices = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Gateway started...");

        // Start multicast listener
        new Thread(Gateway::startMulticastListener).start();

        // Start TCP server
        startTCPServer();
    }

    private static void startMulticastListener() {
        try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
            socket.joinGroup(InetAddress.getByName(MULTICAST_GROUP));
            System.out.println("Gateway: Listening for multicast messages...");

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                System.out.println("Received multicast packet from: " + packet.getAddress());
                String message = new String(packet.getData(), 0, packet.getLength());

                try {
                    if ("HEALTH_CHECK".equals(message)) {
                        System.out.println("Received health check request. Sending response...");
                        InetAddress address = packet.getAddress();
                        int port = packet.getPort();

                        String response = "HEALTH_OK";
                        DatagramPacket responsePacket = new DatagramPacket(
                                response.getBytes(), response.length(), address, port
                        );
                        socket.send(responsePacket);
                    }else{
                        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                        System.out.println("Received data length: " + data.length);

                        // Deserialize the data
                        Smarthome.Device device = Smarthome.Device.parseFrom(data);

                        // Log the device details
                        System.out.println("Device Registered: ID=" + device.getId() + ", Type=" + device.getType() + ", State=" + device.getState());

                        // Add device to the map
                        devices.put(device.getId(), device);
                        System.out.println("Devices Map: " + devices);
                    }
                } catch (Exception e) {
                    System.err.println("Multicast error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Multicast socket error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void startTCPServer() {
        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            System.out.println("Gateway: TCP server started on port " + TCP_PORT);

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     InputStream input = clientSocket.getInputStream();
                     OutputStream output = clientSocket.getOutputStream()) {

                    System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());

                    while (!clientSocket.isClosed()) {
                        Smarthome.Command command = Smarthome.Command.parseDelimitedFrom(input);
                        if (command == null) {
                            break; // End of stream
                        }

                        System.out.println("Parsed Command: Action=" + command.getAction() + ", DeviceID=" + command.getDeviceId());

                        Smarthome.Response.Builder response = Smarthome.Response.newBuilder();

                        if ("LIST_DEVICES".equalsIgnoreCase(command.getAction())) {
                            if (devices.isEmpty()) {
                                response.setStatus("OK")
                                        .setMessage("No devices connected.");
                            } else {
                                StringBuilder deviceList = new StringBuilder();
                                for (Smarthome.Device device : devices.values()) {
                                    deviceList.append("Device ID: ").append(device.getId())
                                            .append(", Type: ").append(device.getType())
                                            .append(", State: ").append(device.getState())
                                            .append("\n");
                                }
                                response.setStatus("OK")
                                        .setMessage(deviceList.toString());
                            }
                        } else if ("STATUS".equalsIgnoreCase(command.getAction())) {
                            if (devices.containsKey(command.getDeviceId())) {
                                Smarthome.Device device = devices.get(command.getDeviceId());
                                response.setStatus("OK")
                                        .setMessage("Device ID: " + device.getId() +
                                                ", Type: " + device.getType() +
                                                ", State: " + device.getState());
                            } else {
                                response.setStatus("Error")
                                        .setMessage("Device not found: " + command.getDeviceId());
                            }
                        } else {
                            if (devices.containsKey(command.getDeviceId())) {
                                Smarthome.Device device = devices.get(command.getDeviceId());
                                Smarthome.Device updatedDevice = device.toBuilder()
                                        .setState(command.getAction())
                                        .build();

                                devices.put(command.getDeviceId(), updatedDevice);
                                System.out.println("Updated Device: ID=" + updatedDevice.getId() + ", State=" + updatedDevice.getState());

                                response.setStatus("OK")
                                        .setMessage("Command executed: " + command.getAction());
                            } else {
                                System.out.println("Device not found: " + command.getDeviceId());
                                response.setStatus("Error")
                                        .setMessage("Device not found: " + command.getDeviceId());
                            }
                        }

                        // Send response
                        response.build().writeDelimitedTo(output);
                        System.out.println("Response sent to client");
                    }
                } catch (IOException e) {
                    System.err.println("Error processing client: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("TCP server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
