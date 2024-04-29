package csx55.wireformats;

/* message type interface class */
public class Protocol {
    public static final int REGISTER_REQUEST = 1;
    public static final int REGISTER_RESPONSE = 2;
    public static final int DEREGISTER_REQUEST = 3;
    public static final int DEREGISTER_RESPONSE = 4;
    public static final byte SUCCESS = 5;
    public static final byte FAILURE = 6;
    public static final int FIND_SUCCESSOR_REQUEST = 7;
    public static final int FIND_SUCCESSOR_RESPONSE = 8;
    public static final int JOIN_REQUEST = 9;
    public static final int JOIN_RESPONSE = 10;
    public static final int NOTIFY_PREDECESSOR = 11;
    public static final int STABILIZE = 12;
    public static final int STABILIZE_RESPONSE = 13;
    public static final int PEER_EXIT = 14;
    public static final int DOWNLOAD_REQUEST = 15;
    public static final int DOWNLOAD_RESPONSE = 16;
    public static final int MIGRATION = 17;
    public static final int MIGRATION_RESPONSE = 18;
}
