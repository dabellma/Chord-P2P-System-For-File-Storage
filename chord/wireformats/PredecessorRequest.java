package csx55.chord.wireformats;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PredecessorRequest implements Event {

    private int messageType;
    private String newNode;

    public PredecessorRequest(String newNode) {
        this.messageType = Protocol.PREDECESSOR_REQUEST.getValue();
        this.newNode = newNode;
    }

    public PredecessorRequest(byte[] incomingByteArray) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingByteArray);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);

        int messageType = dataInputStream.readInt();

        int newNodeLength = dataInputStream.readInt();
        byte[] newNodeBytes = new byte[newNodeLength];
        dataInputStream.readFully(newNodeBytes);
        String newNode = new String(newNodeBytes, StandardCharsets.UTF_8);

        dataInputStream.close();
        baInputStream.close();

        this.newNode = newNode;
    }

    public String getNewNode() {
        return this.newNode;
    }

    @Override
    public byte[] getbytes() throws IOException {
        byte[] marshalledBytes;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dataOutputStream.writeInt(Protocol.PREDECESSOR_REQUEST.getValue());

        byte[] newNodeBytes = newNode.getBytes(StandardCharsets.UTF_8);
        int byteStringLength = newNodeBytes.length;
        dataOutputStream.writeInt(byteStringLength);
        dataOutputStream.write(newNodeBytes);

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
