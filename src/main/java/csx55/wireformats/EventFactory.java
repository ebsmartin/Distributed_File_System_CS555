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
                case Protocol.FIND_SUCCESSOR_REQUEST:
                    return new FindSuccessorRequest(message);
                case Protocol.FIND_SUCCESSOR_RESPONSE:
                    return new FindSuccessorResponse(message);
                case Protocol.JOIN_REQUEST:
                    return new JoinRequest(message);
                case Protocol.JOIN_RESPONSE:
                    return new JoinResponse(message);
                case Protocol.NOTIFY_PREDECESSOR:
                    return new NotifyPredecessor(message);
                case Protocol.STABILIZE:
                    return new Stabilize(message);
                case Protocol.STABILIZE_RESPONSE:
                    return new StabilizeResponse(message);
                case Protocol.PEER_EXIT:
                    return new PeerExit(message);
                case Protocol.DOWNLOAD_REQUEST:
                    return new DownloadRequest(message);
                case Protocol.DOWNLOAD_RESPONSE:
                    return new DownloadResponse(message);
                case Protocol.MIGRATION:
                    return new Migration(message);
                case Protocol.MIGRATION_RESPONSE:
                    return new MigrationResponse(message);
                default:
                    throw new IllegalArgumentException("Invalid event type: " + eventType);
            }
        } catch (IOException e) {
            // Handle the exception
            System.err.println("Error creating event: " + e);
            return null;
        }
    }

    public static void main(String[] args) {
        EventFactory factory = EventFactory.getInstance();
        try {
            // Create a RegisterRequest
            RegisterRequest originalRequest = new RegisterRequest(12345, "129.82.44.246", 49355);
            System.out.println("Original request type: " + originalRequest.getType());
    
            // Get the bytes of the original request
            byte[] bytes = originalRequest.getBytes();
    
            // Create a new RegisterRequest and set its bytes to the bytes of the original request
            RegisterRequest newRequest = new RegisterRequest(bytes);
            System.out.println("New request type: " + newRequest.getType());
    
            // The new request type should now be equal to the original request type
            if (originalRequest.getType() == newRequest.getType()) {
                System.out.println("Test passed: The types are equal.");
            } else {
                System.out.println("Test failed: The types are not equal.");
            }
        
            factory.createEvent(bytes);

        } catch (IOException e) {
            System.out.println("Exception should not be thrown: " + e.getMessage());
        }
    
    }
}