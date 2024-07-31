package csx55.chord.wireformats;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Join implements Event {

    private int messageType;
    private String newPeer;
    private int numNotificationForwardsLeft;

    public Join(String newPeer, int numNotificationForwardsLeft) {
        this.messageType = Protocol.JOIN.getValue();
        this.newPeer = newPeer;
        this.numNotificationForwardsLeft = numNotificationForwardsLeft;
    }

    public Join(byte[] incomingByteArray) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingByteArray);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);

        int messageType = dataInputStream.readInt();

        int newPeerLength = dataInputStream.readInt();
        byte[] newPeerBytes = new byte[newPeerLength];
        dataInputStream.readFully(newPeerBytes);
        String newPeer = new String(newPeerBytes, StandardCharsets.UTF_8);

        int numNotificationForwardsLeft = dataInputStream.readInt();

        dataInputStream.close();
        baInputStream.close();

        this.newPeer = newPeer;
        this.numNotificationForwardsLeft = numNotificationForwardsLeft;
    }

    public String getNewPeer() {
        return this.newPeer;
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

        dataOutputStream.writeInt(Protocol.JOIN.getValue());

        byte[] newPeerBytes = newPeer.getBytes(StandardCharsets.UTF_8);
        dataOutputStream.writeInt(newPeerBytes.length);
        dataOutputStream.write(newPeerBytes);

        dataOutputStream.writeInt(numNotificationForwardsLeft);

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
