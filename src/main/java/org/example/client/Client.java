package org.example.client;

import org.example.devices.Smarthome;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
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
                    System.out.print("Enter Device ID: ");
                    String deviceId = scanner.nextLine();
                    sendDeviceSpecificCommand(deviceId, output, input);
                } else if ("2".equals(option)) {
                    System.out.print("Enter Device ID: ");
                    String deviceId = scanner.nextLine();

                    Smarthome.Command command = Smarthome.Command.newBuilder()
                            .setDeviceId(deviceId)
                            .setAction("STATUS")
                            .build();

                    System.out.println("Requesting status for device: " + deviceId);
                    command.writeDelimitedTo(output);

                    Smarthome.Response response = Smarthome.Response.parseDelimitedFrom(input);
                    System.out.println("Device Status: " + response.getMessage());
                } else if ("3".equals(option)) {
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

    private static void sendDeviceSpecificCommand(String deviceId, OutputStream output, InputStream input) throws IOException {
        Scanner scanner = new Scanner(System.in);
        Smarthome.Command.Builder commandBuilder = Smarthome.Command.newBuilder().setDeviceId(deviceId);

        if (deviceId.toLowerCase().startsWith("lamp")) {
            System.out.println("Choose an action for Lamp:");
            System.out.println("1. ON/OFF");
            System.out.println("2. Change Color");
            System.out.println("3. Set Brightness (%)");
            String lampOption = scanner.nextLine();
            switch (lampOption) {
                case "1":
                    System.out.print("Enter Action (ON/OFF): ");
                    commandBuilder.setAction(scanner.nextLine());
                    break;
                case "2":
                    System.out.print("Enter Color (e.g., Blue, Yellow): ");
                    commandBuilder.setColor(scanner.nextLine());
                    break;
                case "3":
                    System.out.print("Enter Brightness (0-100): ");
                    commandBuilder.setBrightness(scanner.nextLine());
                    break;
                default:
                    System.out.println("Invalid option.");
                    return;
            }
        } else if (deviceId.toLowerCase().startsWith("tv")) {
            System.out.println("Choose an action for TV:");
            System.out.println("1. ON/OFF");
            System.out.println("2. Change Channel");
            System.out.println("3. Set Volume (%)");
            String tvOption = scanner.nextLine();
            switch (tvOption) {
                case "1":
                    System.out.print("Enter Action (ON/OFF): ");
                    commandBuilder.setAction(scanner.nextLine());
                    break;
                case "2":
                    System.out.print("Enter Channel: ");
                    commandBuilder.setChannel(scanner.nextLine());
                    break;
                case "3":
                    System.out.print("Enter Volume (0-100): ");
                    commandBuilder.setVolume(scanner.nextLine());
                    break;
                default:
                    System.out.println("Invalid option.");
                    return;
            }
        } else if (deviceId.toLowerCase().startsWith("air")) {
            System.out.println("Choose an action for Air Conditioner:");
            System.out.println("1. ON/OFF");
            System.out.println("2. Change Temperature");
            System.out.println("3. Change Mode");
            String airOption = scanner.nextLine();
            switch (airOption) {
                case "1":
                    System.out.print("Enter Action (ON/OFF): ");
                    commandBuilder.setAction(scanner.nextLine());
                    break;
                case "2":
                    System.out.print("Enter Temperature: ");
                    commandBuilder.setTemperature(scanner.nextLine());
                    break;
                case "3":
                    System.out.print("Enter Mode (Auto, Cooling, Dry, Fan): ");
                    commandBuilder.setMode(scanner.nextLine());
                    break;
                default:
                    System.out.println("Invalid option.");
                    return;
            }
        } else {
            System.out.println("Unknown device type.");
            return;
        }

        // Send command to server
        commandBuilder.build().writeDelimitedTo(output);
        System.out.println("Command sent. Awaiting response...");

        Smarthome.Response response = Smarthome.Response.parseDelimitedFrom(input);
        System.out.println("Server Response: " + response.getMessage());
    }
}
