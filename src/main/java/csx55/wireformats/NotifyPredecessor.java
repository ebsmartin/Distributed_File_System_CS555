package csx55.wireformats;

import java.io.*;

public class NotifyPredecessor implements Event {

    private int messageType = Protocol.NOTIFY_PREDECESSOR;
    private String node;
    private int peerID;

    public NotifyPredecessor(byte[] message) throws IOException {
        setBytes(message);
    }

    public NotifyPredecessor(int peerID, String node) {
        this.peerID = peerID;
        this.node = node;
    }

    public int getType() {
        return Protocol.NOTIFY_PREDECESSOR;
    }

    public String getNode() {
        return node;
    }

    public int getPeerID() {
        return peerID;
    }


    public String getInfo() {
        return "Notify_Predecessor Type (int): " + Protocol.NOTIFY_PREDECESSOR + "\nPeerID(int): " + peerID + "\nNode (String): " + node + "\n";

    }
    
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dout.writeInt(messageType);
        byte[] nodeBytes = node.getBytes();
        dout.writeInt(nodeBytes.length);
        dout.write(nodeBytes);
        dout.writeInt(peerID);
        
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
        int nodeLength = din.readInt();
        byte[] nodeBytes = new byte[nodeLength];
        din.readFully(nodeBytes);
        node = new String(nodeBytes);
        peerID = din.readInt();

        baInputStream.close();
        din.close();
    }
}