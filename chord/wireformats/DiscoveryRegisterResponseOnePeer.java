package csx55.chord.wireformats;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class DiscoveryRegisterResponseOnePeer implements Event {

    private int messageType;

    public DiscoveryRegisterResponseOnePeer() {
        this.messageType = Protocol.DISCOVERY_REGISTER_RESPONSE_ONE_PEER.getValue();
    }

    public DiscoveryRegisterResponseOnePeer(byte[] incomingByteArray) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingByteArray);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);

        int messageType = dataInputStream.readInt();

        dataInputStream.close();
        baInputStream.close();

    }

    @Override
    public byte[] getbytes() throws IOException {
        byte[] marshalledBytes;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dataOutputStream.writeInt(Protocol.DISCOVERY_REGISTER_RESPONSE_ONE_PEER.getValue());

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
