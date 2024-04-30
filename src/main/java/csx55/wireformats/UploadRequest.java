package csx55.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class UploadRequest implements Event{
    private int messageType = Protocol.UPLOAD_REQUEST;
    private String clientNode;
    private float fileSize;
    private String fileName; // to determine if the file is already in the system

    public UploadRequest(byte[] message) throws IOException {
        try {
            setBytes(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public UploadRequest(String clientNode, float fileSize, String fileName) throws IOException {
        this.clientNode = clientNode;
        this.fileSize = fileSize;
        this.fileName = fileName;
    }

    public String getInfo() {
        return "UPLOAD_REQUEST\nClient Node (String): " + clientNode + "\nFile Size (float): " + fileSize + "\nFile Name (String): " + fileName + "\n";
    }

    public String getClientNode() {
        return clientNode;
    }

    public float getFileSize() {
        return fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public int getType() {
        return Protocol.UPLOAD_REQUEST;
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
        byte[] clientNodeBytes = clientNode.getBytes();
        dout.writeInt(clientNodeBytes.length);
        dout.write(clientNodeBytes);
        dout.writeFloat(fileSize);
        byte[] fileNameBytes = fileName.getBytes();
        dout.writeInt(fileNameBytes.length);
        dout.write(fileNameBytes);

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
        int clientNodeLength = din.readInt();
        byte[] clientNodeBytes = new byte[clientNodeLength];
        din.readFully(clientNodeBytes);
        clientNode = new String(clientNodeBytes);
        fileSize = din.readFloat();
        int fileNameLength = din.readInt();
        byte[] fileNameBytes = new byte[fileNameLength];
        din.readFully(fileNameBytes);
        fileName = new String(fileNameBytes);

        // close the byte array input stream and the data input stream
        baInputStream.close();
        din.close(); 
    }

}
