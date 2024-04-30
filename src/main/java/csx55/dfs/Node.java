package csx55.dfs;

import java.io.IOException;
import java.net.Socket;
import csx55.wireformats.Event;

public interface Node {
    /**
     * Called when an event is received by the node.
     * 
     * @param event The event that was received.
     * @param socket The socket from which the event was received.
     * @throws IOException If an I/O error occurs.
     */
    void onEvent(Event event, Socket socket) throws IOException;

}