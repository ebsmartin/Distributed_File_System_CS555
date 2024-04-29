package csx55.wireformats;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;

public class MigrationResponse implements Event {


    private int messageType = Protocol.MIGRATION_RESPONSE;
    private String fileName;
    private boolean success = true;
    
    public MigrationResponse(byte[] message) throws IOException {
        setBytes(message);
    }

    public MigrationResponse(String fileName, boolean success) {
        this.fileName = fileName;
        this.success = success;
    }

    public int getType() {
        return Protocol.MIGRATION_RESPONSE;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean getSuccess() {
        return success;
    }

    public String getInfo() {
        return "Migration " + success + " for file: " + fileName + "\n";
    }
    
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dout.writeInt(messageType);

        // Write the file name
        dout.writeInt(fileName.length());
        dout.writeBytes(fileName);

        // Write the success status
        dout.writeBoolean(success);

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

        // Read the success status
        success = din.readBoolean();

        baInputStream.close();
        din.close();
    }
    
}
