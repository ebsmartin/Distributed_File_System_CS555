package csx55.dfs;

import java.io.File;
import java.io.IOException;


import java.util.Scanner;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import csx55.transport.TCPRecieverThread;
import csx55.transport.TCPSender;
import csx55.transport.TCPServerThread;
import csx55.wireformats.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class Client implements Node {

    // Associated Server Thread
    private TCPServerThread serverThread;

    // Controller Information 
    private Socket controllerSocket; // socket to connect to the controller
    private TCPSender controllerSenderSocket; // sender to send messages to the controller
    private String controllerHost; // controller host
    private int controllerPort; // controller port

    AtomicReference<String> filePathToUpload = new AtomicReference<>(""); // file path to upload
    AtomicReference<String> fileNameToDownload = new AtomicReference<>(""); // file name to download
    AtomicInteger numberOfChunksToDownload = new AtomicInteger(0); // number of chunks to download

    CountDownLatch latch; // Declare the latch for downloading chunks

    // thread safe data structure to store file chunks before combining
    private ConcurrentHashMap<String, byte[]> fileChunks = new ConcurrentHashMap<>();
    
    // Peer Node Information
    private String IpAddress; // ip Address
    private int portNumber = 0;  // port number
    private String node; // hostname:port

    public Client(String hostname, int port) {
        
        // get the local ip Address
        try {
            this.IpAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println("Failed to get local IP Address: " + e.getMessage());
        }
        // set the controller host and port
        this.controllerHost = hostname;
        this.controllerPort = port;
    }

    public void bootUpNodeConnection() {
        // start the server thread
        try {
            setServerThread(new TCPServerThread(this.portNumber, this)); // 0 means the OS will pick a port number
            new Thread(this.serverThread).start();  // start the server thread
        } catch (IOException e) {
            System.out.println("Failed to start the server: " + e.getMessage());
        }

        // set port number to the port number that the server thread is found
        setPortNumber(serverThread.getPortNumber()); // get the port number from the server thread

        // set the node to the hostname:port
        this.node = this.IpAddress + ":" + this.portNumber;

        // connect to the controller
        try {
            setControllerSocket(new Socket(this.controllerHost, this.controllerPort));
        } catch (IOException e) {
            System.out.println("Failed to connect to the controller: " + e.getMessage());
        }
        
        try {
            // create a TCPSender to send the register request
            TCPSender sender = new TCPSender(this.controllerSocket);
            setControllerSenderSocket(sender);
            // create a new thread to listen for responses from the controller
            TCPRecieverThread reciever = new TCPRecieverThread(this.controllerSocket, this);
            new Thread(reciever).start();  // start the reciever thread
            // create the register request
            RegisterRequest registerRequest = new RegisterRequest(IpAddress, portNumber, true);
            System.out.println("Printing Register Request Info: \n" + registerRequest.getInfo());
            // send the register request
            controllerSenderSocket.sendData(registerRequest.getBytes());
        } catch (IOException e) {
            System.out.println("Failed to send the register request: " + e.getMessage());
        }
    }


    public void setServerThread(TCPServerThread serverThread) {
        this.serverThread = serverThread;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public void setControllerSocket(Socket controllerSocket) {
        this.controllerSocket = controllerSocket;
    }

    public void setControllerSenderSocket(TCPSender controllerSenderSocket) {
        this.controllerSenderSocket = controllerSenderSocket;
    }

    public String getIpAddress() {
        return IpAddress;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public String getNode() {
        return node;
    }


    // ---------------------------- On Event Handlers and helpers --------------------------------------------------


    public synchronized void sendToNode(String node, Event message){
        // node is the hostname:port of the next node
        String[] nodeInfo = node.split(":");
        String host = nodeInfo[0];
        int port = Integer.parseInt(nodeInfo[1]);
        Socket socket = null;
        try{
            socket = new Socket(host, port);
            TCPSender sender = new TCPSender(socket);
            sender.sendData(message.getBytes());
            // Disable the output stream for this socket
            // socket.shutdownOutput();
        } catch (IOException e) {
            System.out.println("Failed to send message to node: " + node + "\nerror: " + e.getMessage());
        } 
    }

    public void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // Set the interrupt status
            System.out.println("Interrupted while sleeping: " + e.getMessage());
        }
    }

    public void handleRegistrationResponse(RegisterResponse registerResponse) {
        if (registerResponse.getSuccessStatus() == Protocol.SUCCESS) {
            System.out.println("Register Response Info: \n" + registerResponse.getInfo());
        } else {
            System.out.println("Register Response Info: \n" + registerResponse.getInfo());
        }
    }

    public void handleUploadResponse(UploadResponse uploadResponse) {
        System.out.println("Upload Response Info From Controller: \n" + uploadResponse.getInfo());
        // send the file to the returned node
        if (uploadResponse.getSuccessStatus() == Protocol.SUCCESS) {
            // get the chunk servers
            List<String> chunkServers = uploadResponse.getChunkServers();
            // get the file path
            String filePath = filePathToUpload.get();
            // get the file name
            String fileName = Paths.get(filePath).getFileName().toString();
            // send the file to the chunk servers
            try {
                // Read the file into a byte array
                byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));

                // Split the byte array into 64KB chunks
                int chunkSize = 64 * 1024; // 64KB
                byte[][] chunks = new byte[(int) Math.ceil(fileBytes.length / (double) chunkSize)][];
                for (int i = 0; i < chunks.length; i++) {
                    int start = i * chunkSize;
                    int length = Math.min(chunkSize, fileBytes.length - start);
                    chunks[i] = Arrays.copyOfRange(fileBytes, start, start + length);
                }

                Socket socket = new Socket(chunkServers.get(0).split(":")[0], Integer.parseInt(chunkServers.get(0).split(":")[1]));
                TCPSender sender = new TCPSender(socket);
                int chunkNumber = 1;
                for (byte[] chunk : chunks) {
                    System.out.println("Sending chunk " + chunkNumber + " to server: " + chunkServers.get(0));
                    // create a file path for the chunk with _chunk<number> appended to the file name
                    String chunkFilePath = filePath + "_chunk" + (chunkNumber++);
                    File chunkFile = new File(chunkFilePath);
                    Upload upload = new Upload(chunkFile, chunk, chunks.length, chunkServers);
                    sender.sendData(upload.getBytes());
                }
            } catch (IOException e) {
                System.out.println("Failed to read file: " + filePath);
                e.printStackTrace();
            }
        }
    }

    public void handleDownloadResponse(DownloadResponse downloadResponse) {
        System.out.println("Download Response Info From Controller: \n" + downloadResponse.getInfo());
        if (downloadResponse.getStatus() == Protocol.SUCCESS){
            Map<String, Set<Integer>> chunkServersToContact = downloadResponse.getChunkServersToContact();
            int highestChunkNumber = Integer.MIN_VALUE;
            for (Map.Entry<String, Set<Integer>> entry : chunkServersToContact.entrySet()) {
                Set<Integer> chunks = entry.getValue();
                int maxChunkNumber = Collections.max(chunks);
                if (maxChunkNumber > highestChunkNumber) {
                    highestChunkNumber = maxChunkNumber;
                }
            }
            System.out.println("Highest chunk number: " + highestChunkNumber);
            numberOfChunksToDownload.set(highestChunkNumber);
            // Initialize the latch with the highest chunk number
            System.out.println("Initializing latch with count: " + highestChunkNumber);
            latch = new CountDownLatch(highestChunkNumber);
    
            for (Map.Entry<String, Set<Integer>> entry : chunkServersToContact.entrySet()) {
                String chunkServer = entry.getKey();
                Set<Integer> chunks = entry.getValue();
                for (Integer chunk : chunks) {
                    try {
                        // Create a DownloadChunkRequest for each chunk
                        DownloadChunkRequest request = new DownloadChunkRequest(this.node, downloadResponse.getFilePath(), chunk);
                        // Send the DownloadChunkRequest to the chunk server
                        Socket socket = new Socket(chunkServer.split(":")[0], Integer.parseInt(chunkServer.split(":")[1]));
                        TCPSender sender = new TCPSender(socket);
                        sender.sendData(request.getBytes());
                    } catch (IOException e) {
                        System.out.println("Failed to send download chunk request: " + e.getMessage());
                    }
                }
            }
        }else{
            System.out.println("Failed to download the file: " + downloadResponse.getFilePath());
        
        }
    }
    
    public void createUploadRequest(String clientNode, float fileSize, String fileName) {
        try {
            UploadRequest uploadRequest = new UploadRequest(clientNode, fileSize, fileName);
            System.out.println("Upload Request Info: \n" + uploadRequest.getInfo());
            controllerSenderSocket.sendData(uploadRequest.getBytes());
        } catch (IOException e) {
            System.out.println("Failed to create upload request: " + e.getMessage());
        }
    }

    public void handleDownloadChunkResponse(DownloadChunkResponse downloadChunkResponse) {
        System.out.println("Download Chunk Response Info: \n" + downloadChunkResponse.getInfo());
        if (downloadChunkResponse.getSuccess()) {
            // Get the chunk and its file path
            Path filePath = downloadChunkResponse.getFilePath();
            byte[] chunkContents = downloadChunkResponse.getChunkContents();
            String chunkFilePath = filePath.toString();
    
            // Store the chunk in the ConcurrentHashMap
            fileChunks.put(chunkFilePath, chunkContents);
    
            // Decrement the latch count
            latch.countDown();
            System.out.println("Latch count: " + latch.getCount());
            if (latch.getCount() == 0) {
                System.out.println("All chunks received");
                System.out.println("List of chunks received: " + fileChunks.keySet());
                combineChunks();
            }
        }
    }
    
    public void combineChunks() {
        try {
            System.out.println("Combining chunks for final download...");
            // Sort the keys of fileChunks in ascending order of chunk number
            List<String> sortedKeys = new ArrayList<>(fileChunks.keySet());
            Pattern pattern = Pattern.compile("_chunk(\\d+)");
            sortedKeys.sort(Comparator.comparingInt(key -> {
                Matcher matcher = pattern.matcher(key);
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                } else {
                    return Integer.MAX_VALUE; // or some default value
                }
            }));
                
            // Create a new byte array to store the combined chunks
            byte[] combinedChunks = new byte[0];
    
            // For each key in the sorted list, append its corresponding byte array to combinedChunks
            for (String key : sortedKeys) {
                byte[] chunk = fileChunks.get(key);
                byte[] temp = new byte[combinedChunks.length + chunk.length];
                System.arraycopy(combinedChunks, 0, temp, 0, combinedChunks.length);
                System.arraycopy(chunk, 0, temp, combinedChunks.length, chunk.length);
                combinedChunks = temp;
            }
    
            // Write the combined chunks to the final file
            Path finalDownloadPath = Paths.get(fileNameToDownload.get());
            Files.write(finalDownloadPath, combinedChunks);
            System.out.println("File downloaded successfully: " + finalDownloadPath);
        } catch (IOException e) {
            System.out.println("Failed to combine chunks: " + e.getMessage());
        }
    }


    // -------------------------------------------------- On Event Switch --------------------------------------------------

    public void onEvent(Event event, Socket socket) throws IOException {

        switch (event.getType()) {

            case Protocol.REGISTER_RESPONSE:
                // casting the event to a RegisterResponse
                RegisterResponse registerResponse = (RegisterResponse) event;
                handleRegistrationResponse(registerResponse);
                break;
            
            case Protocol.UPLOAD_RESPONSE:
                // casting the event to a UploadResponse
                UploadResponse uploadResponse = (UploadResponse) event;
                handleUploadResponse(uploadResponse);
            
                break;
            
            case Protocol.DOWNLOAD_RESPONSE:
                // casting the event to a DownloadResponse
                DownloadResponse downloadResponse = (DownloadResponse) event;
                handleDownloadResponse(downloadResponse);
                break;

            case Protocol.DOWNLOAD_CHUNK_RESPONSE:
                // casting the event to a DownloadChunkResponse
                DownloadChunkResponse downloadChunkResponse = (DownloadChunkResponse) event;
                System.out.println("Download Chunk Response Info: \n" + downloadChunkResponse.getInfo());
                handleDownloadChunkResponse(downloadChunkResponse);
                break;

            default:
                System.out.println("Unknown event type: " + event.getType());
        }
    }

    
    
    // Main method to run the peerNode
    // gradle build neighbors
    // ~/CS555/hw3/build/classes/java/main$ java csx55.dfs.Client 129.82.44.143 45559
    // -------------------------------------------------- Main Method --------------------------------------------------
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Please provide exactly two arguments: controller-ip controller-port");
            return;
        }

        String controllerHost = args[0];
        int controllerPort;

        try {
            controllerPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Controller port must be an integer");
            return;
        }

        Client node = new Client(controllerHost, controllerPort);
        node.bootUpNodeConnection();

        // Start a new thread to read commands from the console
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Enter command: ");
                String command = scanner.nextLine();
                String[] words = command.split(" ", 2);
                String action = words[0];

                switch (action) {
                    case "exit":
                        try {
                            System.out.println("Deregistering from the controller...");
                            node.sleep(5000);
                            System.out.println("Closing the scanner...");
                            scanner.close();
                            System.out.println("Exiting the program...");
                            System.exit(0);
                        } catch (Exception e) {
                            System.out.println("Failed to exit overlay: " + e.getMessage());
                        }
                        break; 
                    case "my-info":
                        System.out.println("My IP Address: " + node.getIpAddress());
                        System.out.println("My Port Number: " + node.getPortNumber());

                        break;
                    case "upload":
                        if (words.length > 1) {
                            String filePath = words[1];
                            try {
                                // Read the file into a byte array
                                Path path = Paths.get(filePath);
                                byte[] fileBytes = Files.readAllBytes(path);

                                node.filePathToUpload.set(filePath);

                                // Calculate the file size in MB
                                float fileSize = fileBytes.length / (1024.0f * 1024.0f);
                    
                                // Create and send an UploadRequest
                                node.createUploadRequest(node.getNode(), fileSize, path.getFileName().toString());
                    
                            } catch (IOException e) {
                                System.out.println("Failed to read file: " + filePath);
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("Please specify a file path to upload.");
                        }
                        break;
                   
                    case "download":
                        if (words.length > 1) {
                            // Queries the chord system and downloads the file with the given name to the current working
                            // directory. <file-name> should contain the extension, but not the original path where it was uploaded
                            // from. If the file does not exist, an error message should be printed. If the file is downloaded, the peer
                            // node should print the list of hops that was used to retrieve the file. The list should contain the starting
                            // peer and the final peer, as well as all intermediate hops. Each peer node should be on a separate line
                            // and it should be represented just by its peerID. The peer nodes should be ordered from starting node
                            // to final node. Example usage: download readme.txt
                            String filePath = words[1];
                            node.fileNameToDownload.set(filePath);
                            Path path = Paths.get(filePath);
                            try {
                                DownloadRequest downloadRequest = new DownloadRequest(path, node.getNode());
                                node.controllerSenderSocket.sendData(downloadRequest.getBytes());
                            } catch (IOException e) {
                                System.out.println("Failed to create download request: " + e.getMessage());
                            }
                        } else {
                            System.out.println("Please specify a file to download");
                        }
                        break;
                    default:
                        System.out.println("Unknown command: " + command);
                }
            }
        }).start();
    }
    
}
