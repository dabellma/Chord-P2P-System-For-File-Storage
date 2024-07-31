package csx55.chord;


import csx55.chord.transport.TCPChannel;
import csx55.chord.transport.TCPServerThread;
import csx55.chord.wireformats.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Discovery implements Node {

    private Map<String, TCPChannel> tcpChannels = new ConcurrentHashMap<>();

    public Map<String, TCPChannel> getTCPChannels() {
        return this.tcpChannels;
    }

    public static void main(String[] args) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        if (args.length == 1) {
            try {
                int portNumber = Integer.parseInt(args[0]);

                Discovery discovery = new Discovery();

                //Create the socket to accept incoming messages from messenger nodes
                ServerSocket ourServerSocket = new ServerSocket(Integer.parseInt(args[0]));

                //Spawn a thread to receive connections to the discovery node
                //while at the same time being able to run as a foreground process
                Thread discoveryTCPServerThread = new Thread(new TCPServerThread(ourServerSocket, discovery));
                discoveryTCPServerThread.start();

                while (true) {
                    String input;
                    input = reader.readLine();

                    discovery.processInput(input);
                }

            } catch (IOException | InterruptedException exception) {
                System.out.println("Encountered an issue while setting up main discovery");
                System.exit(1);
            }

        } else {
            System.out.println("Please enter exactly one argument. Exiting.");
            System.exit(1);
        }
    }

    private void processInput(String input) throws IOException, InterruptedException {
        String[] tokens = input.split("\\s+");
        switch (tokens[0].toUpperCase()) {

            case "PEER-NODES":
                Map<Integer, String> hashCodeToPeer = new HashMap<>();

                for (String peer : tcpChannels.keySet()) {
                    hashCodeToPeer.put(peer.hashCode(), peer);
                }

                List<Integer> orderedPeers = new ArrayList<>();
                for (String peer : tcpChannels.keySet()) {
                    orderedPeers.add(peer.hashCode());
                }

                Collections.sort(orderedPeers);
                for (int peer : orderedPeers) {
                    System.out.println(peer + " " + hashCodeToPeer.get(peer));
                }

                break;

            default:
                System.out.println("Unknown command. Re-entering wait period.");
                break;
        }
    }

    @Override
    public void onEvent(Event event, TCPChannel tcpChannel) throws IOException, InterruptedException {
        if (event instanceof ExitRequest) {
            ExitRequest exitRequest = (ExitRequest) event;

            if (!exitRequest.getIpAddress().equals(tcpChannel.getSocket().getInetAddress().getHostAddress())) {
                System.out.println("Did not pass checks to deregister this peer because there's a mismatch between the deregister address and the address of the request");

                //send a response that the peer can not safely exit and terminate the process
                ExitResponse exitResponse = new ExitResponse((byte) 0);
                tcpChannel.getTcpSenderThread().sendData(exitResponse.getbytes());
            } else if (!tcpChannels.containsKey(new Peer(exitRequest.getIpAddress(), exitRequest.getPortNumber()).toString())) {
                System.out.println("Did not pass checks to deregister this peer because the deregister address doesn't exist in the discovery");

                //send a response that the peer can not safely exit and terminate the process
                ExitResponse exitResponse = new ExitResponse((byte) 0);
                tcpChannel.getTcpSenderThread().sendData(exitResponse.getbytes());
            } else {
                //if there's a successful deregistration
                String peerToRemove = new Peer(exitRequest.getIpAddress(), exitRequest.getPortNumber()).toString();

                tcpChannels.remove(peerToRemove);

                //send a response that the peer can safely exit and terminate the process
                ExitResponse exitResponse = new ExitResponse((byte) 1);
                tcpChannel.getTcpSenderThread().sendData(exitResponse.getbytes());

                Thread.sleep(2000);

                tcpChannel.getTcpReceiverThread().getDataInputStream().close();
                tcpChannel.getTcpSenderThread().getDataOutputStream().close();
                tcpChannel.getSocket().close();
            }

        } else if (event instanceof RegisterRequest) {
            RegisterRequest registerRequest = (RegisterRequest) event;

            //if there's an unsuccessful registration
            if (!registerRequest.getIpAddress().equals(tcpChannel.getSocket().getInetAddress().getHostAddress())) {

                System.out.println("Did not pass checks to register this peer because there's a mismatch between the registration address and the address of the request" );

                //send a response from discovery to peer
                //saying the peer did not register successfully
                DiscoveryRegisterResponse discoveryRegisterResponse = new DiscoveryRegisterResponse(null, 0, false);
                tcpChannel.getTcpSenderThread().sendData(discoveryRegisterResponse.getbytes());

            } else if (tcpChannels.containsKey(new Peer(registerRequest.getIpAddress(), registerRequest.getPortNumber()).toString())) {
                System.out.println("Did not pass checks to register this peer because this node already exists in the discovery" );

                //send a response from discovery to peer
                //saying the peer did not register successfully
                DiscoveryRegisterResponse discoveryRegisterResponse = new DiscoveryRegisterResponse(null, 0, false);
                tcpChannel.getTcpSenderThread().sendData(discoveryRegisterResponse.getbytes());
            } else if (new Peer(registerRequest.getIpAddress(), registerRequest.getPortNumber()).toString().hashCode() != registerRequest.getPeerID()) {
                System.out.println("Did not pass checks to register this peer because this node already exists in the discovery" );

                //send a response from discovery to peer
                //saying the peer did not register successfully
                DiscoveryRegisterResponse discoveryRegisterResponse = new DiscoveryRegisterResponse(null, 0, false);
                tcpChannel.getTcpSenderThread().sendData(discoveryRegisterResponse.getbytes());
            } else {
                //if there's a successful registration
                String thisPeer = new Peer(registerRequest.getIpAddress(), registerRequest.getPortNumber()).toString();
                tcpChannels.put(thisPeer, tcpChannel);

                //send a response from discovery to peer saying the peer registered successfully
                //if this isn't the first peer registering, give a random peer back
                if (tcpChannels.size() > 1) {
                    String randomPeerString = getRandomPeerThatIsNotTheCallingPeer(thisPeer);
                    Peer randomPeer = peerDestring(randomPeerString);

                    DiscoveryRegisterResponse discoveryRegisterResponse = new DiscoveryRegisterResponse(randomPeer.getIpAddress(), randomPeer.getPortNumber(), true);
                    tcpChannel.getTcpSenderThread().sendData(discoveryRegisterResponse.getbytes());

                //if this is the first peer registering, give a success response only
                } else {
                    DiscoveryRegisterResponseOnePeer discoveryRegisterResponse = new DiscoveryRegisterResponseOnePeer();
                    tcpChannel.getTcpSenderThread().sendData(discoveryRegisterResponse.getbytes());
                }
            }

        }  else {
            System.out.println("Received unknown event");
        }
    }

    private String getRandomPeerThatIsNotTheCallingPeer(String callingPeer) {

        List<String> peers = new ArrayList<>(tcpChannels.keySet());
        peers.remove(callingPeer);
        Random random = new Random();
        int index = random.nextInt(peers.size());
        return peers.get(index);

    }

    private Peer peerDestring(String firstValue) {
        String[] ipAddressAndPort = firstValue.split(":");
        String ipAddress = ipAddressAndPort[0];
        int portNumber = Integer.parseInt(ipAddressAndPort[1]);
        return new Peer(ipAddress, portNumber);
    }

    private static String prettyPrintIdentifier(String dataOrNode) {
        return String.format("%,d", dataOrNode.hashCode());
    }

    private static String prettyPrintIdentifierInteger(int dataOrNode) {
        return String.format("%,d", dataOrNode);
    }

    private Long peerToUnsignedIdentifier(String peer) {
        return Integer.toUnsignedLong((peer).hashCode());
    }
}
