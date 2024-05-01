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
    private long lastHeartbeatTime = System.currentTimeMillis();

    public ChunkInfo(String chunkID) {
        this.chunkID = chunkID;
        this.ip = chunkID.split(":")[0];
        this.port = Integer.parseInt(chunkID.split(":")[1]);
    }

    public void addFile(String fileName, String chunkID) {
        if (filesToChunks.containsKey(fileName)) {
            filesToChunks.get(fileName).add(chunkID);
        } else {
            List<String> chunks = new ArrayList<>();
            chunks.add(chunkID);
            filesToChunks.put(fileName, chunks);
        }
    }

    public boolean isStillAlive(String heartBeatType) {
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - lastHeartbeatTime;
        lastHeartbeatTime = currentTime;  // update the last heartbeat time
        
        if (heartBeatType.equals("Minor")) {
            if (timeElapsed > 20000) {  // 20000 milliseconds = 20 seconds
                System.out.println("More than 20 seconds have passed since the last minor heartbeat");
                return false;
            }
        }
        if (heartBeatType.equals("Major")) {
            if (timeElapsed > 125000) {  //  125000 milliseconds = 2 minutes and 5 seconds
                System.out.println("More than 2 minutes have passed since the last major heartbeat");
                return false;
            }
        }
        return true;
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
