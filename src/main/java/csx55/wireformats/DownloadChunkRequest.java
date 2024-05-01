package csx55.wireformats;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DownloadChunkRequest implements Event {

    private int messageType = Protocol.DOWNLOAD_CHUNK_REQUEST;
    private Path filePath;
    private int chunk;
    private String clientNode;

    public DownloadChunkRequest(byte[] message) throws IOException {
        setBytes(message);
    }

    public DownloadChunkRequest(String clientNode, Path filePath, int chunk) {
        this.filePath = filePath;
        this.chunk = chunk;
        this.clientNode = clientNode;
    }

    public int getType() {
        return Protocol.DOWNLOAD_CHUNK_REQUEST;
    }

    public Path getFilePath() {
        return filePath;
    }

    public int getChunk() {
        return chunk;
    }

    public String getClient() {
        return clientNode;
    }

    public String getInfo() {
        return "DOWNLOAD_CHUNK_REQUEST\nFile Path (Path): " + filePath + "\nChunk (int): " + chunk + "\nClient Node (String): " + clientNode + "\n";
    }


    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dout.writeInt(messageType);
        byte[] filePathBytes = filePath.toString().getBytes();
        dout.writeInt(filePathBytes.length);
        dout.write(filePathBytes);
        dout.writeInt(chunk);
        byte[] clientNodeBytes = clientNode.getBytes();
        dout.writeInt(clientNodeBytes.length);
        dout.write(clientNodeBytes);

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
        int filePathLength = din.readInt();
        byte[] filePathBytes = new byte[filePathLength];
        din.readFully(filePathBytes);
        filePath = Paths.get(new String(filePathBytes));
        chunk = din.readInt();
        int clientNodeLength = din.readInt();
        byte[] clientNodeBytes = new byte[clientNodeLength];
        din.readFully(clientNodeBytes);
        clientNode = new String(clientNodeBytes);

        baInputStream.close();
        din.close();
    }
}