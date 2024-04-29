package csx55.wireformats;

import java.io.*;

public class Stabilize implements Event {

    private int messageType = Protocol.STABILIZE;
    private String node;
    private String nodeID_nodeHostPort;

    public Stabilize(byte[] message) throws IOException {
        setBytes(message);
    }

    public Stabilize(String sendingNode) {
        this.node = sendingNode;
        this.nodeID_nodeHostPort = sendingNode.hashCode() + " " + sendingNode;
    }

    public int getType() {
        return Protocol.STABILIZE;
    }

    public String getNode() {
        return node;
    }

    public String getNodeID_nodeHostPort() {
        return nodeID_nodeHostPort;
    }

    public String getInfo() {
        return "Stabilize message:\nFrom Node: " + getNodeID_nodeHostPort() + "\n";
    }
    
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dout.writeInt(messageType);
        byte[] nodeBytes = node.getBytes();
        dout.writeInt(nodeBytes.length);
        dout.write(nodeBytes);
        byte[] nodeID_nodeHostPortBytes = nodeID_nodeHostPort.getBytes();
        dout.writeInt(nodeID_nodeHostPortBytes.length);
        dout.write(nodeID_nodeHostPortBytes);

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
        int nodeID_nodeHostPortLength = din.readInt();
        byte[] nodeID_nodeHostPortBytes = new byte[nodeID_nodeHostPortLength];
        din.readFully(nodeID_nodeHostPortBytes);
        nodeID_nodeHostPort = new String(nodeID_nodeHostPortBytes);

        baInputStream.close();
        din.close();
    }
}