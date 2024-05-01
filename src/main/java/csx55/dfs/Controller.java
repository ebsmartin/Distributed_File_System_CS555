
package csx55.dfs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.io.IOException;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Set;


import csx55.transport.TCPSender;
import csx55.transport.TCPServerThread;
import csx55.helper.ChunkInfo;
import csx55.wireformats.*;

public class Controller implements Node{
    
    // Hash table to store the <peerID, hostname:portnum> of the peer nodes
    // using the wrapper class Collections to synchronize the map to prevent concurrent modification
    // maps hostname:port to a ChunkInfo object
    private Map<String, ChunkInfo> chunkServerInfo = Collections.synchronizedMap(new HashMap<String, ChunkInfo>());
    private Map<String, TCPSender> chunkServerSockets = Collections.synchronizedMap(new HashMap<String, TCPSender>());
    private Map<String, TCPSender> clientSockets = Collections.synchronizedMap(new HashMap<String, TCPSender>());
    
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

    public synchronized void registerNode(String hostname, int port, Socket socket, boolean isClient) throws IOException {
        
        String key = hostname + ":" + port;
        
        // Check if the node had previously registered
        if (chunkServerInfo.containsKey(key) || clientSockets.containsKey(key)) {
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
            if (isClient) {
                TCPSender sender = new TCPSender(socket);
                clientSockets.put(key, sender);
                System.out.println("\nAdded client: " + key);
            } else {
                chunkServerInfo.put(key, new ChunkInfo(key));
                System.out.println("\nAdded chunkServer: " + key);
                // create a new TCPSender to send the response
                TCPSender sender = new TCPSender(socket);
                chunkServerSockets.put(key, sender);
            }
        } catch (IOException e) {
            System.out.println("Failed to add node to the list of chunk server nodes: " + e.getMessage());
        } 
    }
   
    // Deregister a peer node
    public void deregisterNode(String chunkID, Socket socket) throws Exception {
        if (!chunkServerSockets.containsKey(chunkID)) {
            throw new Exception("Node not found in network: " + chunkID);
        }
        try {
            TCPSender sender = new TCPSender(socket);
            // remove node from the list of peer nodes
            sendDeregisterResponse(sender, Protocol.SUCCESS);
            chunkServerSockets.remove(chunkID);
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
    public void checkHeartbeat(MinorHeartBeat minorHeartbeat) {

        if (chunkServerInfo.isEmpty()) {
            return;
        }
        String chunkID = minorHeartbeat.getChunkID();
        ChunkInfo chunkInfo = chunkServerInfo.get(chunkID);
        if (chunkInfo == null) {
            return;
        }
        if (!chunkInfo.isStillAlive("Minor")) {
            System.out.println("Node " + chunkID + " has not sent a heartbeat");
            chunkServerInfo.remove(chunkID);
            chunkServerSockets.remove(chunkID);
            System.out.println("Removed node from the list of chunk server nodes: " + chunkID);
        }
        if (minorHeartbeat.getCorruptFileFound()) {
            System.out.println("Corrupt file found on node: " + chunkID);
            List<String> corruptedChunks = minorHeartbeat.getCorruptedChunks();
            for (String chunk : corruptedChunks) {
                System.out.println("Corrupted chunk: " + chunk);
            }
            // contact other node with the same file to resend the chunks
            System.out.println("Contacting other nodes with the same file to resend the chunks");
        }
        if (minorHeartbeat.wereChunksAdded()) {
            System.out.println("New chunks added to node: " + chunkID);
            List<String> addedChunks = minorHeartbeat.getAddedChunks();
            for (String chunk : addedChunks) {
                System.out.println("Added chunk: " + chunk);
                String fileName = chunk.split("_")[0];
                chunkInfo.addFile(fileName, chunk.split("_")[1]);
            }
        }

    }

    public void checkHeartbeat(MajorHeartBeat majorHeartbeat) {

        System.out.println("Checking major heartbeat");
        if (chunkServerInfo.isEmpty()) {
            return;
        }
        String chunkID = majorHeartbeat.getChunkID();
        ChunkInfo chunkInfo = chunkServerInfo.get(chunkID);
        
        if (chunkInfo == null) {
            return;
        }
        if (!chunkInfo.isStillAlive("Major")) {
            System.out.println("Node " + chunkInfo.getChunkID() + " has not sent a heartbeat");
            chunkServerInfo.remove(chunkInfo.getChunkID());
            chunkServerSockets.remove(chunkInfo.getChunkID());
            System.out.println("Removed node from the list of chunk server nodes: " + chunkInfo.getChunkID());
        }
        if (majorHeartbeat.isCorruptFileFound()) {
            System.out.println("Corrupt file found on node: " + majorHeartbeat.getChunkID());
            List<String> corruptedChunks = majorHeartbeat.getCorruptedChunks();
            for (String chunk : corruptedChunks) {
                System.out.println("Corrupted chunk: " + chunk);
            }
            // contact other node with the same file to resend the chunks
            System.out.println("Contacting other nodes with the same file to resend the chunks");
        }
        if (chunkInfo.getAvailableSpace() != majorHeartbeat.getAvailableSpace()) {
            System.out.println("Available space has changed on node: " + majorHeartbeat.getChunkID());
            chunkInfo.setAvailableSpace(majorHeartbeat.getAvailableSpace());
            System.out.println("New available space: " + majorHeartbeat.getAvailableSpace());
        }
        for (Map.Entry<String, Set<Path>> entry : majorHeartbeat.getFileMap().entrySet()) {
            String fileName = entry.getKey();
            Set<Path> chunks = entry.getValue();
            if (!chunkInfo.hasFile(fileName)) {
                System.out.println("New file found on node: " + majorHeartbeat.getChunkID());
                System.out.println("File: " + fileName);
                for (Path chunk : chunks) {
                    System.out.println("Chunk: " + chunk);
                    chunkInfo.addFile(fileName, chunk.getFileName().toString().split("_")[1]);
                }
            }
        }
    }

    public List<String> getChunkServers(float fileSize) {
        if (chunkServerInfo.isEmpty()) {
            return Collections.emptyList();
        }
        // Create a list to store the chunk servers
        List<String> chunkServers = new ArrayList<>();
    
        // Loop through the chunkServerInfo map
        for (Map.Entry<String, ChunkInfo> entry : chunkServerInfo.entrySet()) {
            ChunkInfo chunkInfo = entry.getValue();
    
            // Check if the chunk server has enough space
            if (chunkInfo.getAvailableSpace() >= fileSize) {
                // Add the chunkID to the list
                chunkServers.add(chunkInfo.getChunkID());
    
                // If the list has 3 items, break the loop
                if (chunkServers.size() == 3) {
                    break;
                }
            }
        }
        // Return the list of chunk servers
        return chunkServers;
    }

    // method to handle the upload request
    public void handleUploadRequest(UploadRequest uploadRequest) throws IOException {
        // get the file size
        float fileSize = uploadRequest.getFileSize();
        // get the client node
        String clientNode = uploadRequest.getClientNode();
        System.out.println("DEBUG: Client Node Upload Request: " + clientNode);
        // get the chunk server to store the file
        List<String> chunkServer = getChunkServers(fileSize);
        try {
            sendUploadResponse(this.clientSockets.get(clientNode), Protocol.SUCCESS, chunkServer);
        } catch (IOException e) {
            System.out.println("Failed to send upload response: " + e.getMessage());
            sendUploadResponse(clientSockets.get(clientNode), Protocol.FAILURE, Collections.emptyList());
        }
    }

    public void sendUploadResponse(TCPSender sender, byte status, List<String> chunkServers) throws IOException {
        UploadResponse response = new UploadResponse(status, chunkServers);
        sendMessageToNode(sender, response.getBytes());
        System.out.println("Sending Upload Response: \n" + response.getInfo());
    }

    public void handleDownloadRequest(DownloadRequest downloadRequest) {
        String fileName = downloadRequest.getFileName();
        Path filePath = downloadRequest.getFilePath();
        String client = downloadRequest.getClient();
        System.out.println("DEBUG: Client Node Download Request: " + client);
        Map<String, Set<Integer>> chunkServersToContact = new HashMap<>();
        Set<Integer> addedChunks = new HashSet<>(); // to keep track of added chunks
        for (Map.Entry<String, ChunkInfo> entry : chunkServerInfo.entrySet()) {
            ChunkInfo chunkInfo = entry.getValue();  // get the chunkInfo object
            if (chunkInfo.hasFile(fileName)) { // check if the chunk server has the file
                System.out.println("Chunk server " + entry.getKey() + " has the file: " + fileName);
                // check if the chunk server has the currentChunk chunk
                for (String chunkFile : chunkInfo.getChunksForFile(fileName)) {
                    String[] parts = chunkFile.split("chunk");
                    if (parts.length > 1) {
                        int currentChunk = Integer.parseInt(parts[1]);
                        if (!addedChunks.contains(currentChunk)) {
                            Set<Integer> chunks = chunkServersToContact.getOrDefault(entry.getKey(), new HashSet<>());
                            chunks.add(currentChunk);
                            chunkServersToContact.put(entry.getKey(), chunks);
                            addedChunks.add(currentChunk); // add the chunk to addedChunks set
                        }
                    } else {
                        System.out.println("Chunk file does not contain chunk: " + chunkFile);
                    }
                }
            }
        }
        if (chunkServersToContact.isEmpty()) {
            System.out.println("File not found: " + fileName);
            return;
        }
        try {
            System.out.println("Sending download response to client: " + client);
            sendDownloadResponse(clientSockets.get(client), Protocol.SUCCESS, filePath, chunkServersToContact);
        } catch (IOException e) {
            System.out.println("Failed to send download response: " + e.getMessage());
            try{
                sendDownloadResponse(clientSockets.get(client), Protocol.FAILURE, filePath, Collections.emptyMap());
            } catch (IOException ex) {
                System.out.println("Failed to send download response: " + ex.getMessage());
            }
        }
    }

    public void sendDownloadResponse(TCPSender sender, byte status, Path filePath, Map<String, Set<Integer>> chunkServersToContact) throws IOException {

        DownloadResponse response = new DownloadResponse(status, filePath, chunkServersToContact);
        sendMessageToNode(sender, response.getBytes());
        System.out.println("Sending Download Response: \n" + response.getInfo());
    }
        

    public void onEvent(Event event, Socket socket) throws IOException {

        switch (event.getType()) {
            case Protocol.REGISTER_REQUEST:
                // cast the event to a RegisterRequest
                RegisterRequest request = (RegisterRequest) event;
                System.out.println("Printing Register Request Info: \n" + request.getInfo());
                String hostname = request.getIpAddress();
                int port = request.getPortNumber();
                boolean isClient = request.isClient();
                // Register the node and send a success response
                registerNode(hostname, port, socket, isClient);

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
                checkHeartbeat(minorHeartbeat);
                break;
            case Protocol.MAJOR_HEARTBEAT:
                // cast the event to a MinorHeartBeat
                MajorHeartBeat majorHeartbeat = (MajorHeartBeat) event;
                System.out.println("Printing Major Heartbeat Info: \n" + majorHeartbeat.getInfo());
                checkHeartbeat(majorHeartbeat);
                break;

            case Protocol.UPLOAD_REQUEST:
                // cast the event to a UploadRequest
                UploadRequest uploadRequest = (UploadRequest) event;
                System.out.println("Printing Upload Request Info: \n" + uploadRequest.getInfo());
                handleUploadRequest(uploadRequest);
                break;
            
            case Protocol.DOWNLOAD_REQUEST:
                // cast the event to a DownloadRequest
                DownloadRequest downloadRequest = (DownloadRequest) event;
                System.out.println("Printing Download Request Info: \n" + downloadRequest.getInfo());
                handleDownloadRequest(downloadRequest);
                break;
            default:
                System.out.println("Unknown event type: " + event.getType());
                break;
        }
    }
    

    // Main method to run the controllerNode
    // gradle build
    // ~/CS555/hw3/build/classes/java/main$ java csx55.dfs.Controller 45559
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