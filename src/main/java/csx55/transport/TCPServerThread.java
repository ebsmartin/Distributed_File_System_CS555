package csx55.transport;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import csx55.dfs.Node;

/* 
The TCPServerThread class will be a Runnable to allow it to be executed in a separate thread.
It will have a ServerSocket member to listen for incoming connections.
The constructor will initialize the ServerSocket on a given port. 
*/


public class TCPServerThread implements Runnable {

    private ServerSocket serverSocket;  // server socket to listen for incoming connections
    private List<TCPRecieverThread> receiverThreads;  // list of threads to handle incoming messages
    private Node node; // node that will handle incoming messages

    public TCPServerThread(int port, Node node) throws IOException {
        serverSocket = new ServerSocket(port);
        receiverThreads = new ArrayList<>();
        this.node = node;
    }

    public int getPortNumber() {
        return serverSocket.getLocalPort();
    }

    /*
     * Upon accepting a connection, it will create a new TCPRecieverThread to handle incoming messages from this connection.
     */
    public void run() {
        try {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept(); // Accept incoming connection
                TCPRecieverThread receiverThread = new TCPRecieverThread(clientSocket, node);
                receiverThreads.add(receiverThread);
                new Thread(receiverThread).start(); // Start a new thread for each connection
            }
        } catch (IOException e) {
            System.out.println("TCPServerThread IOException: " + e.getMessage());
        } finally {
            closeServerSocket();
        }
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing server socket: " + e.getMessage());
        }
    }

    // Add a method to close the server externally
    public void shutdown() {
        closeServerSocket();
    }
}
