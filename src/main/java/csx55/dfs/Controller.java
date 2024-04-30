
package csx55.dfs;

import java.util.Collections;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;

import java.io.IOException;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import csx55.transport.TCPSender;
import csx55.transport.TCPServerThread;
import csx55.helper.ChunkInfo;
import csx55.wireformats.*;

public class Controller implements Node{
    
    // Hash table to store the <peerID, hostname:portnum> of the peer nodes
    // using the wrapper class Collections to synchronize the map to prevent concurrent modification
    // maps hostname:port to a ChunkInfo object
    private Map<String, ChunkInfo> chunkServerInfo = Collections.synchronizedMap(new HashMap<String, ChunkInfo>());

    // Singleton instance to ensure only one controllerNode is created
    private static Controller instance = null;

    // user defined port number and ip address
    private int portNumber; // port number to listen on
    private String ipAddress; // ip address to listen on
    private String node; // hostname:port

    private TCPServerThread serverThread; // server thread to listen for incoming connections

    // Constructor is private so that only one controllerNode can be created
    private Controller(int portNumber) {
        this.portNumber = portNumber;
        this.chunkServerInfo = Collections.synchronizedMap(new HashMap<String, ChunkInfo>());

        // get the local ip Address
        try {
            this.ipAddress = InetAddress.getLocalHost().getHostAddress();
            this.node = this.ipAddress + ":" + this.portNumber;
        } catch (UnknownHostException e) {
            System.out.println("Failed to get local IP Address: " + e.getMessage());
        }
    }

    // Public static method to get the single instance of the Controller
    public static Controller getInstance(int portNumber) {
        if (instance == null) {
            instance = new Controller(portNumber);
        }
        return instance;
    }

    // start the server thread
    public void startServer() {
        try {
            this.serverThread = new TCPServerThread(this.portNumber, this); // 0 means the OS will pick a port number
            new Thread(this.serverThread).start();
        } catch (IOException e) {
            System.out.println("Failed to start the server: " + e.getMessage());
        }
        
    }

    public int getPortNumber() {
        return portNumber;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public synchronized void registerNode(String hostname, int port, Socket socket) throws IOException {
        
        String key = hostname + ":" + port;
        
        // Check if the node had previously registered
        if (chunkServerInfo.containsKey(key)) {
            // create a new TCPSender to send the response
            TCPSender sender = new TCPSender(socket);
            try {
                sendRegisterResponse(sender, Protocol.FAILURE, "Node had previously registered");
            } catch (IOException e) {
                System.out.println("Failed to send register response: " + e.getMessage());
            } 
            return;
        }
        // Add the node to the list of peer nodes
        try {
            chunkServerInfo.put(key, new ChunkInfo(key));
            System.out.println("\nAdded chunkServer: " + key);
            // create a new TCPSender to send the response
            TCPSender sender = new TCPSender(socket);
            if (chunkServerInfo.size() == 1) {
                sendRegisterResponse(sender, Protocol.SUCCESS, "Registration request successful. The number of chunk servers currently registered: (" + chunkServerInfo.size() + ")");
                return;
            }
        } catch (IOException e) {
            System.out.println("Failed to add node to the list of chunk server nodes: " + e.getMessage());
        } 
    }
   
    // Deregister a peer node
    public void deregisterNode(String chunkID, Socket socket) throws Exception {
        if (!chunkServerInfo.containsKey(chunkID)) {
            throw new Exception("Node not found in network: " + chunkID);
        }
        try {
            TCPSender sender = new TCPSender(socket);
            // remove node from the list of peer nodes
            sendDeregisterResponse(sender, Protocol.SUCCESS);
            chunkServerInfo.remove(chunkID);
            System.out.println("Removed node from the list of chunk server nodes: " + chunkID);
            sender.closeSocket();
        } catch (IOException e) {
            throw new Exception("Failed to deregister: " + chunkID, e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Failed to close socket: " + e.getMessage());
            }
        }
    }

    // List all chunk servers in the overlay
    public void listChunkServers() {
        System.out.println("Chunk Servers in the overlay:");
        chunkServerInfo.forEach((key, value) -> {
            System.out.println(key);
        });
    }

    // Exit the overlay
    public void exit() {
        // Close the server thread
        System.out.println("Controller Node is leaving the topology. Goodbye!");
        serverThread.shutdown();
    }

    private void sendRegisterResponse(TCPSender senderSocket, byte status, String info) throws IOException {
        RegisterResponse response = new RegisterResponse(status, info);
        sendMessageToNode(senderSocket, response.getBytes());
        System.out.println("Printing Register Response Info: \n" + response.getInfo());
    }

    private void sendDeregisterResponse(TCPSender senderSocket, byte status) throws IOException {
        DeregisterResponse response = new DeregisterResponse(status);

        sendMessageToNode(senderSocket, response.getBytes());
        System.out.println("Printing Deegister Response Info: \n" + response.getInfo());
    }

    public void sendMessageToNode(TCPSender sender, byte[] message) {
        if (sender != null) {
            try {
                // send the message (synchronized)
                System.out.println("Sending message to " + sender.getSocket().getInetAddress().getHostAddress() + ":" + sender.getSocket().getPort());
                sender.sendData(message);
            } catch (IOException e) {
                System.out.println("Failed to send message: " + e.getMessage());
            }
        } else {
            System.out.println("No connection to node to sender");
        }
    }

    // method to check if no heartbeat is received from a node
    public void checkHeartbeat() {
        // check if the last heartbeat received from a node is more than 5 seconds
        // if so, remove the node from the list of peer nodes
        // and send a message to all other nodes to remove the node from their list of peer nodes
    }

    public void onEvent(Event event, Socket socket) throws IOException {

        switch (event.getType()) {
            case Protocol.REGISTER_REQUEST:
                // cast the event to a RegisterRequest
                RegisterRequest request = (RegisterRequest) event;
                System.out.println("Printing Register Request Info: \n" + request.getInfo());
                String hostname = request.getIpAddress();
                int port = request.getPortNumber();
                // Register the node and send a success response
                registerNode(hostname, port, socket);

                break;
    
            case Protocol.DEREGISTER_REQUEST:
                // cast the event to a DeregisterRequest
                DeregisterRequest deregisterRequest = (DeregisterRequest) event;
                try {
                    deregisterNode(deregisterRequest.getChunkID(), socket);
                    System.out.println("Printing deregister Request Info: \n" + deregisterRequest.getInfo());
                } catch (Exception e) {
                    System.out.println("Failed to deregister node: " + e.getMessage());
                }
                break;

            case Protocol.MINOR_HEARTBEAT:
                // cast the event to a MinorHeartBeat
                MinorHeartBeat minorHeartbeat = (MinorHeartBeat) event;
                System.out.println("Printing Minor Heartbeat Info: \n" + minorHeartbeat.getInfo());
                break;
            case Protocol.MAJOR_HEARTBEAT:
                // cast the event to a MinorHeartBeat
                MinorHeartBeat majorHeartbeat = (MinorHeartBeat) event;
                System.out.println("Printing Major Heartbeat Info: \n" + majorHeartbeat.getInfo());
                break;
            default:
                System.out.println("Unknown event type: " + event.getType());
                break;
        }
    }
    

    // Main method to run the controllerNode
    // gradle build
    // ~/CS555/hw3/build/classes/java/main$ java csx55.chord.ControllerNode 45555
    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Please provide exactly one argument: portnum");
            return;
        }
    
        int portNumber;
        try {
            portNumber = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Port number must be an integer");
            return;
        }
    
        if (portNumber < 1024 || portNumber > 65535) {
            System.out.println("Port number must be between 1024 and 65535");
            return;
        }
    
        // create a new controllerNode
        Controller controllerNode = new Controller(portNumber);

        // start the server thread
        controllerNode.startServer();

        // get the local ip Address
        try {
            controllerNode.ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println("Failed to get local IP Address: " + e.getMessage());
            return;
        }

        System.out.println("Controller Node is listening on port " + portNumber);
        System.out.println("IP Address: " + controllerNode.ipAddress);
        System.out.println("(Type h or help for a list of commands)\n");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter command: ");
            String command = scanner.nextLine();

            if (command.equals("chunk-servers")) {
                controllerNode.listChunkServers();
            }
            else if ((command.toLowerCase().equals("h")) || (command.equals("help"))) {
                System.out.println("Commands:");
                System.out.println("chunk-servers: List all peer nodes in the overlay");
                System.out.println("exit: Exit the topology");
            } 
            else if (command.equals("exit")) {
                controllerNode.exit();
                scanner.close();
                break;
            }
            else {
                System.out.println("Invalid command");
            }
        }
    }

}