package csx55.wireformats;

import java.io.*;

public class DownloadRequest implements Event{

    private int messageType = Protocol.DOWNLOAD_REQUEST;
    private String fileName;
    private String client;

    public DownloadRequest(byte[] message) throws IOException {
        setBytes(message);
    }

    public DownloadRequest(String fileName, String client) {
        this.fileName = fileName;
    }

    public int getType() {
        return Protocol.DOWNLOAD_REQUEST;
    }

    public String getFileName() {
        return fileName;
    }

    public String getClient() {
        return client;
    }


    public String getInfo() {
        return "Download Request for file: " + fileName + " from client: " + client;
    }
    
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dout.writeInt(messageType);
        byte[] fileNameBytes = fileName.getBytes();
        dout.writeInt(fileNameBytes.length);
        dout.write(fileNameBytes);
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
        int fileNameLength = din.readInt();
        byte[] fileNameBytes = new byte[fileNameLength];
        din.readFully(fileNameBytes);
        fileName = new String(fileNameBytes);
        int clientLength = din.readInt();
        byte[] clientBytes = new byte[clientLength];
        din.readFully(clientBytes);
        
        baInputStream.close();
        din.close();
    }
}
