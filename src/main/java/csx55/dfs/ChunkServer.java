package csx55.dfs;

import java.io.File;
import java.io.IOException;


import java.util.Scanner;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;


import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import csx55.transport.TCPRecieverThread;
import csx55.transport.TCPSender;
import csx55.transport.TCPServerThread;
import csx55.wireformats.*;
import csx55.storage.*;


public class ChunkServer implements Node {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    // Associated Server Thread
    private TCPServerThread serverThread;

    // Controller Information 
    private Socket controllerSocket; // socket to connect to the controller
    private TCPSender controllerSenderSocket; // sender to send messages to the controller
    private String controllerHost; // controller host
    private int controllerPort; // controller port

    // Peer Node Information
    private String IpAddress; // ip Address
    private int portNumber = 0;  // port number
    private String node; // hostname:port

    // File Manager
    private FileHandler fileHandler;


    public ChunkServer(String hostname, int port) {
        
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

        // instantiate the file handler
        this.fileHandler = new FileHandler(this);

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
            RegisterRequest registerRequest = new RegisterRequest(IpAddress, portNumber);
            System.out.println("Printing Register Request Info: \n" + registerRequest.getInfo());
            // send the register request
            controllerSenderSocket.sendData(registerRequest.getBytes());
        } catch (IOException e) {
            System.out.println("Failed to send the register request: " + e.getMessage());
        }
    }

    public void initiateHeartBeatScheduler() {
        ScheduledExecutorService minorBeatExecutor = Executors.newSingleThreadScheduledExecutor();
        minorBeatExecutor.scheduleAtFixedRate(this::minorBeat, 0, 15, TimeUnit.SECONDS);
    
        ScheduledExecutorService majorBeatExecutor = Executors.newSingleThreadScheduledExecutor();
        majorBeatExecutor.scheduleAtFixedRate(this::majorBeat, 0, 2, TimeUnit.MINUTES);
    }

    public void minorBeat() {
        // send a minor heartbeat to the successor
            // private String chunkID;
            // private boolean addedChunks = false;
            // private List<String> newlyAddedChunks = new ArrayList<>();
            // private boolean corruptFileFound = false;
            // private List<String> corruptedChunks = new ArrayList<>();
            
        MinorHeartBeat minorHeartBeat = new MinorHeartBeat(peerID, predecessor, successor);
        // send to controller node
        controllerSenderSocket.sendData(minorHeartBeat.getBytes());
    }

    public void majorBeat() {
        // send a major heartbeat to the successor
        MajorHeartBeat majorHeartBeat = new MajorHeartBeat(peerID, predecessor, successor);
        controllerSenderSocket.sendData(majorHeartBeat.getBytes());
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

    public void deregisterNode(String controllerHost, int controllerPort) throws IOException {
        try{
            // create a new deregister request
            DeregisterRequest deregisterRequest = new DeregisterRequest(IpAddress, portNumber);
            // Send a deregister request to the controller
            controllerSenderSocket.sendData(deregisterRequest.getBytes());
        } catch (IOException e) {
            System.out.println("Failed to send the deregister request: " + e.getMessage());
        }            
    }


    // ---------------------------- On Event Handlers and helpers --------------------------------------------------

    public void deregisterHandler(DeregisterResponse deregisterResponse) {
        System.out.println("Printing Deregister Response info: \n" + deregisterResponse.getInfo());
        if (deregisterResponse.getSuccessStatus() == Protocol.SUCCESS) {
            try {
                // close the server thread
                serverThread.shutdown();
                // close the connection to the controller
                controllerSenderSocket.closeSocket();
                controllerSocket.close();
                System.out.println("Deregistered successfully");
            } catch (IOException e) {
                System.out.println("Failed to close the Server or ControllerNode socket: " + e.getMessage());
            }
        }
    }

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

    // -------------------------------------------------- On Event Switch --------------------------------------------------

    public void onEvent(Event event, Socket socket) throws IOException {

        switch (event.getType()) {

            case Protocol.DEREGISTER_REQUEST:
                // casting the event to a DeregisterRequest
                DeregisterRequest deregisterRequest = (DeregisterRequest) event;
                System.out.println("Printing Deregister Request Info: \n" + deregisterRequest.getInfo());
                break;

            case Protocol.REGISTER_RESPONSE:
                // casting the event to a RegisterResponse
                RegisterResponse registerResponse = (RegisterResponse) event;
                handleRegistrationResponse(registerResponse);
                break;
            
            case Protocol.DEREGISTER_RESPONSE:
                // casting the event to a DeregisterResponse
                DeregisterResponse deregisterResponse = (DeregisterResponse) event;
                deregisterHandler(deregisterResponse);
                break;

            case Protocol.MIGRATION:

                // cast the event to a Migration
                Migration migration = (Migration) event;
                System.out.println("Printing Incoming File Migration Info: \n" + migration.getInfo());
            
                // check if this node is responsible for storing the file
                String fileName = migration.getFileName();
                String chunkFileName = migration.getChunkFileName();
                File chunkFile = migration.getFile();
                int totalChunksBeingSent = migration.getTotalChunksBeingSent();
                List<String> forwardToTheseChunks = migration.getForwardToTheseChunks();
                
            
                if (responsibleNode.getSuccessorID() == this.getPeerID() || migration.getForce()) {                    // This node is responsible, store the file locally
                    fileHandler.storeFile(migration.getFile(), migration.getFileIdentifier());
                    // check if the file is stored locally
                    Boolean doesFileExist = fileHandler.fileExists(migration.getFileName());
                    if (doesFileExist) {
                        // create a migration response
                        MigrationResponse migrationResponse = new MigrationResponse(migration.getFileName(), true);
                        // send the migration response
                        sendToNode(migration.getOriginatingPeer(), migrationResponse);
                    } else {
                        System.out.println("Failed to store the file locally.");
                        MigrationResponse migrationResponse = new MigrationResponse(migration.getFileName(), false);
                        sendToNode(migration.getOriginatingPeer(), migrationResponse);
                    }
                } else {
                    // This node is not responsible, relay the file to the responsible node
                    System.out.println("Relaying file to node: " + responsibleNode.getSuccessorHostPort());
                    sendToNode(responsibleNode.getSuccessorHostPort(), migration);
                }
                break;

            case Protocol.MIGRATION_RESPONSE:

                // cast the event to a MigrationResponse
                MigrationResponse migrationResponse = (MigrationResponse) event;
                System.out.println("Printing Migration Response Info: \n" + migrationResponse.getInfo());
                // check success status
                if (migrationResponse.getSuccess()) {
                    System.out.println("File migrated successfully.");
                    // remove the file from the local storage and hash table
                    if (fileHandler.fileExists(migrationResponse.getFileName())) {
                        fileHandler.removeFile(migrationResponse.getFileName());
                        fileHandler.removeFromHashTable(migrationResponse.getFileName());
                    }
                } else {
                    System.out.println("Failed to migrate the file. Trying again.");
                    // resend the file
                    // File file = fileHandler.retrieveFile(migrationResponse.getFileName());
                    // if (file != null) {
                    //     int fileIdentifier = migrationResponse.getFileName().hashCode();
                    //     Migration retryMigration = new Migration(file, fileIdentifier, this.node, false);
                    //     sendToNode(retryingNode.getSuccessorHostPort(), retryMigration);
                    // }
                }
                break;

            case Protocol.DOWNLOAD_REQUEST:

                // cast the event to a DownloadRequest
                DownloadRequest downloadRequest = (DownloadRequest) event;
                System.out.println("Printing Download Request Info: \n" + downloadRequest.getInfo());
                // check if the file exists
                if (fileHandler.fileExists(downloadRequest.getFileName())) {
                    System.out.println("File found. Sending the chunk files.");
                    for (File chunk : fileHandler.downloadFile(downloadRequest.getFileName())) {
                        // create a download response
                        DownloadResponse downloadResponse = new DownloadResponse(chunk);
                        // send the download response
                        sendToNode(downloadRequest.getClient(), downloadResponse);
                    }
                    // create a download response
                    DownloadResponse downloadResponse = new DownloadResponse(fileHandler.retrieveFile(downloadRequest.getFileName()));
                    // send the download response
                    sendToNode(downloadRequest.getClient(), downloadResponse);
                } else {
                    System.out.println("File not found. Please try again.");
                    
                }
                break;
                
            default:
                System.out.println("Unknown event type: " + event.getType());
        }

    }

    
    
    // Main method to run the peerNode
    // gradle build neighbors
    // ~/CS555/hw3/build/classes/java/main$ java csx55.chord.Peer 129.82.44.146 45559
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

        ChunkServer node = new ChunkServer(controllerHost, controllerPort);
        node.bootUpNodeConnection();

        node.initiateHeartBeatScheduler();

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
                            node.deregisterNode(controllerHost, controllerPort);
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
                    case "files":
                        // Prints the list of files this peer node is responsible for. 
                        // Each file should appear on a separate line with the following format:
                        // <file-name> <hash-code>
                        node.fileHandler.printFileList();
                        break;
                    case "upload":
                        // Stores the specified file into the chord system. The file is not directly stored on the peer node where
                        // the command is issued. Instead, the file is stored in the peer node with the smallest peerID that is
                        // greater than the hash code of the file name (with extension and without path). The peer node where
                        // the command is issued is responsible for reading the file and sending it to the correct peer node. Example
                        // usage: upload work/projects/readme.txt
                        if (words.length > 1) {
                            String filePath = words[1];
                            node.fileHandler.uploadFile(filePath);
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
                            String fileName = words[1];
                            node.fileHandler.downloadFile(fileName);
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
