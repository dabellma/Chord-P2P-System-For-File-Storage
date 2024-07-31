package csx55.chord.wireformats;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Exit implements Event {

    private int messageType;
    private String exitingPeersSuccessor;
    private String exitingPeer;
    private int numNotificationForwardsLeft;

    public Exit(String exitingPeersSuccessor, String exitingPeer, int numNotificationForwardsLeft) {
        this.messageType = Protocol.EXIT.getValue();
        this.exitingPeersSuccessor = exitingPeersSuccessor;
        this.exitingPeer = exitingPeer;
        this.numNotificationForwardsLeft = numNotificationForwardsLeft;
    }

    public Exit(byte[] incomingByteArray) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingByteArray);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);

        int messageType = dataInputStream.readInt();

        int exitingPeersSuccessorLength = dataInputStream.readInt();
        byte[] exitingPeersSuccessorBytes = new byte[exitingPeersSuccessorLength];
        dataInputStream.readFully(exitingPeersSuccessorBytes);
        String exitingPeersSuccessor = new String(exitingPeersSuccessorBytes, StandardCharsets.UTF_8);

        int exitingPeerLength = dataInputStream.readInt();
        byte[] exitinPeerBytes = new byte[exitingPeerLength];
        dataInputStream.readFully(exitinPeerBytes);
        String exitingPeer = new String(exitinPeerBytes, StandardCharsets.UTF_8);

        int numNotificationForwardsLeft = dataInputStream.readInt();

        dataInputStream.close();
        baInputStream.close();

        this.exitingPeersSuccessor = exitingPeersSuccessor;
        this.exitingPeer = exitingPeer;
        this.numNotificationForwardsLeft = numNotificationForwardsLeft;
    }


    public String getExitingPeersSuccessor() {
        return this.exitingPeersSuccessor;
    }

    public String getExitingPeer() {
        return this.exitingPeer;
    }

    public int getNumNotificationForwardsLeft() {
        return this.numNotificationForwardsLeft;
    }

    @Override
    public byte[] getbytes() throws IOException {
        byte[] marshalledBytes;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dataOutputStream.writeInt(Protocol.EXIT.getValue());

        byte[] exitingPeersSuccessorBytes = exitingPeersSuccessor.getBytes(StandardCharsets.UTF_8);
        dataOutputStream.writeInt(exitingPeersSuccessorBytes.length);
        dataOutputStream.write(exitingPeersSuccessorBytes);

        byte[] exitingPeerBytes = exitingPeer.getBytes(StandardCharsets.UTF_8);
        dataOutputStream.writeInt(exitingPeerBytes.length);
        dataOutputStream.write(exitingPeerBytes);

        dataOutputStream.writeInt(numNotificationForwardsLeft);

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
