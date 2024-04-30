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
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));
    
        dout.writeInt(messageType);
        byte[] chunkIDBytes = chunkID.getBytes();
        dout.writeInt(chunkIDBytes.length);
        dout.write(chunkIDBytes);
        dout.writeBoolean(addedChunks);
        dout.writeInt(newlyAddedChunks != null ? newlyAddedChunks.size() : 0);
        if (newlyAddedChunks != null) {
            for (String chunk : newlyAddedChunks) {
                byte[] chunkBytes = chunk.getBytes();
                dout.writeInt(chunkBytes.length);
                dout.write(chunkBytes);
            }
        }
        dout.writeBoolean(corruptFileFound);
        dout.writeInt(corruptedChunks != null ? corruptedChunks.size() : 0);
        if (corruptedChunks != null) {
            for (String chunk : corruptedChunks) {
                byte[] chunkBytes = chunk.getBytes();
                dout.writeInt(chunkBytes.length);
                dout.write(chunkBytes);
            }
        }
        dout.flush();
        byte[] marshalledBytes = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        dout.close();
    
        return marshalledBytes;
    }
    
    public void setBytes(byte[] marshalledBytes) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(marshalledBytes);
        DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
    
        messageType = din.readInt();
        int chunkIDLength = din.readInt();
        byte[] chunkIDBytes = new byte[chunkIDLength];
        din.readFully(chunkIDBytes);
        chunkID = new String(chunkIDBytes);
        addedChunks = din.readBoolean();
        int numNewlyAddedChunks = din.readInt();
        newlyAddedChunks = new ArrayList<>();
        for (int i = 0; i < numNewlyAddedChunks; i++) {
            int chunkLength = din.readInt();
            byte[] chunkBytes = new byte[chunkLength];
            din.readFully(chunkBytes);
            newlyAddedChunks.add(new String(chunkBytes));
        }
        corruptFileFound = din.readBoolean();
        int numCorruptedChunks = din.readInt();
        corruptedChunks = new ArrayList<>();
        for (int i = 0; i < numCorruptedChunks; i++) {
            int chunkLength = din.readInt();
            byte[] chunkBytes = new byte[chunkLength];
            din.readFully(chunkBytes);
            corruptedChunks.add(new String(chunkBytes));
        }
        baInputStream.close();
        din.close();
    }
}