package csx55.wireformats;

import java.io.*;
import java.util.ArrayList;

public class DownloadRequest implements Event{

    private int messageType = Protocol.DOWNLOAD_REQUEST;
    private String fileName;
    private int fileIdentifier;
    // list of hops taken to find the file
    ArrayList<String> hops = new ArrayList<String>();

    public DownloadRequest(byte[] message) throws IOException {
        setBytes(message);
    }

    public DownloadRequest(String fileName, int fileIdentifier, String hop) {
        this.fileName = fileName;
        this.fileIdentifier = fileIdentifier;
        hops.add(hop); // peer that sent the request
    }

    public int getType() {
        return Protocol.DOWNLOAD_REQUEST;
    }

    public String getFileName() {
        return fileName;
    }

    public int getFileIdentifier() {
        return fileIdentifier;
    }

    public ArrayList<String> getHops() {
        return hops;
    }

    public String getInfo() {
        return "Download Request for file: " + fileName + "\nFile Identifier: " + fileIdentifier + "\n"
                + "Hops taken during search so far: " + hops.toString();
    }
    
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dout.writeInt(messageType);
        byte[] fileNameBytes = fileName.getBytes();
        dout.writeInt(fileNameBytes.length);
        dout.write(fileNameBytes);
        dout.writeInt(fileIdentifier);
        dout.writeInt(hops.size());
        for (String hop : hops) {
            byte[] hopBytes = hop.getBytes();
            dout.writeInt(hopBytes.length);
            dout.write(hopBytes);
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
        int fileNameLength = din.readInt();
        byte[] fileNameBytes = new byte[fileNameLength];
        din.readFully(fileNameBytes);
        fileName = new String(fileNameBytes);
        fileIdentifier = din.readInt();
        int hopsSize = din.readInt();
        for (int i = 0; i < hopsSize; i++) {
            int hopLength = din.readInt();
            byte[] hopBytes = new byte[hopLength];
            din.readFully(hopBytes);
            hops.add(new String(hopBytes));
        }

        baInputStream.close();
        din.close();
    }
}
