package csx55.wireformats;

/* message type interface class */
public class Protocol {
    public static final int REGISTER_REQUEST = 1;
    public static final int REGISTER_RESPONSE = 2;
    public static final int DEREGISTER_REQUEST = 3;
    public static final int DEREGISTER_RESPONSE = 4;
    public static final byte SUCCESS = 5;
    public static final byte FAILURE = 6;
    public static final int DOWNLOAD_REQUEST = 7;
    public static final int DOWNLOAD_RESPONSE = 8;
    public static final int MIGRATION = 9;
    public static final int MIGRATION_RESPONSE = 10;
    public static final int MINOR_HEARTBEAT = 11;
    public static final int MAJOR_HEARTBEAT = 12;
}
