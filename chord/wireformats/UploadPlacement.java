package csx55.chord.wireformats;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class UploadPlacement implements Event {

    private int messageType;
    private String fileName;
    private int fileLength;
    private byte[] file;

    public UploadPlacement(String fileName, int fileLength, byte[] file) {
        this.messageType = Protocol.UPLOAD_PLACEMENT.getValue();
        this.fileName = fileName;
        this.fileLength = fileLength;
        this.file = file;
    }

    public UploadPlacement(byte[] incomingByteArray) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingByteArray);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);

        int messageType = dataInputStream.readInt();

        int fileNameLength = dataInputStream.readInt();
        byte[] fileNameBytes = new byte[fileNameLength];
        dataInputStream.readFully(fileNameBytes);
        String fileName = new String(fileNameBytes, StandardCharsets.UTF_8);

        int fileLength = dataInputStream.readInt();

        byte[] file = dataInputStream.readNBytes(fileLength);

        dataInputStream.close();
        baInputStream.close();

        this.fileName = fileName;
        this.fileLength = fileLength;
        this.file = file;
    }

    public String getFileName() {
        return this.fileName;
    }

    public int getFileLength() {
        return fileLength;
    }

    public byte[] getFile() {
        return file;
    }

    @Override
    public byte[] getbytes() throws IOException {
        byte[] marshalledBytes;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dataOutputStream.writeInt(Protocol.UPLOAD_PLACEMENT.getValue());

        byte[] fileNameByes = fileName.getBytes(StandardCharsets.UTF_8);
        int byteStringLength = fileNameByes.length;
        dataOutputStream.writeInt(byteStringLength);
        dataOutputStream.write(fileNameByes);

        dataOutputStream.writeInt(fileLength);

        dataOutputStream.write(file);

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
