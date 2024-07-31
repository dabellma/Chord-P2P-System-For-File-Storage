package csx55.chord.wireformats;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PredecessorUpdate implements Event {

    private int messageType;
    private String predecessor;

    public PredecessorUpdate(String predecessor) {
        this.messageType = Protocol.PREDECESSOR_UPDATE.getValue();
        this.predecessor = predecessor;
    }

    public PredecessorUpdate(byte[] incomingByteArray) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingByteArray);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);

        int messageType = dataInputStream.readInt();

        int predecessorLength = dataInputStream.readInt();
        byte[] predecessorBytes = new byte[predecessorLength];
        dataInputStream.readFully(predecessorBytes);
        String predecessor = new String(predecessorBytes, StandardCharsets.UTF_8);

        dataInputStream.close();
        baInputStream.close();

        this.predecessor = predecessor;
    }

    public String getPredecessor() {
        return this.predecessor;
    }

    @Override
    public byte[] getbytes() throws IOException {
        byte[] marshalledBytes;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dataOutputStream.writeInt(Protocol.PREDECESSOR_UPDATE.getValue());

        byte[] predecessorBytes = predecessor.getBytes(StandardCharsets.UTF_8);
        int byteStringLength = predecessorBytes.length;
        dataOutputStream.writeInt(byteStringLength);
        dataOutputStream.write(predecessorBytes);

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
