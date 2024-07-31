package csx55.chord.wireformats;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class SuccessorUpdate implements Event {

    private int messageType;
    private String successor;

    public SuccessorUpdate(String successor) {
        this.messageType = Protocol.SUCCESSOR_UPDATE.getValue();
        this.successor = successor;
    }

    public SuccessorUpdate(byte[] incomingByteArray) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingByteArray);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);

        int messageType = dataInputStream.readInt();

        int successorLength = dataInputStream.readInt();
        byte[] successorBytes = new byte[successorLength];
        dataInputStream.readFully(successorBytes);
        String successor = new String(successorBytes, StandardCharsets.UTF_8);

        dataInputStream.close();
        baInputStream.close();

        this.successor = successor;
    }

    public String getSuccessor() {
        return this.successor;
    }

    @Override
    public byte[] getbytes() throws IOException {
        byte[] marshalledBytes;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dataOutputStream.writeInt(Protocol.SUCCESSOR_UPDATE.getValue());

        byte[] successorBytes = successor.getBytes(StandardCharsets.UTF_8);
        int byteStringLength = successorBytes.length;
        dataOutputStream.writeInt(byteStringLength);
        dataOutputStream.write(successorBytes);

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
