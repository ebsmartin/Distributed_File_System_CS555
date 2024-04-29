package csx55.wireformats;

import java.io.*;

public class FindSuccessorRequest implements Event{

    private String nodeRequestingSuccessor;
    private int messageType = Protocol.FIND_SUCCESSOR_REQUEST;
    private String ipAddress;
    private int portNumber;


    public FindSuccessorRequest(byte[] message) throws IOException {
        setBytes(message);
    }

    public FindSuccessorRequest(String nodeRequestingSuccessor) {
        this.nodeRequestingSuccessor = nodeRequestingSuccessor;
        this.ipAddress = nodeRequestingSuccessor.split(":")[0];
        this.portNumber = Integer.parseInt(nodeRequestingSuccessor.split(":")[1]);
    }

    public int getType() {
        return Protocol.FIND_SUCCESSOR_REQUEST;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getInfo() {
        return "FIND_SUCCESSOR_REQUEST\nNode to find successor: " + nodeRequestingSuccessor + "\n";
    }

    public String getNode() {
        return nodeRequestingSuccessor;
    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dout.writeInt(messageType);
        byte[] ipBytes = ipAddress.getBytes();
        dout.writeInt(ipBytes.length);
        dout.write(ipBytes);
        dout.writeInt(portNumber);
        byte[] nodeToFindSuccessorBytes = nodeRequestingSuccessor.getBytes();
        dout.writeInt(nodeToFindSuccessorBytes.length);
        dout.write(nodeToFindSuccessorBytes);

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
        int ipLength = din.readInt();
        byte[] ipBytes = new byte[ipLength];
        din.readFully(ipBytes);
        ipAddress = new String(ipBytes);
        portNumber = din.readInt();
        int nodeToFindSuccessorLength = din.readInt();
        byte[] nodeToFindSuccessorBytes = new byte[nodeToFindSuccessorLength];
        din.readFully(nodeToFindSuccessorBytes);
        nodeRequestingSuccessor = new String(nodeToFindSuccessorBytes);

        baInputStream.close();
        din.close();
    }
}
