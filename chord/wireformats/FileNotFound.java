package csx55.chord.wireformats;

import java.io.*;

public class FileNotFound implements Event {

    private int messageType;

    public FileNotFound() {
        this.messageType = Protocol.FILE_NOT_FOUND.getValue();
    }

    public FileNotFound(byte[] incomingByteArray) throws IOException {
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

        dataOutputStream.writeInt(Protocol.FILE_NOT_FOUND.getValue());

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
