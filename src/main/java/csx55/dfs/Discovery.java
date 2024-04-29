
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
import csx55.wireformats.*;

public class Discovery implements Node{
    
    // Hash table to store the <peerID, hostname:portnum> of the peer nodes
    // using the wrapper class Collections to synchronize the map to prevent concurrent modification
    private Map<Integer, String> peerNodes = Collections.synchronizedMap(new HashMap<Integer, String>());

    // Singleton instance to ensure only one discoveryNode is created
    private static Discovery instance = null;

    // user defined port number and ip address
    private int portNumber; // port number to listen on
    private String ipAddress; // ip address to listen on
    private String node; // hostname:port
    private int peerID; // hashcode of the node

    private TCPServerThread serverThread; // server thread to listen for incoming connections

    // Constructor is private so that only one discoveryNode can be created
    private Discovery(int portNumber) {
        this.portNumber = portNumber;
        this.peerNodes = Collections.synchronizedMap(new HashMap<Integer, String>());

        // get the local ip Address
        try {
            this.ipAddress = InetAddress.getLocalHost().getHostAddress();
            this.node = this.ipAddress + ":" + this.portNumber;
            this.peerID = generateNodeID(this.node);
        } catch (UnknownHostException e) {
            System.out.println("Failed to get local IP Address: " + e.getMessage());
        }
    }

    // Public static method to get the single instance of the Discovery
    public static Discovery getInstance(int portNumber) {
        if (instance == null) {
            instance = new Discovery(portNumber);
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

    @Override
    public int generateNodeID(String node) {
        // Return the hash code of the unique identifier <IP>:<port> (this.node)
        return node.hashCode();
    }

    public int getPortNumber() {
        return portNumber;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public synchronized void registerNode(int peerId, String hostname, int port, Socket socket) throws IOException {
        
        int key = peerId;
        String value = hostname + ":" + port;
        
        // Check if the node had previously registered
        if (peerNodes.containsKey(key)) {
            // create a new TCPSender to send the response
            TCPSender sender = new TCPSender(socket);
            try {
                sendRegisterResponse(sender, Protocol.FAILURE, "NULL", "Node had previously registered");
            } catch (IOException e) {
                System.out.println("Failed to send register response: " + e.getMessage());
            } 
            return;
        }
    
        // Check if the peerID is equal to the hashcode of the value
        if (key != value.hashCode()) {
            // create a new TCPSender to send the response
            TCPSender sender = new TCPSender(socket);
            try {
                sendRegisterResponse(sender, Protocol.FAILURE, "NULL", "PeerID does not match the hashcode of the IP address and port number");
            } catch (IOException e) {
                System.out.println("Failed to send register response: " + e.getMessage());
            } 
            return;
        }

        // Add the node to the list of peer nodes
        try {
            peerNodes.put(key, value);
            System.out.println("\nAdded node to the list of peer nodes: " + key + " " + value);
            // create a new TCPSender to send the response
            TCPSender sender = new TCPSender(socket);
            // select a random peer node to send to the new node
            if (peerNodes.size() == 1) {
                sendRegisterResponse(sender, Protocol.SUCCESS, value, "Registration request successful. The number of peer nodes currently registered: (" + peerNodes.size() + ")");
                return;
            }else{
                String randomPeerNode = "";
                // as long as the random peer node is not the same as the new node
                do {
                    randomPeerNode = peerNodes.get(peerNodes.keySet().toArray()[(int)(Math.random() * peerNodes.size())]);
                } while(randomPeerNode.equals(value));
                System.out.println("Random peer node: " + randomPeerNode);
                sendRegisterResponse(sender, Protocol.SUCCESS, randomPeerNode, "Registration request successful. The number of peer nodes currently registered: (" + peerNodes.size() + ")");
            }
        } catch (IOException e) {
            System.out.println("Failed to add node to the list of peer nodes: " + e.getMessage());
        } 
    }
   
    // Deregister a peer node
    public void deregisterNode(int peerID, String hostname, int port, Socket socket) throws Exception {
        String peerNode = hostname + ":" + port;
        int peerNodeKey = peerID;
        if (!peerNodes.containsKey(peerID)) {
            throw new Exception("Node not found in network: " + peerNodeKey + " " + peerNode);
        }
        try {
            TCPSender sender = new TCPSender(socket);
            // remove node from the list of peer nodes
            sendDeregisterResponse(sender, Protocol.SUCCESS);
            peerNodes.remove(peerID);
            System.out.println("Removed node from the list of peer nodes: " + peerNodeKey + " " + peerNode);
            sender.closeSocket();
        } catch (IOException e) {
            throw new Exception("Failed to deregister: " + peerNodeKey + " " + peerNode, e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Failed to close socket: " + e.getMessage());
            }
        }
    }

    // List all peer nodes in the overlay <peerID> <ipaddress>:<port>
    public void listPeerNodes() {
        System.out.println("Peer nodes in the overlay:");
        peerNodes.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> System.out.println(entry.getKey() + " " + entry.getValue()));
    }

    // Exit the overlay
    public void exit() {
        // Close the server thread
        System.out.println("Discovery Node is leaving the topology. Goodbye!");
        serverThread.shutdown();
    }

    private void sendRegisterResponse(TCPSender senderSocket, byte status, String randPeer, String info) throws IOException {
        RegisterResponse response = new RegisterResponse(status, randPeer, info);

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

    public void onEvent(Event event, Socket socket) throws IOException {

        switch (event.getType()) {
            case Protocol.REGISTER_REQUEST:
                // cast the event to a RegisterRequest
                RegisterRequest request = (RegisterRequest) event;
                System.out.println("Printing Register Request Info: \n" + request.getInfo());
                int peerID = request.getPeerID();
                String hostname = request.getIpAddress();
                int port = request.getPortNumber();
                // Register the node and send a success response
                registerNode(peerID, hostname, port, socket);

                break;
    
            case Protocol.DEREGISTER_REQUEST:
                // cast the event to a DeregisterRequest
                DeregisterRequest deregisterRequest = (DeregisterRequest) event;
                try {
                    deregisterNode(deregisterRequest.getPeerID(), deregisterRequest.getIpAddress(), deregisterRequest.getPortNumber(), socket);
                    System.out.println("Printing deregister Request Info: \n" + deregisterRequest.getInfo());
                } catch (Exception e) {
                    System.out.println("Failed to deregister node: " + e.getMessage());
                }
                break;
                
            default:
                System.out.println("Unknown event type: " + event.getType());
                break;
        }
    }
    

    // Main method to run the discoveryNode
    // gradle build
    // ~/CS555/hw3/build/classes/java/main$ java csx55.chord.DiscoveryNode 45555
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
    
        // create a new discoveryNode
        Discovery discoveryNode = new Discovery(portNumber);

        // start the server thread
        discoveryNode.startServer();

        // get the local ip Address
        try {
            discoveryNode.ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println("Failed to get local IP Address: " + e.getMessage());
            return;
        }

        System.out.println("Discovery Node is listening on port " + portNumber);
        System.out.println("IP Address: " + discoveryNode.ipAddress);
        System.out.println("(Type h or help for a list of commands)\n");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter command: ");
            String command = scanner.nextLine();

            if (command.equals("peer-nodes")) {
                discoveryNode.listPeerNodes();
            }
            else if ((command.toLowerCase().equals("h")) || (command.equals("help"))) {
                System.out.println("Commands:");
                System.out.println("peer-nodes: List all peer nodes in the overlay");
                System.out.println("exit: Exit the topology");
            } 
            else if (command.equals("exit")) {
                discoveryNode.exit();
                scanner.close();
                break;
            }
            else {
                System.out.println("Invalid command");
            }
        }
    }

}