package csx55.wireformats;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DownloadResponse implements Event {

    private int messageType = Protocol.DOWNLOAD_RESPONSE;
    private Path filePath;
    private byte status;
    private Map<String, Set<Integer>> chunkServersToContact;

    public DownloadResponse(byte[] message) throws IOException {
        setBytes(message);
    }

    public DownloadResponse(byte status, Path filePath, Map<String, Set<Integer>> chunkServersToContact) {
        this.status = status;
        this.filePath = filePath;
        this.chunkServersToContact = chunkServersToContact;
    }

    public int getType() {
        return Protocol.DOWNLOAD_RESPONSE;
    }

    public Path getFilePath() {
        return filePath;
    }

    public byte getStatus() {
        return status;
    }

    public Map<String, Set<Integer>> getChunkServersToContact() {
        return chunkServersToContact;
    }

    public String getInfo() {
        return "DOWNLOAD_RESPONSE\nFile Path (Path): " + filePath + "\nStatus (byte): " + status + "\nChunk Servers to Contact (Map<String, Set<Integer>>): " + chunkServersToContact + "\n";
    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dout.writeInt(messageType);

        // Write the file path
        String filePathString = filePath.toString();
        dout.writeInt(filePathString.length());
        dout.writeBytes(filePathString);

        // Write the status
        dout.writeByte(status);

        // Write the chunkServersToContact map
        dout.writeInt(chunkServersToContact.size());
        for (Map.Entry<String, Set<Integer>> entry : chunkServersToContact.entrySet()) {
            dout.writeInt(entry.getKey().length());
            dout.writeBytes(entry.getKey());
            dout.writeInt(entry.getValue().size());
            for (Integer chunk : entry.getValue()) {
                dout.writeInt(chunk);
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

        // Read the file path
        int filePathLength = din.readInt();
        byte[] filePathBytes = new byte[filePathLength];
        din.readFully(filePathBytes);
        filePath = Paths.get(new String(filePathBytes));

        // Read the status
        status = din.readByte();

        // Read the chunkServersToContact map
        int mapSize = din.readInt();
        chunkServersToContact = new HashMap<>();
        for (int i = 0; i < mapSize; i++) {
            int keyLength = din.readInt();
            byte[] keyBytes = new byte[keyLength];
            din.readFully(keyBytes);
            String key = new String(keyBytes);
            int setSize = din.readInt();
            Set<Integer> set = new HashSet<>();
            for (int j = 0; j < setSize; j++) {
                set.add(din.readInt());
            }
            chunkServersToContact.put(key, set);
        }

        baInputStream.close();
        din.close();
    }
}