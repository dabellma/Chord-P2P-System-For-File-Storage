package csx55.chord.wireformats;

import java.io.*;

public class FileTransferResponse implements Event {

    private int messageType;

    public FileTransferResponse() {
        this.messageType = Protocol.FILE_TRANSFER_RESPONSE.getValue();
    }

    public FileTransferResponse(byte[] incomingByteArray) throws IOException {
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

        dataOutputStream.writeInt(Protocol.FILE_TRANSFER_RESPONSE.getValue());

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
