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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private Map<String, Set<Path>> fileMap;
    
    
    public MajorHeartBeat(byte[] message) throws IOException {
        try {
            setBytes(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public MajorHeartBeat(String chunkID, boolean corruptFileFound, 
                    List<String> corruptedFiles, float availableSpace,
                    ConcurrentHashMap<String, Set<Path>> fileMap) throws IOException {
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

    public Map<String, Set<Path>> getFileMap() {
        return fileMap;
    }


    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));
    
        dout.writeInt(messageType);
        byte[] chunkIDBytes = chunkID.getBytes();
        dout.writeInt(chunkIDBytes.length);
        dout.write(chunkIDBytes);
        dout.writeBoolean(corruptFileFound);
        dout.writeInt(corruptedChunks != null ? corruptedChunks.size() : 0);
        if (corruptedChunks != null) {
            for (String chunk : corruptedChunks) {
                byte[] chunkBytes = chunk.getBytes();
                dout.writeInt(chunkBytes.length);
                dout.write(chunkBytes);
            }
        }
        dout.writeFloat(availableSpace);
        dout.writeInt(fileMap != null ? fileMap.size() : 0);
        if (fileMap != null) {
            for (Map.Entry<String, Set<Path>> entry : fileMap.entrySet()) {
                byte[] fileNameBytes = entry.getKey().getBytes();
                dout.writeInt(fileNameBytes.length);
                dout.write(fileNameBytes);
                Set<Path> chunkIDs = entry.getValue();
                dout.writeInt(chunkIDs != null ? chunkIDs.size() : 0);
                if (chunkIDs != null) {
                    for (Path chunkID : chunkIDs) {
                        byte[] chunkListIDBytes = chunkID.toString().getBytes();
                        dout.writeInt(chunkListIDBytes.length);
                        dout.write(chunkListIDBytes);
                    }
                }
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
            Set<Path> chunkIDs = new HashSet<>();
            for (int j = 0; j < numChunks; j++) {
                int newChunkIDLength = din.readInt();
                byte[] newchunkIDBytes = new byte[newChunkIDLength];
                din.readFully(newchunkIDBytes);
                chunkIDs.add(Paths.get(new String(newchunkIDBytes)));
            }
            fileMap.put(fileName, chunkIDs);
        }
        baInputStream.close();
        din.close();
    }
}