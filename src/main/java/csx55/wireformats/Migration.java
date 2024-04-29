package csx55.wireformats;

import java.io.*;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Files;

public class Migration implements Event{

    private int messageType = Protocol.MIGRATION;
    private String fileName;
    private File file;
    private int fileIdentifier;
    private String originatingPeer;
    private Boolean force; // force migration, peer is leaving

    public Migration(byte[] message) throws IOException {
        setBytes(message);
    }

    public Migration(File file, int fileIdentifier, String originatingPeer, Boolean force) {
        this.file = file;
        this.fileName = file.getName();
        this.fileIdentifier = fileIdentifier;
        this.originatingPeer = originatingPeer;
        this.force = force;
    }

    public int getType() {
        return Protocol.MIGRATION;
    }

    public String getFileName() {
        return fileName;
    }

    public File getFile() {
        return file;
    }

    public int getFileIdentifier() {
        return fileIdentifier;
    }

    public String getOriginatingPeer() {
        return originatingPeer;
    }

    public Boolean getForce() {
        return force;
    }

    public String getInfo() {
        if (force) {
            return "Forcefully Migrating file: " + fileName + " from " + originatingPeer + "\n";
        }
        return "Migrating file: " + fileName + " from " + originatingPeer + "\n";
    }
    
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dout.writeInt(messageType);
        byte[] fileNameBytes = fileName.getBytes();
        dout.writeInt(fileNameBytes.length);
        dout.write(fileNameBytes);
        byte[] fileContent = Files.readAllBytes(file.toPath());
        dout.writeInt(fileContent.length);
        dout.write(fileContent);
        dout.writeInt(fileIdentifier);
        byte[] originatingPeerBytes = originatingPeer.getBytes();
        dout.writeInt(originatingPeerBytes.length);
        dout.write(originatingPeerBytes);
        dout.writeBoolean(force);
        
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
        int fileContentLength = din.readInt();
        byte[] fileContent = new byte[fileContentLength];
        din.readFully(fileContent);
        Files.write(Paths.get(fileName), fileContent);
        file = new File(fileName); 
        fileIdentifier = din.readInt();
        int originatingPeerLength = din.readInt();
        byte[] originatingPeerBytes = new byte[originatingPeerLength];
        din.readFully(originatingPeerBytes);
        originatingPeer = new String(originatingPeerBytes);
        force = din.readBoolean();
    
        baInputStream.close();
        din.close();
    }
}
