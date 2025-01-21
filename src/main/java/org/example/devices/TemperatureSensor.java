package org.example.devices;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class TemperatureSensor {
    private static final String MULTICAST_GROUP = "230.0.0.1";
    private static final int MULTICAST_PORT = 6001;
    private static final int TIMEOUT_MS = 5000; // Timeout for health check

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java TemperatureSensor <id> <type> <temperature>");
            return;
        }

        String id = args[0];
        String type = args[1];
        String temperature = args[2];
        InetAddress group;

        try {
            group = InetAddress.getByName(MULTICAST_GROUP);
        } catch (UnknownHostException e) {
            System.err.println("Invalid multicast group: " + e.getMessage());
            return;
        }

        System.out.println("Temperature Sensor initializing: ID=" + id + ", Type=" + type + ", Temperature=" + temperature);

        try (MulticastSocket socket = new MulticastSocket()) {
            boolean gatewayAvailable = true;

            sendTemperaturePacket(socket, group, id, type, temperature);
            System.out.println("Initial temperature sent: " + temperature);

            while (gatewayAvailable) {

                String updatedTemperature = getSensorTemperature(socket, group, id);
                if (updatedTemperature != null) {
                    temperature = updatedTemperature;
                }

                // Envia o pacote com a temperatura atualizada
                sendTemperaturePacket(socket, group, id, type, temperature);

                // Verifica a saúde do gateway
                gatewayAvailable = checkGatewayHealth(socket, group);

                // Wait for the next interval
                TimeUnit.SECONDS.sleep(15);
            }

            System.out.println("Gateway not reachable. Stopping temperature sensor...");
        } catch (IOException | InterruptedException e) {
            System.err.println("Sensor error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendTemperaturePacket(MulticastSocket socket, InetAddress group, String id, String type, String temperature) throws IOException {
        Smarthome.Device device = Smarthome.Device.newBuilder()
                .setId(id)
                .setType(type)
                .setState(temperature)
                .build();

        byte[] data = device.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, group, MULTICAST_PORT);
        socket.send(packet);
        System.out.println("Temperature sent: " + temperature);
    }

    private static boolean checkGatewayHealth(MulticastSocket socket, InetAddress group) {
        try {
            // Send a health check message
            String healthCheckMessage = "HEALTH_CHECK";
            DatagramPacket healthCheckPacket = new DatagramPacket(
                    healthCheckMessage.getBytes(), healthCheckMessage.length(), group, MULTICAST_PORT
            );
            socket.send(healthCheckPacket);

            // Set socket timeout for receiving the response
            socket.setSoTimeout(TIMEOUT_MS);

            // Listen for a response
            byte[] buffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(responsePacket);

            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            if ("HEALTH_OK".equals(response)) {
                System.out.println("Gateway is reachable.");
                return true;
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Health check timeout. No response from Gateway.");
        } catch (IOException e) {
            System.err.println("Health check error: " + e.getMessage());
        }

        return false;
    }

    private static String getSensorTemperature(MulticastSocket socket, InetAddress group, String id) {
        try {
            // Send a health check message
            String statusSensorMessage = "STATUSSENSOR_" + id;
            DatagramPacket healthCheckPacket = new DatagramPacket(
                    statusSensorMessage.getBytes(), statusSensorMessage.length(), group, MULTICAST_PORT
            );
            socket.send(healthCheckPacket);

            // Listen for a response
            byte[] buffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(responsePacket);

            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            if (response != null) {
                return response;
            }
        } catch (IOException e) {
            System.err.println("STATUS_SENSOR error: " + e.getMessage());
        }

        return null;
    }
}
