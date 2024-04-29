package csx55.transport;

import java.net.Socket;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.SocketException;

import csx55.dfs.Node;
import csx55.wireformats.Event;
import csx55.wireformats.EventFactory;

public class TCPRecieverThread implements Runnable {

    private Socket socket;      // socket to receive messages from
    private DataInputStream din;  // input stream to receive messages from
    private Node recieverNode;        // reciever node

    public TCPRecieverThread(Socket socket, Node node) throws IOException {
        this.socket = socket;  // set socket to communicate with
        din = new DataInputStream(socket.getInputStream()); // set input stream to communicate with
        this.recieverNode = node;
    }

    public void run() {

        int dataLength = -1; // length of data to receive
            byte[] data = null; // Initialize data to null

        while (socket != null) {
            try {
                dataLength = din.readInt(); // read length of data to receive
                data = new byte[dataLength]; // create byte array to store data to receive
                /* Parameters of readFully() method:
                b - the buffer into which the data is read.
                off - the start offset of the data.
                len - the number of bytes to read.
                */
                din.readFully(data, 0, dataLength); // read data to receive
                EventFactory eventFactory = new EventFactory();
                Event event = eventFactory.createEvent(data); // create event from data
                recieverNode.onEvent(event, socket);  // I am adding the socket as a param so I can send messages back

            } catch (SocketException se) {
                // System.out.println("Socket exception in TCPRecieverThread: " + se.getMessage());
                break;
            }
            catch (IOException ioe) {
                // System.out.println("IOException in TCPRecieverThread: " + ioe.getMessage());
                // System.out.println("Data length: " + dataLength);
                // System.out.println("Data array length: " + data.length);
                // ioe.printStackTrace();
                break;
            }
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public Node getNode() {
        return recieverNode;
    }

    public void closeSocket() throws IOException {
        socket.close();
    }
}
