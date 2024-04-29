package csx55.wireformats;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Files;

public class DownloadResponse implements Event {


    private int messageType = Protocol.DOWNLOAD_RESPONSE;
    private File file;
    private String fileName;
    
    public DownloadResponse(byte[] message) throws IOException {
        setBytes(message);
    }

    public DownloadResponse(File file) {
        this.file = file;
        this.fileName = file.getName();
    }

    public int getType() {
        return Protocol.DOWNLOAD_RESPONSE;
    }

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    public String getInfo() {
        return "Download Delivery for file: " + fileName + "\n";
    }
    
    public byte[] getBytes() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

    dout.writeInt(messageType);

    // Write the file name
    dout.writeInt(fileName.length());
    dout.writeBytes(fileName);

    // Write the file content
    byte[] fileContent = Files.readAllBytes(file.toPath());
    dout.writeInt(fileContent.length);
    dout.write(fileContent);

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

    // Read the file name
    int fileNameLength = din.readInt();
    byte[] fileNameBytes = new byte[fileNameLength];
    din.readFully(fileNameBytes);
    fileName = new String(fileNameBytes);

    // Read the file content
    int fileContentLength = din.readInt();
    byte[] fileContent = new byte[fileContentLength];
    din.readFully(fileContent);
    Files.write(Paths.get(fileName), fileContent);
    file = new File(fileName);

    baInputStream.close();
    din.close();
}
    
}
