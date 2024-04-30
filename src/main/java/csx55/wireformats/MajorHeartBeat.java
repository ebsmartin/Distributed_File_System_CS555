package csx55.wireformats;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MajorHeartBeat implements Event{
    private int messageType = Protocol.MAJOR_HEARTBEAT;
    private String chunkID;
    private boolean corruptFileFound = false;
    private List<String> corruptedChunks = new ArrayList<>();
    private float availableSpace;
    private Map<String, List<Path>> fileMap;
    
    
    public MajorHeartBeat(byte[] message) throws IOException {
        try {
            setBytes(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public MajorHeartBeat(String chunkID, boolean corruptFileFound, 
                    List<String> corruptedFiles, float availableSpace,
                    ConcurrentHashMap<String, List<Path>> fileMap) throws IOException {
        this.chunkID = chunkID;
        this.corruptFileFound = corruptFileFound;
        this.corruptedChunks = corruptedFiles == null ? new ArrayList<>() : corruptedFiles;
        this.availableSpace = availableSpace;
        this.fileMap = new ConcurrentHashMap<>(fileMap); // copy the ConcurrentHashMap
    }

    public String getInfo() {
        return "MAJOR_HEARTBEAT\nChunk ID (String): " + chunkID + "\nCorrupt File Found (boolean): " + corruptFileFound + "\nCorrupted Chunks (List<String>): " + corruptedChunks + "\nAvailable Space (int): " + availableSpace + "\nFiles to Chunks (Map<String, List<String>>): " + fileMap + "\n";
    }

    public String getChunkID() {
        return chunkID;
    }

    public int getType() {
        return Protocol.MAJOR_HEARTBEAT;
    }

    public float getAvailableSpace() {
        return availableSpace;
    }

    public boolean isCorruptFileFound() {
        return corruptFileFound;
    }

    public List<String> getCorruptedChunks() {
        return corruptedChunks;
    }

    public Map<String, List<Path>> getFileMap() {
        return fileMap;
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
        dout.writeBoolean(corruptFileFound);
        dout.writeInt(corruptedChunks.size());
        for (String chunk : corruptedChunks) {
            byte[] chunkBytes = chunk.getBytes();
            dout.writeInt(chunkBytes.length);
            dout.write(chunkBytes);
        }
        dout.writeFloat(availableSpace);
        dout.writeInt(fileMap.size());
        for (Map.Entry<String, List<Path>> entry : fileMap.entrySet()) {
            byte[] fileNameBytes = entry.getKey().getBytes();
            dout.writeInt(fileNameBytes.length);
            dout.write(fileNameBytes);
            List<Path> chunkIDs = entry.getValue();
            dout.writeInt(chunkIDs.size());
            for (Path chunkID : chunkIDs) {
                byte[] chunkListIDBytes = chunkID.toString().getBytes(); // convert Path to String
                dout.writeInt(chunkListIDBytes.length);
                dout.write(chunkListIDBytes);
            }
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
        corruptFileFound = din.readBoolean();
        int numCorruptedChunks = din.readInt();
        corruptedChunks = new ArrayList<>();
        for (int i = 0; i < numCorruptedChunks; i++) {
            int chunkLength = din.readInt();
            byte[] chunkBytes = new byte[chunkLength];
            din.readFully(chunkBytes);
            corruptedChunks.add(new String(chunkBytes));
        }
        availableSpace = din.readFloat();
        int numFiles = din.readInt();
        fileMap = new HashMap<>();
        for (int i = 0; i < numFiles; i++) {
            int fileNameLength = din.readInt();
            byte[] fileNameBytes = new byte[fileNameLength];
            din.readFully(fileNameBytes);
            String fileName = new String(fileNameBytes);
            int numChunks = din.readInt();
            List<Path> chunkIDs = new ArrayList<>();
            for (int j = 0; j < numChunks; j++) {
                int newChunkIDLength = din.readInt();
                byte[] newchunkIDBytes = new byte[newChunkIDLength];
                din.readFully(newchunkIDBytes);
                chunkIDs.add(Paths.get(new String(newchunkIDBytes))); // convert String to Path
            }
            fileMap.put(fileName, chunkIDs);
        }
        // close the byte array input stream and the data input stream
        baInputStream.close();
        din.close(); 
    }

}