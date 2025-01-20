package org.example.client;

import org.example.devices.Smarthome;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 6000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             OutputStream output = socket.getOutputStream();
             InputStream input = socket.getInputStream();
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Client connected to Gateway!");

            while (true) {
                System.out.println("\nChoose an option:");
                System.out.println("1. Send Command");
                System.out.println("2. Get Device Status");
                System.out.println("3. List All Devices");
                System.out.println("4. Exit");
                System.out.print("Option: ");
                String option = scanner.nextLine();

                if ("1".equals(option)) {
                    System.out.print("Device ID: ");
                    String id = scanner.nextLine();
                    System.out.print("Action (ON/OFF): ");
                    String action = scanner.nextLine();

                    Smarthome.Command command = Smarthome.Command.newBuilder()
                            .setDeviceId(id)
                            .setAction(action)
                            .build();

                    System.out.println("Sending serialized Command: " + Arrays.toString(command.toByteArray()));
                    command.writeDelimitedTo(output);

                    Smarthome.Response response = Smarthome.Response.parseDelimitedFrom(input);
                    System.out.println("Gateway Response: " + response.getMessage());
                } else if ("2".equals(option)) {
                    System.out.print("Enter Device ID: ");
                    String id = scanner.nextLine();

                    Smarthome.Command command = Smarthome.Command.newBuilder()
                            .setDeviceId(id)
                            .setAction("STATUS")
                            .build();

                    System.out.println("Requesting status for device: " + id);
                    command.writeDelimitedTo(output);

                    Smarthome.Response response = Smarthome.Response.parseDelimitedFrom(input);
                    System.out.println("Device Status: " + response.getMessage());
                } else if ("3".equals(option)) {
                    // Request to list all devices
                    Smarthome.Command command = Smarthome.Command.newBuilder()
                            .setAction("LIST_DEVICES")
                            .build();

                    System.out.println("Requesting list of all devices...");
                    command.writeDelimitedTo(output);

                    Smarthome.Response response = Smarthome.Response.parseDelimitedFrom(input);
                    System.out.println("Connected Devices:\n" + response.getMessage());
                } else if ("4".equals(option)) {
                    System.out.println("Exiting client...");
                    break;
                } else {
                    System.out.println("Invalid option.");
                }
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
