package csx55.chord.wireformats;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class LookupForward implements Event {

    private int messageType;
    private String ipAddress;
    private int portNumber;
    private int finger;

    public LookupForward(String ipAddress, int portNumber, int finger) {
        this.messageType = Protocol.LOOKUP_FORWARD.getValue();
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.finger = finger;
    }

    public LookupForward(byte[] incomingByteArray) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingByteArray);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);

        int messageType = dataInputStream.readInt();


        int ipAddressLength = dataInputStream.readInt();
        byte[] ipAddressBytes = new byte[ipAddressLength];
        dataInputStream.readFully(ipAddressBytes);
        String ipAddress = new String(ipAddressBytes, StandardCharsets.UTF_8);

        int portNumber = dataInputStream.readInt();
        int finger = dataInputStream.readInt();

        dataInputStream.close();
        baInputStream.close();

        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.finger = finger;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }
    public int getPortNumber() {
        return this.portNumber;
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

        dataOutputStream.writeInt(Protocol.LOOKUP_FORWARD.getValue());


        byte[] ipAddressBytes = ipAddress.getBytes(StandardCharsets.UTF_8);
        int byteStringLength = ipAddressBytes.length;
        dataOutputStream.writeInt(byteStringLength);
        dataOutputStream.write(ipAddressBytes);

        dataOutputStream.writeInt(portNumber);
        dataOutputStream.writeInt(finger);

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
