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
                            break;
                        }

                        Smarthome.Response.Builder response = Smarthome.Response.newBuilder();
                        handleCommand(command, response);
                        response.build().writeDelimitedTo(output);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("TCP server error: " + e.getMessage());
        }
    }

    private static void handleCommand(Smarthome.Command command, Smarthome.Response.Builder response) {
        if ("LIST_DEVICES".equalsIgnoreCase(command.getAction())) {
            if (devices.isEmpty()) {
                response.setStatus("OK")
                        .setMessage("No devices connected.");
            } else {
                StringBuilder deviceList = new StringBuilder();
                for (Smarthome.Device device : devices.values()) {
                    deviceList.append("\n")
                            .append("ID: ").append(device.getId())
                            .append(", Type: ").append(device.getType())
                            .append(", State: ").append(device.getState());
                    if(device.getId().startsWith("lamp")){
                        deviceList.append(", ").append(device.getColor())
                                .append(", ").append(device.getBrightness());
                    }
                    if(device.getId().startsWith("tv")){
                        deviceList.append(", ").append(device.getChannel())
                                .append(", ").append(device.getVolume());
                    }
                    if(device.getId().startsWith("air")){
                        deviceList.append(", ").append(device.getTemperature())
                                .append(", ").append(device.getMode());
                    }
                }
                response.setStatus("OK").setMessage(deviceList.toString());
            }
        } else if ("STATUS".equalsIgnoreCase(command.getAction())) {
            if (devices.containsKey(command.getDeviceId())) {
                StringBuilder deviceList = new StringBuilder();
                Smarthome.Device device = devices.get(command.getDeviceId());
                deviceList.append("ID: ").append(device.getId())
                        .append(", Type: ").append(device.getType())
                        .append(", State: ").append(device.getState());
                if(device.getId().startsWith("lamp")){
                    deviceList.append(", ").append(device.getColor())
                            .append(", ").append(device.getBrightness()).append("\n");
                }
                if(device.getId().startsWith("tv")){
                    deviceList.append(", ").append(device.getChannel())
                            .append(", ").append(device.getVolume()).append("\n");
                }
                if(device.getId().startsWith("air")){
                    deviceList.append(", ").append(device.getTemperature())
                            .append(", ").append(device.getMode()).append("\n");
                }
                response.setStatus("OK").setMessage(deviceList.toString());
            } else {
                response.setStatus("Error")
                        .setMessage("Device not found: " + command.getDeviceId());
            }
        } else {
            // Handle specific commands based on action
            Smarthome.Device device = devices.get(command.getDeviceId());
            if (device != null) {
                Smarthome.Device.Builder deviceBuilder = device.toBuilder();

                if (!command.getColor().isEmpty()) {
                    deviceBuilder.setColor("Color: " + command.getColor());
                }
                if (!command.getAction().isEmpty()) {
                    deviceBuilder.setState(command.getAction());
                }
                if (!command.getBrightness().isEmpty()) {
                    deviceBuilder.setBrightness("Brightness: " + command.getBrightness() + "%");
                }
                if (!command.getChannel().isEmpty()) {
                    deviceBuilder.setChannel("Channel: " + command.getChannel());
                }
                if (!command.getVolume().isEmpty()) {
                    deviceBuilder.setVolume("Volume: " + command.getVolume() + "%");
                }
                if (!command.getTemperature().isEmpty()) {
                    deviceBuilder.setTemperature("Temperature: " + command.getTemperature() + "°C");
                }
                if (!command.getMode().isEmpty()) {
                    deviceBuilder.setMode("Mode: " + command.getMode());
                }

                devices.put(command.getDeviceId(), deviceBuilder.build());
                response.setStatus("OK").setMessage("Command executed.");
            } else {
                response.setStatus("Error").setMessage("Device not found.");
            }
        }
    }
}
