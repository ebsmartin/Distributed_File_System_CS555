package csx55.wireformats;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DownloadRequest implements Event{

    private int messageType = Protocol.DOWNLOAD_REQUEST;
    private Path filePath;
    private String client;

    public DownloadRequest(byte[] message) throws IOException {
        setBytes(message);
    }

    public DownloadRequest(Path filePath, String client) {
        this.filePath = filePath;
        this.client = client;
    }

    public int getType() {
        return Protocol.DOWNLOAD_REQUEST;
    }

    public Path getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return filePath.getFileName().toString();
    }

    public String getClient() {
        return client;
    }


    public String getInfo() {
        return "Download Request for file: " + filePath + " from client: " + client;
    }
    
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));
    
        dout.writeInt(messageType);
        byte[] filePathBytes = filePath.toString().getBytes();
        dout.writeInt(filePathBytes.length);
        dout.write(filePathBytes);
        byte[] clientBytes = client.getBytes();
        dout.writeInt(clientBytes.length);
        dout.write(clientBytes);
        
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
        int filePathLength = din.readInt();
        byte[] filePathBytes = new byte[filePathLength];
        din.readFully(filePathBytes);
        filePath = Paths.get(new String(filePathBytes));
        int clientLength = din.readInt();
        byte[] clientBytes = new byte[clientLength];
        din.readFully(clientBytes);
        client = new String(clientBytes);
        
        baInputStream.close();
        din.close();
    }
}
