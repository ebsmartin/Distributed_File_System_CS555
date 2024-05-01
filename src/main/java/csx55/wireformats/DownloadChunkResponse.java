package csx55.wireformats;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DownloadChunkResponse implements Event {

    private int messageType = Protocol.DOWNLOAD_CHUNK_RESPONSE;
    private Path filePath;
    private int chunk;
    private byte[] chunkContents;
    private boolean success;

    public DownloadChunkResponse(byte[] message) throws IOException {
        setBytes(message);
    }

    public DownloadChunkResponse(Path filePath, int chunk, byte[] chunkContents, boolean success) {
        this.filePath = filePath;
        this.chunk = chunk;
        this.chunkContents = chunkContents;
        this.success = success;
    }

    public int getType() {
        return Protocol.DOWNLOAD_CHUNK_RESPONSE;
    }

    public Path getFilePath() {
        return filePath;
    }

    public int getChunk() {
        return chunk;
    }

    public byte[] getChunkContents() {
        return chunkContents;
    }

    public boolean getSuccess() {
        return success;
    }

    public String getInfo() {
        return "DOWNLOAD_CHUNK_RESPONSE\nFile Path (Path): " + filePath + "\nChunk (int): " + chunk + "\nSuccess (boolean): " + success + "\n";
    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dout.writeInt(messageType);
        byte[] filePathBytes = filePath.toString().getBytes();
        dout.writeInt(filePathBytes.length);
        dout.write(filePathBytes);
        dout.writeBoolean(success);
        dout.writeInt(chunk);
        dout.writeInt(chunkContents.length);
        dout.write(chunkContents);

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
        success = din.readBoolean();
        chunk = din.readInt();
        int chunkContentsLength = din.readInt();
        chunkContents = new byte[chunkContentsLength];
        din.readFully(chunkContents);

        baInputStream.close();
        din.close();
    }
}