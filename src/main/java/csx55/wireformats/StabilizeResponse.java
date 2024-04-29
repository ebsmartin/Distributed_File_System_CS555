package csx55.wireformats;

import java.io.*;

public class StabilizeResponse implements Event {

    private int messageType = Protocol.STABILIZE_RESPONSE;
    private String predecessor;

    public StabilizeResponse(byte[] message) throws IOException {
        setBytes(message);
    }

    public StabilizeResponse(String predecessor) {
        this.predecessor = predecessor;
    }

    public int getType() {
        return Protocol.STABILIZE_RESPONSE;
    }

    public String getPredecessor() {
        return predecessor;
    }

    public String getInfo() {
        return "Stabilize Response: " + Protocol.STABILIZE + "\nMy Predecessor is: " + predecessor + "\n";

    }
    
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dout.writeInt(messageType);
        byte[] predecessorBytes = predecessor.getBytes();
        dout.writeInt(predecessorBytes.length);
        dout.write(predecessorBytes);

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
        int predecessorLength = din.readInt();
        byte[] predecessorBytes = new byte[predecessorLength];
        din.readFully(predecessorBytes);
        predecessor = new String(predecessorBytes);

        baInputStream.close();
        din.close();
    }
}