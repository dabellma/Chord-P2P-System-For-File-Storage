package csx55.chord.transport;

import csx55.chord.Discovery;
import csx55.chord.Node;
import csx55.chord.wireformats.Event;
import csx55.chord.wireformats.EventFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;

public class TCPReceiverThread implements Runnable {

    private Socket socket;
    private DataInputStream dataInputStream;
    private Node node;
    private TCPChannel tcpChannel;

    public TCPReceiverThread(Socket socket, Node node, TCPChannel tcpChannel) throws IOException {
        this.socket = socket;
        this.node = node;
        this.dataInputStream = new DataInputStream(socket.getInputStream());
        this.tcpChannel = tcpChannel;

    }

    public DataInputStream getDataInputStream() {
        return this.dataInputStream;
    }

    @Override
    public void run() {
        while (socket != null) {
            try {

                //for this project, the protocol is to receive the length first
                int msgLength = dataInputStream.readInt();
                byte[] incomingMessage = new byte[msgLength];
                dataInputStream.readFully(incomingMessage, 0, msgLength);

                EventFactory eventFactory = EventFactory.getInstance();

                Event event = eventFactory.createEvent(incomingMessage);
                node.onEvent(event, tcpChannel);

            } catch (IOException | InterruptedException exception) {

                if (node instanceof Discovery) {
                    Discovery discoveryInstance = (Discovery) node;
                    String peerToRemove = null;
                    for (Map.Entry<String, TCPChannel> peer : discoveryInstance.getTCPChannels().entrySet()) {
                        if (peer.getValue().equals(tcpChannel)) {
                            peerToRemove = peer.getKey();
                        }
                    }

                    if (peerToRemove != null) {
                        System.out.println("Removing " + peerToRemove);
                        discoveryInstance.getTCPChannels().remove(peerToRemove);
                    }
                }
                break;
            }
        }
    }
}
