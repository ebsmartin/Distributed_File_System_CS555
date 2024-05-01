package csx55.wireformats;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class EventFactory {

    private static EventFactory instance;  // Singleton instance
    
    public static EventFactory getInstance() {
        if (instance == null) {
            instance = new EventFactory();
        }
        return instance;
    }

    public Event createEvent(byte[] message) {

        try{
            // get the event type by reading first int in the message
            // creating a byte array input stream to read the message from the byte array
            ByteArrayInputStream baInputStream = new ByteArrayInputStream(message);
            DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
            // reading the metadata from the input stream using the read methods
            int eventType = din.readInt();

            switch (eventType) {

                // all these returns are Events
                case Protocol.REGISTER_REQUEST:
                    return new RegisterRequest(message);
                case Protocol.REGISTER_RESPONSE:
                    return new RegisterResponse(message);
                case Protocol.DEREGISTER_REQUEST:
                    return new DeregisterRequest(message);
                case Protocol.DEREGISTER_RESPONSE:
                    return new DeregisterResponse(message);
                case Protocol.DOWNLOAD_REQUEST:
                    return new DownloadRequest(message);
                case Protocol.DOWNLOAD_RESPONSE:
                    return new DownloadResponse(message);
                case Protocol.UPLOAD:
                    return new Upload(message);
                case Protocol.MINOR_HEARTBEAT:
                    return new MinorHeartBeat(message);
                case Protocol.MAJOR_HEARTBEAT:
                    return new MajorHeartBeat(message);
                case Protocol.UPLOAD_REQUEST:
                    return new UploadRequest(message);
                case Protocol.UPLOAD_RESPONSE:
                    return new UploadResponse(message);
                case Protocol.DOWNLOAD_CHUNK_REQUEST:
                    return new DownloadChunkRequest(message);
                case Protocol.DOWNLOAD_CHUNK_RESPONSE:
                    return new DownloadChunkResponse(message);
                default:
                    throw new IllegalArgumentException("Invalid event type: " + eventType);
            }
        } catch (IOException e) {
            // Handle the exception
            System.err.println("Error creating event: " + e);
            return null;
        }
    }
}