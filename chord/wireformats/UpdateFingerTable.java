package csx55.chord.wireformats;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class UpdateFingerTable implements Event {

    private int messageType;
    private String updatePeer;
    private int finger;

    public UpdateFingerTable(String updatePeer, int finger) {
        this.messageType = Protocol.UPDATE_FINGER_TABLE.getValue();
        this.updatePeer = updatePeer;
        this.finger = finger;
    }

    public UpdateFingerTable(byte[] incomingByteArray) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingByteArray);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);

        int messageType = dataInputStream.readInt();

        int updatePeerLength = dataInputStream.readInt();
        byte[] updatePeerBytes = new byte[updatePeerLength];
        dataInputStream.readFully(updatePeerBytes);
        String updatePeer = new String(updatePeerBytes, StandardCharsets.UTF_8);

        int finger = dataInputStream.readInt();

        dataInputStream.close();
        baInputStream.close();

        this.updatePeer = updatePeer;
        this.finger = finger;
    }

    public String getUpdatePeer() {
        return this.updatePeer;
    }

    public int getFinger() {
        return this.finger;
    }


    @Override
    public byte[] getbytes() throws IOException {
        byte[] marshalledBytes;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dataOutputStream.writeInt(Protocol.UPDATE_FINGER_TABLE.getValue());

        byte[] updatePeerBytes = updatePeer.getBytes(StandardCharsets.UTF_8);
        int byteStringLength = updatePeerBytes.length;
        dataOutputStream.writeInt(byteStringLength);
        dataOutputStream.write(updatePeerBytes);

        dataOutputStream.writeInt(finger);

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
