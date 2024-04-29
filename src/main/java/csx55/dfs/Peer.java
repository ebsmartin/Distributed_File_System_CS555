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


import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import csx55.transport.TCPRecieverThread;
import csx55.transport.TCPSender;
import csx55.transport.TCPServerThread;
import csx55.wireformats.*;
import csx55.storage.*;


public class Peer implements Node {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    // Associated Server Thread
    private TCPServerThread serverThread;

    // Discovery Information 
    private Socket discoverySocket; // socket to connect to the discovery
    private TCPSender discoverySenderSocket; // sender to send messages to the discovery
    private String discoveryHost; // discovery host
    private int discoveryPort; // discovery port

    // Peer Node Information
    private String IpAddress; // ip Address
    private int portNumber = 0;  // port number
    private String node; // hostname:port
    private int peerID; // unique peer ID

    // File Manager
    private FileHandler fileHandler;

    // Neighbor Information
    String predecessor; // <peerID> <ip-addresss>:<port-number>
    String successor; // <peerID> <ip-addresss>:<port-number>

    Boolean isExiting = false;


    public Peer(String hostname, int port) {
        
        // get the local ip Address
        try {
            this.IpAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println("Failed to get local IP Address: " + e.getMessage());
        }
        // set the discovery host and port
        this.discoveryHost = hostname;
        this.discoveryPort = port;
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
        this.peerID = generateNodeID(this.node);
        this.successor = null;
        this.predecessor = null;

        // instantiate the file handler
        this.fileHandler = new FileHandler(this);

        // connect to the discovery
        try {
            setDiscoverySocket(new Socket(this.discoveryHost, this.discoveryPort));
        } catch (IOException e) {
            System.out.println("Failed to connect to the discovery: " + e.getMessage());
        }
        
        try {
            // create a TCPSender to send the register request
            TCPSender sender = new TCPSender(this.discoverySocket);
            setDiscoverySenderSocket(sender);
            // create a new thread to listen for responses from the discovery
            TCPRecieverThread reciever = new TCPRecieverThread(this.discoverySocket, this);
            new Thread(reciever).start();  // start the reciever thread
            // create the register request
            RegisterRequest registerRequest = new RegisterRequest(peerID, IpAddress, portNumber);
            System.out.println("Printing Register Request Info: \n" + registerRequest.getInfo());
            // send the register request
            discoverySenderSocket.sendData(registerRequest.getBytes());
        } catch (IOException e) {
            System.out.println("Failed to send the register request: " + e.getMessage());
        }
    }

    public void initiateStabilizationScheduler(int delay, int period) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            // send a stabilize message to the successor
            stabilize();
        }, delay, period, TimeUnit.SECONDS);
    }

    @Override
    public int generateNodeID(String node) {
        // Return the hash code of the unique identifier <IP>:<port> (this.node)
        return node.hashCode();
    }

    public void setServerThread(TCPServerThread serverThread) {
        this.serverThread = serverThread;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public void setDiscoverySocket(Socket discoverySocket) {
        this.discoverySocket = discoverySocket;
    }

    public void setDiscoverySenderSocket(TCPSender discoverySenderSocket) {
        this.discoverySenderSocket = discoverySenderSocket;
    }

    public String getIpAddress() {
        return IpAddress;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public int getPeerID() {
        return peerID;
    }

    public String getNode() {
        return node;
    }

    public void deregisterNode(int peerID, String discoveryHost, int discoveryPort) throws IOException {
        try{
            // create a new deregister request
            DeregisterRequest deregisterRequest = new DeregisterRequest(peerID, IpAddress, portNumber);
            // Send a deregister request to the discovery
            discoverySenderSocket.sendData(deregisterRequest.getBytes());
        } catch (IOException e) {
            System.out.println("Failed to send the deregister request: " + e.getMessage());
        }            
    }


    // ---------------------------- On Event Handlers and helpers --------------------------------------------------

    public void deregisterHandler(DeregisterResponse deregisterResponse) {
        System.out.println("Printing Deregister Response info: \n" + deregisterResponse.getInfo());
        if (deregisterResponse.getSuccessStatus() == Protocol.SUCCESS) {
            try {
                // let the successor know that I am leaving
                DeregisterRequest deregisterRequest = new DeregisterRequest(peerID, IpAddress, portNumber);
                sendToNode(successor.split(" ")[1], deregisterRequest);
                // close the server thread
                serverThread.shutdown();
                // close the connection to the discovery
                discoverySenderSocket.closeSocket();
                discoverySocket.close();
                System.out.println("Deregistered successfully");
            } catch (IOException e) {
                System.out.println("Failed to close the Server or DiscoveryNode socket: " + e.getMessage());
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

    public void exitChord() {
        // send a message to the successor to update its predecessor
        PeerExit peerExit = new PeerExit(this.predecessor);
        sendToNode(this.successor.split(" ")[1], peerExit);
        sleep(1000);
        // Migrate the data 
        for (String fileName : fileHandler.getFileList().keySet()) {
            int fileHash = fileName.hashCode();
            File file = fileHandler.retrieveFile(fileName);
            if (file != null) {
                System.out.println("Migrating file: " + fileName + " to node: " + this.successor.split(" ")[1]);
                Migration migration = new Migration(file, fileHash, this.node, true);
                sendToNode(this.successor.split(" ")[1], migration);
            } else {
                System.out.println("While Exiting, File not found: " + fileName);
            }
        }
    }

    public void stabilize() {
        // send a stabilize message to the successor
        // Debug
        // System.out.println("Sending Stabilize message to the successor."); 
        if (this.successor == null) {
            System.out.println("Successor is null. So, not sending the stabilize message.");
            return;
        }
        if (this.successor.equals(this.peerID + " " + this.node)) {
            System.out.println("I am the only node in the network. So, not sending the stabilize message.");
            return;
        }
        Stabilize stabilizeMessage = new Stabilize(this.node);
        sendToNode(this.successor.split(" ")[1], stabilizeMessage);
    }

    public void handleRegistrationResponse(RegisterResponse registerResponse) {
        if (registerResponse.getSuccessStatus() == Protocol.SUCCESS) {
            System.out.println("Printing Register Response Info: \n" + registerResponse.getInfo());
            String randPeer = registerResponse.getPeer();
            if (randPeer.equals("NULL")){
                System.out.println("Printing Register Response Info (NULL rand peer): \n" + registerResponse.getInfo());
                return;
            } else if (randPeer.equals(node)) {
                System.out.println("I am the only node in the network.");
                this.successor = peerID + " " + node;
                this.predecessor = peerID + " " + node;
                return;
            }
            System.out.println("Asking Random peer: " + randPeer + " who my successor is.");
            // send a message to the random peer to compute my successor
            FindSuccessorRequest findSuccessor = new FindSuccessorRequest(this.node);
            // create sender socket for the random peer
            sendToNode(randPeer, findSuccessor);
        } else {
            System.out.println("Printing Register Response Info: \n" + registerResponse.getInfo());
        }
    }

    public void findSuccessorHandler(FindSuccessorResponse findSuccessorResponse) {
        System.out.println("Printing Find Successor Response Info: \n" + findSuccessorResponse.getInfo());
        if (findSuccessorResponse.getSuccessStatus() == Protocol.SUCCESS) {
            String newSuccessor = findSuccessorResponse.getSuccessorNode();
            int newSuccessorID = newSuccessor.hashCode();
            // Debug
            System.out.println("Updated successor from : " + this.successor + " to " + newSuccessorID + "  " + newSuccessor);
            this.successor = newSuccessorID + " " + newSuccessor;
            
            // send join message to the successor
            try{
                JoinRequest joinMessage = new JoinRequest(this.node);
                sendToNode(newSuccessor, joinMessage);
            } catch (IOException e) {
                System.out.println("Failed to send join message to successor: " + e.getMessage());
            }
        } else {
            System.out.println("Failed to find the successor: " + findSuccessorResponse.getInfo());
        }
    }
    
    // -------------------------------------------------- On Event Switch --------------------------------------------------

    public void onEvent(Event event, Socket socket) throws IOException {

        switch (event.getType()) {

            case Protocol.DEREGISTER_REQUEST:
                if (isExiting) {
                    System.out.println("Already exiting. Ignoring the deregister request.");
                    return;
                }
                // casting the event to a DeregisterRequest
                DeregisterRequest deregisterRequest = (DeregisterRequest) event;
                System.out.println("Printing Deregister Request Info: \n" + deregisterRequest.getInfo());
                break;

            case Protocol.REGISTER_RESPONSE:
                if (isExiting) {
                    System.out.println("Already exiting. Ignoring the deregister request.");
                    return;
                }
                // casting the event to a RegisterResponse
                RegisterResponse registerResponse = (RegisterResponse) event;
                handleRegistrationResponse(registerResponse);
                break;

            case Protocol.PEER_EXIT:
                
                // casting the event to a PeerExit
                PeerExit peerExit = (PeerExit) event;
                System.out.println("Printing Peer Exit Info: \n" + peerExit.getInfo());
                // set my predecessor to the peerExit nodes predecessor
                this.predecessor = peerExit.getPredecessor();
                // notify predecessor to update its successor
                NotifyPredecessor exitNotifyMessage = new NotifyPredecessor(this.peerID, this.node);
                sendToNode(this.predecessor.split(" ")[1], exitNotifyMessage);
                break;
            
            case Protocol.DEREGISTER_RESPONSE:
                // casting the event to a DeregisterResponse
                DeregisterResponse deregisterResponse = (DeregisterResponse) event;
                deregisterHandler(deregisterResponse);
                break;

            case Protocol.FIND_SUCCESSOR_REQUEST:
                if (isExiting) {
                    System.out.println("Already exiting. Ignoring the deregister request.");
                    return;
                }
                // cast the event to a FindSuccessorRequest
                FindSuccessorRequest findSuccessorRequest = (FindSuccessorRequest) event;
                String requestingNode = findSuccessorRequest.getNode();
                System.out.println("Printing Find Successor Request Info: \n" + findSuccessorRequest.getInfo());

                // For the case when the successor is null, many nodes are joining at once so some may not be ready
                if (this.successor == null) {
                    // Debug
                    System.out.println("Successor is null. So, requesting new rand node from Discovery.");
                    String originalIp = findSuccessorRequest.getIpAddress();
                    int originalPort = findSuccessorRequest.getPortNumber();
                    int originalPeerID = requestingNode.hashCode();
                    RegisterRequest registerRequest = new RegisterRequest(originalPeerID, originalIp, originalPort);
                    discoverySenderSocket.sendData(registerRequest.getBytes());
                    break;
                }

                // For the case when only one node is in the network and a new node is joining
                if (this.successor.equals(this.peerID + " " + this.node) && this.predecessor.equals(this.peerID + " " + this.node)) {
                    // Debug
                    System.out.println("Only one node in the network, so adding the new node as the successor.");
                    FindSuccessorResponse requestResponse = new FindSuccessorResponse(true, this.node);
                    sendToNode(findSuccessorRequest.getNode(), requestResponse);
                    break;
                }
                
                // find the successor of the node
                // Debug
                System.out.println("Returned node after search for successor is " + result.getSuccessorID() + "  " + result.getSuccessorHostPort());
                
                if (result.getSuccessor().equals(this.successor)){
                    // Debug
                    System.out.println("Successor found. My successor is your successor.");
                    String successorHostPort = this.successor.split(" ")[1];
                    FindSuccessorResponse requestResponse = new FindSuccessorResponse(true, successorHostPort);
                    sendToNode(findSuccessorRequest.getNode(), requestResponse);
                } else if (result.getSuccessorID() == this.peerID) {
                    // Debug
                    System.out.println("I am the successor. So, sending the response to the node.");
                    // send the response to the node
                    FindSuccessorResponse requestResponse = new FindSuccessorResponse(true, this.node);
                    sendToNode(findSuccessorRequest.getNode(), requestResponse);
                } else if (result.getSuccessorID() == findSuccessorRequest.getNode().hashCode()) {
                        // Debug
                        System.out.println("The successor is the node itself. So, sending the response to the node.");
                        // send the response to the node
                        FindSuccessorResponse requestResponse = new FindSuccessorResponse(true, result.getSuccessorHostPort());
                        sendToNode(findSuccessorRequest.getNode(), requestResponse);
                        break;
                } else {
                    // Debug
                    System.out.println("The successor is not me. So, sending the request to the next node " + result.getSuccessorHostPort());
                    // send a findSuccessorRequest to the closest node (relay the message to the closest node for the requesting node)
                    FindSuccessorRequest request = new FindSuccessorRequest(requestingNode);
                    sleep(4000);
                    sendToNode(result.getSuccessorHostPort(), request);
                }
                break;

            case Protocol.FIND_SUCCESSOR_RESPONSE:
                if (isExiting) {
                    System.out.println("Already exiting. Ignoring the deregister request.");
                    return;
                }
                // cast the event to a FindSuccessorResponse
                FindSuccessorResponse findSuccessorResponse = (FindSuccessorResponse) event;
                findSuccessorHandler(findSuccessorResponse);
                break;

            case Protocol.JOIN_REQUEST:
                if (isExiting) {
                    System.out.println("Already exiting. Ignoring the deregister request.");
                    return;
                }
                // cast the event to a JoinRequest
                JoinRequest joinRequest = (JoinRequest) event;
                System.out.println("Printing Join Request Info: \n" + joinRequest.getInfo());
                String oldPredecessor = this.predecessor;
                // set my predecessor to the joinRequest node
                this.predecessor = joinRequest.getPeerID() + " " + joinRequest.getNode();
                // create a JOIN_RESPONSE message with the predecessor and successor information
                JoinResponse joinResponseMessage = new JoinResponse(oldPredecessor);
                sendToNode(joinRequest.getNode(), joinResponseMessage);
                break;

            case Protocol.JOIN_RESPONSE:
                if (isExiting) {
                    System.out.println("Already exiting. Ignoring the deregister request.");
                    return;
                }
                // cast the event to a JoinResponse
                JoinResponse joinResponse = (JoinResponse) event;
                System.out.println("Printing Join Response Info: \n" + joinResponse.getInfo());
            
                // update the predecessor of the current node
                this.predecessor = joinResponse.getPredecessor();
            
                // reach out to the predecessor to update its successor
                String[] predecessorInfo = this.predecessor.split(" ");
                String predecessorNode = predecessorInfo[1];
                // create a NOTIFY message
                NotifyPredecessor notifyMessage = new NotifyPredecessor(this.peerID, this.node);

                // send the NOTIFY message to the predecessor
                sendToNode(predecessorNode, notifyMessage);

                break;

            case Protocol.NOTIFY_PREDECESSOR:
                if (isExiting) {
                    System.out.println("Already exiting. Ignoring the deregister request.");
                    return;
                }
                // cast the event to a NotifyPredecessor
                NotifyPredecessor notify = (NotifyPredecessor) event;
                System.out.println("Printing NotifyPredecessor Info: \n" + notify.getInfo());
            
                // update the successor of the current node
                String newSuccessor = notify.getPeerID() + " " + notify.getNode();
                this.successor = newSuccessor;
            
                break;

            case Protocol.STABILIZE:
                if (isExiting) {
                    System.out.println("Already exiting. Ignoring the deregister request.");
                    return;
                }
                // cast the event to a Stabilize
                Stabilize stabilize = (Stabilize) event;
               
                if (this.predecessor == null) {
                    System.out.println("Predecessor is null. So, not sending the stabilize response.");
                    return;
                } else if (this.predecessor.equals(this.peerID + " " + this.node)) {
                    System.out.println("I am the only node in the network. So, not sending the stabilize response.");
                    return;
                } else if (!(this.predecessor.equals(stabilize.getNodeID_nodeHostPort()))) {
                    // check if the sender of the stabilize message is my predecessor
                    // if not, send a stabilizeResponse message to the sender to update its successor
                   
                    // A new node has joined the network between the current node and its predecessor
                    // create a new stabilizeResponse message and send to the sender
                    // Debug
                    System.out.println("Sending Stabilize Response to " + stabilize.getNodeID_nodeHostPort());
                    StabilizeResponse stabilizeResponse = new StabilizeResponse(this.predecessor);
                    sendToNode(stabilize.getNode(), stabilizeResponse);
                    return;
                } else {
                    // else just ignore the message, keep those nodes on their toes
                    // System.out.println("Received a Stabilize message from my predecessor. Ignoring the message.");
                    break;
                }

            case Protocol.STABILIZE_RESPONSE:
                if (isExiting) {
                    System.out.println("Already exiting. Ignoring the deregister request.");
                    return;
                }
                // cast the event to a StabilizeResponse
                StabilizeResponse stabilizeResponse = (StabilizeResponse) event;
                System.out.println("Printing Stabilize Response Info: \n" + stabilizeResponse.getInfo());
                // update the predecessor of the current node
                this.successor = stabilizeResponse.getPredecessor();
                // send a join request to the new successor
                JoinRequest joinRequestMessage = new JoinRequest(this.node);
                sendToNode(this.successor, joinRequestMessage);

                break;

            case Protocol.MIGRATION:
                if (isExiting) {
                    System.out.println("Already exiting. Ignoring the deregister request.");
                    return;
                }
                // cast the event to a Migration
                Migration migration = (Migration) event;
                System.out.println("Printing Incoming File Migration Info: \n" + migration.getInfo());
            
                // check if this node is responsible for storing the file
                String fileName = migration.getFileName();
                int fileHash = fileName.hashCode();
            
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
                if (isExiting) {
                    System.out.println("Already exiting. Ignoring the deregister request.");
                    return;
                }
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
                if (isExiting) {
                    System.out.println("Already exiting. Ignoring the deregister request.");
                    return;
                }
                // cast the event to a DownloadRequest
                DownloadRequest downloadRequest = (DownloadRequest) event;
                System.out.println("Printing Download Request Info: \n" + downloadRequest.getInfo());
                // check if the file exists
                if (fileHandler.fileExists(downloadRequest.getFileName())) {
                    System.out.println("File found. Sending the file to the requesting node.");
                    // create a download response
                    DownloadResponse downloadResponse = new DownloadResponse(fileHandler.retrieveFile(downloadRequest.getFileName()));
                    // send the download response
                    sendToNode(downloadRequest.getHops().get(0).split(" ")[1], downloadResponse);
                } else {
                    System.out.println("File not found. Please try again.");
                    
                }
                break;
            
            case Protocol.DOWNLOAD_RESPONSE:
                if (isExiting) {
                    System.out.println("Already exiting. Ignoring the deregister request.");
                    return;
                }
                // cast the event to a DownloadResponse
                DownloadResponse downloadResponse = (DownloadResponse) event;
                System.out.println("Printing Download Response Info: \n" + downloadResponse.getInfo());
                // store the file locally
                int downloadedFileNameHash = downloadResponse.getFile().getName().hashCode();
                fileHandler.storeFile(downloadResponse.getFile(), downloadedFileNameHash);
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
            System.out.println("Please provide exactly two arguments: discovery-ip discovery-port");
            return;
        }

        String discoveryHost = args[0];
        int discoveryPort;

        try {
            discoveryPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Discovery port must be an integer");
            return;
        }

        Peer node = new Peer(discoveryHost, discoveryPort);
        node.bootUpNodeConnection();
        int delayToStartStabilizer = 10;
        int periodToStabilize = 10;
        node.initiateStabilizationScheduler(delayToStartStabilizer, periodToStabilize);

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
                            System.out.println("Deregistering from the discovery...");
                            node.deregisterNode(node.peerID, discoveryHost, discoveryPort);

                            System.out.println("Exiting the network... Cleaning up...");
                            node.exitChord();
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
                        System.out.println("My Peer ID: " + node.peerID);
                        System.out.println("My IP Address: " + node.getIpAddress());
                        System.out.println("My Port Number: " + node.getPortNumber());
                        break;
                    case "neighbors":
                        // Prints information about the neighboring peer nodes in the following format:
                        // predecessor: <peerID> <ip-address>:<port>
                        // successor: <peerID> <ip-address>:<port>
                        System.out.println("Predecessor: " + node.predecessor);
                        System.out.println("Successor: " + node.successor);
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
