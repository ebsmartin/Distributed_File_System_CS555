package csx55.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

import java.util.List;

public class UploadResponse implements Event{
    private int messageType = Protocol.UPLOAD_RESPONSE;
    private byte successStatus;
    private List<String> chunkServers;
    

    public UploadResponse(byte[] message) throws IOException {
        try {
            setBytes(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public UploadResponse(byte success, List<String> chunkServers) throws IOException {
        this.successStatus = success;
        this.chunkServers = chunkServers;
    }

    public String getInfo() {
        return "UPLOAD_RESPONSE\nStatus Code (byte): " + successStatus + "\nChunk Servers (List<String>): " + chunkServers + "\n";
    }

    public byte getSuccessStatus() {
        return successStatus;
    }

    public List<String> getChunkServers() {
        return chunkServers;
    }

    public int getType() {
        return Protocol.UPLOAD_RESPONSE;
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
        dout.writeByte(successStatus);
        dout.writeInt(chunkServers.size());
        for (String chunkServer : chunkServers) {
            byte[] chunkServerBytes = chunkServer.getBytes();
            int chunkServerLength = chunkServerBytes.length;
            dout.writeInt(chunkServerLength);
            dout.write(chunkServerBytes);
        }

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
        successStatus = din.readByte();
        int chunkServerCount = din.readInt();
        chunkServers.clear();
        for (int i = 0; i < chunkServerCount; i++) {
            int chunkServerLength = din.readInt();
            byte[] chunkServerBytes = new byte[chunkServerLength];
            din.readFully(chunkServerBytes);
            chunkServers.add(new String(chunkServerBytes));
        }
        // close the byte array input stream and the data input stream
        baInputStream.close();
        din.close(); 
    }

}
