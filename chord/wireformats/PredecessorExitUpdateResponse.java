package csx55.chord.wireformats;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PredecessorExitUpdateResponse implements Event {

    private int messageType;

    public PredecessorExitUpdateResponse() {
        this.messageType = Protocol.PREDECESSOR_EXIT_UPDATE_RESPONSE.getValue();
    }

    public PredecessorExitUpdateResponse(byte[] incomingByteArray) throws IOException {
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

        dataOutputStream.writeInt(Protocol.PREDECESSOR_EXIT_UPDATE_RESPONSE.getValue());

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
