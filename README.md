# Smart home

## Overview
This project demonstrates communication between processes using **TCP** and **UDP** sockets, message serialization using **Protocol Buffers**, and device discovery using multicast.

## Features
- **TCP and UDP Communication**: Client-Gateway communication uses TCP, while devices communicate with the gateway using UDP multicast.
- **Protocol Buffers**: For efficient serialization and deserialization of messages exchanged between components.
- **Device Discovery**: Devices broadcast their presence to the gateway using multicast UDP.

---

## Project Structure

### Classes
1. **Client** (`org.example.client.Client`)
    - Connects to the gateway using TCP.
    - Sends commands to control or query devices.
    - Options include:
        - Send specific device commands (e.g., ON/OFF, brightness, etc.).
        - Query device status.
        - List all connected devices.

2. **Gateway** (`org.example.gateway.Gateway`)
    - Serves as the central controller.
    - Manages devices and processes commands received from clients.
    - Listens for device broadcasts using multicast UDP.
    - Accepts client connections over TCP and handles commands.

3. **Device** (`org.example.devices.Device`)
    - Represents a smart home device (e.g., Lamp, TV, Air Conditioner).
    - Broadcasts its presence using UDP multicast.

---

## Communication Details

### TCP Communication
- **Client ↔ Gateway**:  
  TCP is used for secure and reliable communication between the client and gateway. Commands and responses are serialized using Protocol Buffers.

### UDP Communication
- **Device → Gateway**:  
  Devices send their details to the gateway using multicast UDP. This allows the gateway to discover devices dynamically.

### Protocol Buffers
- Messages and data exchanged between client, gateway, and devices are serialized using Protocol Buffers for efficiency and compactness.

---

## Usage Instructions

### Prerequisites
- **Java 21**
- **Protocol Buffers Compiler (`protoc`)**
- Protocol Buffers Java Runtime Library

### Steps to Run
1. **Compile the Project**:
    - Compile the `.proto` files to generate Java classes for message serialization.
    - Ensure Protocol Buffers Java runtime library is included in the project.

2. **Start the Gateway**:
   ```bash
   java org.example.gateway.Gateway
   
3. **Start Devices**:
   - Run the Device class for each device you want to register:
   ```bash
      java org.example.devices.Device <id> <type> <state>
   ```
   - Example
   ```bash
      java org.example.devices.Device lamp1 lamp ON
   
4. **Start Devices**:
   ```bash
      java org.example.client.Client
5. **Interact with the System:**:
    - Use the client interface to send commands, query device status, or list all devices.

## Commands
### Client Commands
- Send Command: Sends a specific command to a device (e.g., ON/OFF, brightness, color, etc.).
- Get Device Status: Queries the status of a specific device.
- List All Devices: Displays all registered devices along with their statuses.

### Device Commands
- Devices respond to specific commands depending on their type:
- Lamp: ON/OFF, Change Color, Set Brightness.
- TV: ON/OFF, Change Channel, Set Volume.
- Air Conditioner: ON/OFF, Change Temperature, Change Mode.

## Dependencies
- Protocol Buffers: For message serialization.
- Java Networking APIs: java.net.Socket, java.net.ServerSocket, java.net.MulticastSocket.
