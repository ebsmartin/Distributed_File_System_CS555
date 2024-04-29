package csx55.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FindSuccessorResponse implements Event {
    private int messageType = Protocol.FIND_SUCCESSOR_RESPONSE;
    private byte successStatus;
    private String successStatusString;
    private String successorNode;

    public FindSuccessorResponse(byte[] message) {
        try {
            setBytes(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // and the NodeToReachOutTo is the node that the sender should reach out to
    // if successStatus is SUCCESS then it is the successor node
    // if successStatus is FAILURE then it is the closest preceding node
    public FindSuccessorResponse(Boolean successStatus, String successorNode) {
        if (successStatus) {
            this.successStatus = Protocol.SUCCESS;
            this.successStatusString = "SUCCESS";
        } else {
            this.successStatus = Protocol.FAILURE;
            this.successStatusString = "FAILURE";
        }
        this.successorNode = successorNode;
    }

    public int getType() {
        return Protocol.FIND_SUCCESSOR_RESPONSE;
    }

    public String getSuccessorNode() {
        return successorNode;
    }

    public String getInfo() {
        if (successStatus == Protocol.SUCCESS) {
            return "Find Successor Response: Successor Node is " + successorNode;
        } else {
            return "Failed for some reason";
        }
    }

    public byte getSuccessStatus() {
        return successStatus;
    }

    public String getSuccessStatusString() {
        return successStatusString;
    }

    public byte[] getBytes() throws IOException {
        // creating a byte array to store the marshalled bytes
        byte[] marshalledBytes = null;
        // creating a byte array output stream
        // the buffer capacity is initially 32 bytes, though its size increases if necessary.
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // wrapping the output stream in a data output stream in order to use the write methods to write data
        // creates a new data output stream to write data to the specified underlying output stream. 
        // The counter 'written' is set to zero.
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        // Writing to the output stream using the write methods
        dout.writeInt(messageType);
        dout.writeByte(successStatus);
        dout.writeUTF(successorNode);
    
        // this tells the output stream to flush its buffer
        dout.flush();
        // now we grab the byte array from the output stream, convert it to a byte array, and return it
        marshalledBytes = byteArrayOutputStream.toByteArray();
        // closing the output stream
        byteArrayOutputStream.close();
        // closing the data output stream
        dout.close();

        return marshalledBytes;
    }

    public void setBytes(byte[] marshalledBytes) throws IOException {
        // creating a byte array input stream to read the message from the byte array
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(marshalledBytes);
        // wrapping the byte array input stream in a data input stream in order to use the read methods to read data
        DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
        // reading the metadata from the input stream using the read methods
        messageType = din.readInt();
        // reads the success status from the input stream
        successStatus = din.readByte();
        if (successStatus == Protocol.SUCCESS) {
            successStatusString = "SUCCESS";
        } else {
            successStatusString = "FAILURE";
        }
        // reads the NodeToReachOutTo from the input stream
        successorNode = din.readUTF();

        // close the byte array input stream and the data input stream
        baInputStream.close();
        din.close(); 
    }
}
