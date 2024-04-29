package csx55.transport;

import java.net.Socket;

import csx55.dfs.Node;

import java.io.DataOutputStream;
import java.io.IOException;

/* 
This class is responsible for sending messages to other nodes in the overlay.
 */
public class TCPSender {
    
    private Socket socket; // socket to send messages to
    private DataOutputStream dout; // output stream to send messages to
    private Node senderNode; // node that will send messages 

    // constructor for TCPSender
    public TCPSender(Socket socket) throws IOException {
        this.socket = socket;  // set socket to communicate with
        dout = new DataOutputStream(socket.getOutputStream()); // set output stream to communicate with
    }

    // send message to socket (This is syncronized to prevent multiple threads from sending messages at the same time.)
    public synchronized void sendData(byte[] dataToSend) throws IOException {
        int dataLength = dataToSend.length; // get length of data to send
        dout.writeInt(dataLength); // write length of data to send
        dout.write(dataToSend, 0, dataLength); // write data to send
        dout.flush(); // flush the output stream
    }

    // Do i want these?
    public Socket getSocket() {
        return socket;
    }

    public Node getNode() {
        return senderNode;
    }

    // close the socket
    public void closeSocket() throws IOException {
        socket.close();
    }

}
