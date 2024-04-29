package csx55.wireformats;

import java.io.IOException;

public interface Event {
    /*
     * returns an integer representing the type of the event
     */
    int getType();  // get the type of the event
    /*
     * returns a byte array representing the event in a format that can be sent over a network
     */
    byte[] getBytes() throws IOException;  // get the bytes of the event

    void setBytes(byte[] bytes) throws IOException;  // set the bytes of the event
}