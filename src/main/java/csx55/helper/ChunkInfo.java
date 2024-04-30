package csx55.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChunkInfo {

    public String chunkID;
    public int port;
    public String ip;
    public float availableSpace = 1000000;
    private Map<String, List<String>> filesToChunks = Collections.synchronizedMap(new HashMap<>());

    public ChunkInfo(String chunkID) {
        this.chunkID = chunkID;
        this.ip = chunkID.split(":")[0];
        this.port = Integer.parseInt(chunkID.split(":")[1]);
    }

    public void addFile(String fileName, List<String> chunkIDs) {
        filesToChunks.put(fileName, chunkIDs);
    }

    public List<String> getChunksForFile(String fileName) {
        return filesToChunks.get(fileName);
    }

    public void removeFile(String fileName) {
        filesToChunks.remove(fileName);
    }

    public List<String> getFiles() {
        return new ArrayList<>(filesToChunks.keySet());
    }

    public boolean hasFile(String fileName) {
        return filesToChunks.containsKey(fileName);
    }

    public boolean hasChunk(String chunkID) {
        return filesToChunks.containsValue(Collections.singletonList(chunkID));
    }

    public float getAvailableSpace() {
        return availableSpace;
    }

    public void setAvailableSpace(float availableSpace) {
        this.availableSpace = availableSpace;
    }
    public String getChunkID() {
        return chunkID;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

}
