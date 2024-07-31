package csx55.chord.wireformats;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PredecessorExitUpdate implements Event {

    private int messageType;
    private String predecessor;
    private String leavingNode;

    public PredecessorExitUpdate(String predecessor, String leavingNode) {
        this.messageType = Protocol.PREDECESSOR_EXIT_UPDATE.getValue();
        this.predecessor = predecessor;
        this.leavingNode = leavingNode;
    }

    public PredecessorExitUpdate(byte[] incomingByteArray) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingByteArray);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);

        int messageType = dataInputStream.readInt();

        int predecessorLength = dataInputStream.readInt();
        byte[] predecessorBytes = new byte[predecessorLength];
        dataInputStream.readFully(predecessorBytes);
        String predecessor = new String(predecessorBytes, StandardCharsets.UTF_8);

        int leavingNodeLength = dataInputStream.readInt();
        byte[] leavingNodeBytes = new byte[leavingNodeLength];
        dataInputStream.readFully(leavingNodeBytes);
        String leavingNode = new String(leavingNodeBytes, StandardCharsets.UTF_8);

        dataInputStream.close();
        baInputStream.close();

        this.predecessor = predecessor;
        this.leavingNode = leavingNode;
    }

    public String getPredecessor() {
        return this.predecessor;
    }

    public String getLeavingNode() {
        return this.leavingNode;
    }

    @Override
    public byte[] getbytes() throws IOException {
        byte[] marshalledBytes;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dataOutputStream.writeInt(Protocol.PREDECESSOR_EXIT_UPDATE.getValue());

        byte[] predecessorBytes = predecessor.getBytes(StandardCharsets.UTF_8);
        int byteStringLength = predecessorBytes.length;
        dataOutputStream.writeInt(byteStringLength);
        dataOutputStream.write(predecessorBytes);

        byte[] leavingNodeBytes = leavingNode.getBytes(StandardCharsets.UTF_8);
        int leavingNodeByteStringLength = leavingNodeBytes.length;
        dataOutputStream.writeInt(leavingNodeByteStringLength);
        dataOutputStream.write(leavingNodeBytes);

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
