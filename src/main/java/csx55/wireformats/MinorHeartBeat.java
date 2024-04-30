package csx55.wireformats;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.List;

public class MinorHeartBeat implements Event{
    private int messageType = Protocol.MINOR_HEARTBEAT;
    private String chunkID;
    private boolean addedChunks = false;
    private List<String> newlyAddedChunks = new ArrayList<>();
    private boolean corruptFileFound = false;
    private List<String> corruptedChunks = new ArrayList<>();
    

    public MinorHeartBeat(byte[] message) throws IOException {
        try {
            setBytes(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public MinorHeartBeat(String chunkID, boolean addedChunks, 
                    List<String> newlyAddedChunks, boolean corruptFileFound, 
                    List<String> corruptedFiles) throws IOException {
        this.chunkID = chunkID;
        this.addedChunks = addedChunks;
        this.newlyAddedChunks = newlyAddedChunks == null ? new ArrayList<>() : newlyAddedChunks;
        this.corruptFileFound = corruptFileFound;
        this.corruptedChunks = corruptedFiles == null ? new ArrayList<>() : corruptedFiles;
    }

    public String getInfo() {
        return "MINOR_HEARTBEAT\nChunk ID (String): " + chunkID + "\nAdded Chunks (boolean): " + addedChunks + "\nNewly Added Chunks (List<String>): " + newlyAddedChunks + "\nCorrupt File Found (boolean): " + corruptFileFound + "\nCorrupted Chunks (List<String>): " + corruptedChunks + "\n";
    }

    public String getChunkID() {
        return chunkID;
    }

    public int getType() {
        return Protocol.MINOR_HEARTBEAT;
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
        byte[] chunkIDBytes = chunkID.getBytes();
        dout.writeInt(chunkIDBytes.length);
        dout.write(chunkIDBytes);
        dout.writeBoolean(addedChunks);
        dout.writeInt(newlyAddedChunks.size());
        for (String chunk : newlyAddedChunks) {
            byte[] chunkBytes = chunk.getBytes();
            dout.writeInt(chunkBytes.length);
            dout.write(chunkBytes);
        }
        dout.writeBoolean(corruptFileFound);
        dout.writeInt(corruptedChunks.size());
        for (String chunk : corruptedChunks) {
            byte[] chunkBytes = chunk.getBytes();
            dout.writeInt(chunkBytes.length);
            dout.write(chunkBytes);
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
        // read the chunk ID
        int chunkIDLength = din.readInt();
        byte[] chunkIDBytes = new byte[chunkIDLength];
        din.readFully(chunkIDBytes);
        chunkID = new String(chunkIDBytes);
        // read the added chunks boolean
        addedChunks = din.readBoolean();
        // read the number of newly added chunks
        int numNewlyAddedChunks = din.readInt();
        newlyAddedChunks = new ArrayList<>();
        for (int i = 0; i < numNewlyAddedChunks; i++) {
            int chunkLength = din.readInt();
            byte[] chunkBytes = new byte[chunkLength];
            din.readFully(chunkBytes);
            newlyAddedChunks.add(new String(chunkBytes));
        }
        // read the corrupt file boolean
        corruptFileFound = din.readBoolean();
        // read the number of corrupted chunks
        int numCorruptedChunks = din.readInt();
        corruptedChunks = new ArrayList<>();
        for (int i = 0; i < numCorruptedChunks; i++) {
            int chunkLength = din.readInt();
            byte[] chunkBytes = new byte[chunkLength];
            din.readFully(chunkBytes);
            corruptedChunks.add(new String(chunkBytes));
        }
        // close the byte array input stream and the data input stream
        baInputStream.close();
        din.close(); 
    }

}