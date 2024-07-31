package csx55.chord.wireformats;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DownloadResponse implements Event {

    private int messageType;
    private String fileName;
    private int fileLength;
    private byte[] file;
    private int lengthPathTaken;
    private List<String> pathTaken;

    public DownloadResponse(String fileName, int fileLength, byte[] file, int lengthPathTaken, List<String> pathTaken) {
        this.messageType = Protocol.DOWNLOAD_RESPONSE.getValue();
        this.fileName = fileName;
        this.fileLength = fileLength;
        this.file = file;
        this.lengthPathTaken = lengthPathTaken;
        this.pathTaken = pathTaken;
    }

    public DownloadResponse(byte[] incomingByteArray) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingByteArray);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);
        List<String> pathTaken = new ArrayList<>();

        int messageType = dataInputStream.readInt();

        int fileNameLength = dataInputStream.readInt();
        byte[] fileNameBytes = new byte[fileNameLength];
        dataInputStream.readFully(fileNameBytes);
        String fileName = new String(fileNameBytes, StandardCharsets.UTF_8);

        int fileLength = dataInputStream.readInt();

        byte[] file = dataInputStream.readNBytes(fileLength);

        int lengthPathTaken = dataInputStream.readInt();

        for (int i = 0; i < lengthPathTaken; i++) {

            int peerNameSize = dataInputStream.readInt();
            byte[] peerBytes = new byte[peerNameSize];
            dataInputStream.readFully(peerBytes);
            String peer = new String(peerBytes, StandardCharsets.UTF_8);

            pathTaken.add(peer);
        }

        dataInputStream.close();
        baInputStream.close();

        this.fileName = fileName;
        this.fileLength = fileLength;
        this.file = file;
        this.lengthPathTaken = lengthPathTaken;
        this.pathTaken = pathTaken;
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


    public int getLengthPathTaken() {
        return this.lengthPathTaken;
    }
    public List<String> getPathTaken() {
        return this.pathTaken;
    }

    @Override
    public byte[] getbytes() throws IOException {
        byte[] marshalledBytes;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dataOutputStream.writeInt(Protocol.DOWNLOAD_RESPONSE.getValue());

        byte[] fileNameByes = fileName.getBytes(StandardCharsets.UTF_8);
        int byteStringLength = fileNameByes.length;
        dataOutputStream.writeInt(byteStringLength);
        dataOutputStream.write(fileNameByes);

        dataOutputStream.writeInt(fileLength);

        dataOutputStream.write(file);

        dataOutputStream.writeInt(lengthPathTaken);

        for (String peer : pathTaken) {
            byte[] peerBytes = peer.getBytes(StandardCharsets.UTF_8);
            dataOutputStream.writeInt(peerBytes.length);
            dataOutputStream.write(peerBytes);
        }

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
