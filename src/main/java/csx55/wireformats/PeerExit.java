package csx55.wireformats;

import java.io.*;

public class PeerExit implements Event {

    private int messageType = Protocol.PEER_EXIT;
    private String predecessor;

    public PeerExit(byte[] message) throws IOException {
        setBytes(message);
    }

    public PeerExit(String predecessor) {
        this.predecessor = predecessor;
    }

    public int getType() {
        return Protocol.PEER_EXIT;
    }

    public String getPredecessor() {
        return predecessor;
    }

    public String getInfo() {
        return "Peer Exiting: \nMy Predecessor is: " + predecessor + "\n";

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