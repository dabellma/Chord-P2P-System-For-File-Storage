package csx55.chord.wireformats;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class DiscoveryRegisterResponse implements Event {

    private int messageType;
    private String randomNodeIPAddress;
    private int randomNodePort;
    private boolean success;


    public DiscoveryRegisterResponse(String randomNodeIPAddress, int randomNodePort, boolean success) {
        this.messageType = Protocol.DISCOVERY_REGISTER_RESPONSE.getValue();
        this.randomNodeIPAddress = randomNodeIPAddress;
        this.randomNodePort = randomNodePort;
        this.success = success;
    }

    public DiscoveryRegisterResponse(byte[] incomingByteArray) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingByteArray);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);

        int messageType = dataInputStream.readInt();

        int randomNodeIPAddressLength = dataInputStream.readInt();
        byte[] randomNodeIPAddressBytes = new byte[randomNodeIPAddressLength];
        dataInputStream.readFully(randomNodeIPAddressBytes);
        String randomNodeIPAddress = new String(randomNodeIPAddressBytes, StandardCharsets.UTF_8);

        int randomNodePort = dataInputStream.readInt();

        boolean success = dataInputStream.readBoolean();

        dataInputStream.close();
        baInputStream.close();

        this.randomNodeIPAddress = randomNodeIPAddress;
        this.randomNodePort = randomNodePort;
        this.success = success;
    }

    public String getRandomNodeIPAddress() {
        return this.randomNodeIPAddress;
    }

    public int getRandomNodePort() {
        return this.randomNodePort;
    }

    public boolean getSuccess() {
        return this.success;
    }

    @Override
    public byte[] getbytes() throws IOException {
        byte[] marshalledBytes;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dataOutputStream.writeInt(Protocol.DISCOVERY_REGISTER_RESPONSE.getValue());

        byte[] randomNodeIPAddressBytes = randomNodeIPAddress.getBytes(StandardCharsets.UTF_8);
        int randomNodeIPAddressByteStringLength = randomNodeIPAddressBytes.length;
        dataOutputStream.writeInt(randomNodeIPAddressByteStringLength);
        dataOutputStream.write(randomNodeIPAddressBytes);

        dataOutputStream.writeInt(randomNodePort);

        dataOutputStream.writeBoolean(success);

        dataOutputStream.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dataOutputStream.close();
        return marshalledBytes;
    }
}
