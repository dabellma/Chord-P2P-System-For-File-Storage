package csx55.chord.wireformats;

import java.io.*;

public class ExitResponse implements Event {

    private int messageType;
    private byte successOrFailure;

    public ExitResponse(byte successOrFailure) {
        this.messageType = Protocol.EXIT_RESPONSE.getValue();
        this.successOrFailure = successOrFailure;
    }

    public ExitResponse(byte[] incomingByteArray) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingByteArray);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);

        int messageType = dataInputStream.readInt();

        byte successOrFailure = dataInputStream.readByte();

        dataInputStream.close();
        baInputStream.close();

        this.successOrFailure = successOrFailure;
    }

    public byte getSuccessOrFailure() {
        return this.successOrFailure;
    }

    @Override
    public byte[] getbytes() throws IOException {
        byte[] marshalledBytes;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dataOutputStream.writeInt(Protocol.EXIT_RESPONSE.getValue());

        dataOutputStream.writeByte(successOrFailure);

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
