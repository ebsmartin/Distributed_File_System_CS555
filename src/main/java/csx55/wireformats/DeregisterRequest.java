package csx55.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DeregisterRequest implements Event{

    private int messageType = Protocol.DEREGISTER_REQUEST;
    private String ipAddress;
    private int portNumber;


    public DeregisterRequest(byte[] message) throws IOException {
        try {
            setBytes(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DeregisterRequest(String ipAddress, int portNumber) throws IOException {
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
    }

    public int getType() {
        return Protocol.DEREGISTER_REQUEST;
    }

    public String getInfo() {
        return "DEREGISTER_REQUEST\nIP address (String): " + ipAddress + "\nPort number (int): " + portNumber + "\n";
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public String getChunkID() {
        return this.ipAddress + ":" + this.portNumber;
    }

    public byte[] getBytes() throws IOException {
        // creating a byte array to store the marshalled bytes
        byte[] marshalledBytes = null;
        // creating a byte array output stream
        // the buffer capacity is initially 32 bytes, though its size increases if necessary.
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // wrapping the output stream in a data output stream in order to use the write methods to write data
        // creates a new data output stream to write data to the specified underlying output stream. 
        // The counter 'written' is set to zero.
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        // Writing to the output stream using the write methods
        dout.writeInt(messageType);
        byte[] ipAddressBytes = ipAddress.getBytes();
        dout.writeInt(ipAddressBytes.length);
        dout.write(ipAddressBytes);
        dout.writeInt(portNumber);
    

        // this tells the output stream to flush its buffer
        dout.flush();
        // now we grab the byte array from the output stream, convert it to a byte array, and return it
        marshalledBytes = byteArrayOutputStream.toByteArray();
        // closing the output stream
        byteArrayOutputStream.close();
        // closing the data output stream
        dout.close();

        return marshalledBytes;
    }

    public void setBytes(byte[] marshalledBytes) throws IOException {

        // creating a byte array input stream to read the message from the byte array
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(marshalledBytes);
        // wrapping the byte array input stream in a data input stream in order to use the read methods to read data
        DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
        // reading the metadata from the input stream using the read methods
        messageType = din.readInt();
        // reads the length of the identifier from the input stream in order to know how many bytes to read
        int ipAddressLength = din.readInt();
        // create a byte array to store the identifier equal to the length of the identifier
        byte[] ipAddressBytes = new byte[ipAddressLength];
        // We need to use the readFully() method to read the entire identifier otherwise bad things happen
        // the data read is stored in the byte array identifierBytes
        din.readFully(ipAddressBytes);
        // convert the byte array to a string and store it in the ipAddress variable
        ipAddress = new String(ipAddressBytes);
        // read the tracker from the input stream, whatever that is
        portNumber = din.readInt();
        // close the byte array input stream and the data input stream
        baInputStream.close();
        din.close(); 
    }
    
}
