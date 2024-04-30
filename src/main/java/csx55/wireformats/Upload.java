package csx55.wireformats;

import java.io.*;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;

public class Upload implements Event{

    private int messageType = Protocol.UPLOAD;
    private String fileName;
    private File chunkFile;
    private String chunkFileName;
    private int totalChunksBeingSent;
    private List<String> forwardToTheseChunks;
    private byte[] fileContents;

    public Upload(byte[] message) throws IOException {
        setBytes(message);
    }

    public Upload(File chunkFile, byte[] fileContents, int totalChunksBeingSent, List<String> forwardToTheseChunks) {
        this.chunkFile = chunkFile;
        String[] parts = chunkFile.getName().split("_");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid file name format");
        }
        this.fileName = parts[0];
        this.chunkFileName = parts[1];
        this.totalChunksBeingSent = totalChunksBeingSent;
        this.forwardToTheseChunks = forwardToTheseChunks;
        this.fileContents = fileContents;
    }

    public int getType() {
        return Protocol.UPLOAD;
    }

    public String getFileName() {
        return fileName;
    }

    public File getFile() {
        return chunkFile;
    }

    public float getFileSize() {
        return chunkFile.length();
    }

    public byte[] getFileContents() {
        return fileContents;
    }

    public String getFilePath() {
        return chunkFile.getPath();
    }

    public String getChunkFileName() {
        return chunkFileName;
    }

    public int getTotalChunksBeingSent() {
        return totalChunksBeingSent;
    }

    public List<String> getForwardToTheseChunks() {
        return forwardToTheseChunks;
    }

    public void setForwardToTheseChunks(List<String> forwardToTheseChunks) {
        this.forwardToTheseChunks = forwardToTheseChunks;
    }

    public void removeChunkFromForwardList(String chunkID) {
        forwardToTheseChunks.remove(chunkID);
    }

    public String getInfo() {
        return "Migrating chunk: " + chunkFileName + " out of " + totalChunksBeingSent + " of file: " + fileName;
    }
    
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dout.writeInt(messageType);
        byte[] fileNameBytes = fileName.getBytes();
        dout.writeInt(fileNameBytes.length);
        dout.write(fileNameBytes);
        byte[] chunkFileNameBytes = chunkFileName.getBytes();
        dout.writeInt(chunkFileNameBytes.length);
        dout.write(chunkFileNameBytes);
        dout.writeInt(totalChunksBeingSent);
        dout.writeInt(forwardToTheseChunks.size());
        for (String chunk : forwardToTheseChunks) {
            byte[] chunkBytes = chunk.getBytes();
            dout.writeInt(chunkBytes.length);
            dout.write(chunkBytes);
        }
        dout.writeInt(fileContents.length);
        dout.write(fileContents);

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
        int chunkFileNameLength = din.readInt();
        byte[] chunkFileNameBytes = new byte[chunkFileNameLength];
        din.readFully(chunkFileNameBytes);
        chunkFileName = new String(chunkFileNameBytes);
        totalChunksBeingSent = din.readInt();
        int numChunks = din.readInt();
        forwardToTheseChunks.clear();
        for (int i = 0; i < numChunks; i++) {
            int chunkLength = din.readInt();
            byte[] chunkBytes = new byte[chunkLength];
            din.readFully(chunkBytes);
            forwardToTheseChunks.add(new String(chunkBytes));
        }
        int fileContentsLength = din.readInt();
        fileContents = new byte[fileContentsLength];
        din.readFully(fileContents);
    
        baInputStream.close();
        din.close();
    }
}
