package csx55.chord.wireformats;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DownloadForward implements Event {

    private int messageType;
    private String ipAddress;
    private int portNumber;
    private String file;
    private int lengthPathTaken;
    private List<String> pathTaken;

    //contains the requesting nodes ip address and port number so the end of the chain can send to the requestor
    public DownloadForward(String ipAddress, int portNumber, String file, int lengthPathTaken, List<String> pathTaken) {
        this.messageType = Protocol.DOWNLOAD_FORWARD.getValue();
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.file = file;
        this.lengthPathTaken = lengthPathTaken;
        this.pathTaken = pathTaken;
    }

    public DownloadForward(byte[] incomingByteArray) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingByteArray);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);
        List<String> pathTaken = new ArrayList<>();

        int messageType = dataInputStream.readInt();

        int ipAddressLength = dataInputStream.readInt();
        byte[] ipAddressBytes = new byte[ipAddressLength];
        dataInputStream.readFully(ipAddressBytes);
        String ipAddress = new String(ipAddressBytes, StandardCharsets.UTF_8);

        int portNumber = dataInputStream.readInt();

        int fileLength = dataInputStream.readInt();
        byte[] fileBytes = new byte[fileLength];
        dataInputStream.readFully(fileBytes);
        String file = new String(fileBytes, StandardCharsets.UTF_8);

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

        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.file = file;
        this.lengthPathTaken = lengthPathTaken;
        this.pathTaken = pathTaken;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public int getPortNumber() {
        return this.portNumber;
    }

    public String getFile() {
        return this.file;
    }

    public int getLengthPathTaken() {
        return this.lengthPathTaken;
    }
    public List<String> getPathTaken() {
        return this.pathTaken;
    }

    public void setPathTaken(List<String> newPathTaken) {
        this.pathTaken = newPathTaken;
    }

    public void setLengthPathTaken(int newLengthPathTaken) {
        this.lengthPathTaken = newLengthPathTaken;
    }

    @Override
    public byte[] getbytes() throws IOException {
        byte[] marshalledBytes;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dataOutputStream.writeInt(Protocol.DOWNLOAD_FORWARD.getValue());

        byte[] ipAddressBytes = ipAddress.getBytes(StandardCharsets.UTF_8);
        int byteStringLength = ipAddressBytes.length;
        dataOutputStream.writeInt(byteStringLength);
        dataOutputStream.write(ipAddressBytes);

        dataOutputStream.writeInt(portNumber);

        byte[] fileBytes = file.getBytes(StandardCharsets.UTF_8);
        int fileByteStringLength = fileBytes.length;
        dataOutputStream.writeInt(fileByteStringLength);
        dataOutputStream.write(fileBytes);

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
