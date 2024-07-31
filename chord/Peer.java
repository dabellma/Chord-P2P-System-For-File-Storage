package csx55.chord;


import csx55.chord.transport.TCPChannel;
import csx55.chord.transport.TCPServerThread;
import csx55.chord.util.SuccessorReturn;
import csx55.chord.util.SuccessorReturnEnum;
import csx55.chord.wireformats.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Peer implements Node {

    private TCPChannel registryTcpChannel;

    private Map<String, TCPChannel> tcpChannels = new ConcurrentHashMap<>();
    private List<String> fingerTable = new ArrayList<>(Collections.nCopies(32, null));
    private List<String> files = new ArrayList<>();

    private final String ipAddress;
    private final int portNumber;
    private String discoveryIPAddress;
    private int discoveryPortNumber;
    private String predecessor;

    //used for help when synchronous communication is desired
    private AtomicBoolean allFilesTransferred = new AtomicBoolean(false);
    private AtomicBoolean predecessorUpdated = new AtomicBoolean(false);


    public Peer(String ipAddress, int portNumber) {
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
    }

    public Peer(String ipAddress, int portNumber, String discoveryIPAddress, int discoveryPortNumber) {
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.discoveryIPAddress = discoveryIPAddress;
        this.discoveryPortNumber = discoveryPortNumber;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public int getPortNumber() {
        return this.portNumber;
    }

    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        if (args.length == 2) {

            try {
                ServerSocket serverSocket = new ServerSocket(0);

                Peer peer = new Peer(InetAddress.getLocalHost().getHostAddress(), serverSocket.getLocalPort(), args[0], Integer.parseInt(args[1]));

                Thread peerServerThread = new Thread(new TCPServerThread(serverSocket, peer));
                peerServerThread.start();

                //let the registry know about this new messaging node
                peer.registerWithDiscovery();

                while (true) {
                    String input;
                    input = reader.readLine();

                    peer.processInput(input);
                }

            } catch (IOException | InterruptedException e) {
                System.out.println("Encountered an issue while setting up Peer: " + e);
                System.exit(1);
            }

        } else {
            System.out.println("Please enter exactly two arguments for the messaging node call. Exiting.");
            System.exit(1);
        }

    }

    private void processInput(String input) throws IOException, InterruptedException {
        String[] tokens = input.split("\\s+");
        switch (tokens[0].toUpperCase()) {

            case "DOWNLOAD":

                if (tokens.length != 2) {
                    System.out.println("Please provide a file to download... exactly 2 arguments: (download) (file).");
                    break;
                }

                String filetoDownload = Paths.get(tokens[1]).getFileName().toString();
                int downloadTargetId = filetoDownload.hashCode();

                //if file is in between this node and predecessor, or is equal to this node, get from here
                if (isInBetweenInteger(downloadTargetId, getPredecessor().hashCode(), this.toString().hashCode())
                        || downloadTargetId == this.toString().hashCode()) {

                    boolean fileFound = false;
                    for (String file : this.files) {
                        if (file.equals(tokens[1])) {
                            fileFound = true;

                            String filePath = "/tmp/" + this.toString().hashCode() + "/" + file;
                            byte[] allBytes = Files.readAllBytes(Paths.get(filePath));
                            Files.write(Paths.get(file), allBytes);

                        }
                    }


                    if (!fileFound) {
                        System.out.println("Error: file not found");
                    } else {
                        //if from here, print out just this node as the pathing that was required to download (just this node)
                        System.out.println(this.toString().hashCode());
                    }

                } else {

                    SuccessorReturn downloadSuccessorReturn = findSuccessorInteger(downloadTargetId);
                    if (downloadSuccessorReturn.getSuccessorReturnEnum().equals(SuccessorReturnEnum.DONE)) {


                        //else forward a request to download the file to this machine, to the final destination
                        Peer peerWithFile = peerDestring(downloadSuccessorReturn.getReturnId());
                        TCPChannel peerWithFileChannel = getOrCreateConnection(peerWithFile.getIpAddress(), peerWithFile.getPortNumber());

                        //add this peer to the path taken
                        DownloadRequest downloadRequest = new DownloadRequest(this.getIpAddress(), this.getPortNumber(), filetoDownload, 1, List.of(this.toString()));
                        peerWithFileChannel.getTcpSenderThread().sendData(downloadRequest.getbytes());

                    } else {

                        //forward the file lookup, eventually coming back to this machine that requested the download
                        Peer closestPrecedingPeer = peerDestring(downloadSuccessorReturn.getReturnId());
                        TCPChannel closestPrecedingPeerChannel = getOrCreateConnection(closestPrecedingPeer.getIpAddress(), closestPrecedingPeer.getPortNumber());

                        //add this peer to the path taken
                        DownloadForward downloadForward = new DownloadForward(this.getIpAddress(), this.getPortNumber(), filetoDownload, 1, List.of(this.toString()));
                        closestPrecedingPeerChannel.getTcpSenderThread().sendData(downloadForward.getbytes());
                    }
                }

                break;

            case "EXIT":

                //need to transfer files and notify certain known nodes upon exit
                Peer successor = peerDestring(getFingerTableEntry(0));
                Peer predecessor = peerDestring(getPredecessor());

                TCPChannel successorConnection = getOrCreateConnection(successor.getIpAddress(), successor.getPortNumber());
                TCPChannel predecessorConnection = getOrCreateConnection(predecessor.getIpAddress(), predecessor.getPortNumber());

                for (String file : this.files) {
                    String filePath = "/tmp/" + this.toString().hashCode() + "/" + file;
                    byte[] allBytes = Files.readAllBytes(Paths.get(filePath));

                    UploadPlacement uploadPlacement = new UploadPlacement(file, allBytes.length, allBytes);
                    successorConnection.getTcpSenderThread().sendData(uploadPlacement.getbytes());

                    Path pathToFile = Paths.get(filePath);
                    Files.delete(pathToFile);
                }

                SuccessorUpdate successorUpdate = new SuccessorUpdate(getFingerTableEntry(0));
                PredecessorExitUpdate predecessorExitUpdate = new PredecessorExitUpdate(getPredecessor(), this.toString());

                predecessorConnection.getTcpSenderThread().sendData(successorUpdate.getbytes());
                successorConnection.getTcpSenderThread().sendData(predecessorExitUpdate.getbytes());

                while (!predecessorUpdated.get()) {
                    Thread.sleep(20);
                }

                Set<String> otherKnownNodes = new HashSet<>();
                otherKnownNodes.addAll(fingerTable);
                otherKnownNodes.add(getPredecessor());
                otherKnownNodes.remove(this.toString());
                for (String otherKnownPeerString : otherKnownNodes) {
                    Peer otherKnownPeer = peerDestring(otherKnownPeerString);
                    TCPChannel otherKnownPeerConnection = getOrCreateConnection(otherKnownPeer.getIpAddress(), otherKnownPeer.getPortNumber());
                    otherKnownPeerConnection.getTcpSenderThread().sendData(new Exit(getFingerTableEntry(0), this.toString(), 5).getbytes());
                }

                //and notify the discovery node
                ExitRequest exitRequest = new ExitRequest(this.getIpAddress(), this.getPortNumber());
                registryTcpChannel.getTcpSenderThread().sendData(exitRequest.getbytes());
                break;

            case "FILES":
                for (String file : this.files) {
                    System.out.println(file + " " + file.hashCode());

                }

                break;

            case "FINGER-TABLE":

                for (int i = 0; i < 32; i++) {
                    int powerOfTwo = 1 << i;
                    int fingerEntry = this.toString().hashCode() + powerOfTwo;
                    System.out.println(i + " " + getFingerTableEntry(i).hashCode());
                }
                break;

            //not needed, but can be helpful with debugging
            case "LIST-CONNECTIONS":
                for (String connection : tcpChannels.keySet()) {
                    System.out.println(connection);
                }
                break;

            case "NEIGHBORS":

                System.out.println("predecessor: " + getPredecessor().hashCode() + " " + getPredecessor());
                System.out.println("successor: " + getFingerTableEntry(0).hashCode() + " " + getFingerTableEntry(0));

                break;

            case "UPLOAD":
                if (tokens.length != 2) {
                    System.out.println("Please provide a file when uploading... exactly 2 arguments: (upload) (file).");
                    break;
                }

                byte[] fileBytes = Files.readAllBytes(Paths.get(tokens[1]));
                String fileName = Paths.get(tokens[1]).getFileName().toString();

                int fileNameTargetId = fileName.hashCode();

                //if file is in between this node and predecessor, or is equal to this node, place here
                if (isInBetweenInteger(fileNameTargetId, getPredecessor().hashCode(), this.toString().hashCode())
                || fileNameTargetId == this.toString().hashCode()) {
                    addFile(fileName);
                    uploadFileToTempFolder(fileName, fileBytes);

                } else {

                    //else, go to successor or forward
                    SuccessorReturn successorReturn = findSuccessorInteger(fileNameTargetId);
                    if (successorReturn.getSuccessorReturnEnum().equals(SuccessorReturnEnum.DONE)) {

                        UploadPlacement uploadPlacement = new UploadPlacement(fileName, fileBytes.length, fileBytes);
                        Peer storageSuccessor = peerDestring(successorReturn.getReturnId());
                        TCPChannel storageSuccessorChannel = getOrCreateConnection(storageSuccessor.getIpAddress(), storageSuccessor.getPortNumber());

                        storageSuccessorChannel.getTcpSenderThread().sendData(uploadPlacement.getbytes());

                    } else {
                        //forward the file lookup
                        Peer closestPrecedingPeer = peerDestring(successorReturn.getReturnId());
                        TCPChannel closestPrecedingPeerChannel = getOrCreateConnection(closestPrecedingPeer.getIpAddress(), closestPrecedingPeer.getPortNumber());

                        UploadForward uploadForward = new UploadForward(fileName, fileBytes.length, fileBytes);
                        closestPrecedingPeerChannel.getTcpSenderThread().sendData(uploadForward.getbytes());
                    }
                }

                break;

            default:
                System.out.println("Unknown command. Re-entering wait period.");
                break;
        }

    }

    @Override
    public void onEvent(Event event, TCPChannel tcpChannel) throws IOException, InterruptedException {
        if (event instanceof DiscoveryRegisterResponse) {

            DiscoveryRegisterResponse discoveryRegisterResponse = (DiscoveryRegisterResponse) event;
            if (!discoveryRegisterResponse.getSuccess()) {
                System.out.println("Failed to register with discovery, see discovery logging for more information.");
                System.exit(1);
            }

            TCPChannel randomNodeChannel = getOrCreateConnection(discoveryRegisterResponse.getRandomNodeIPAddress(), discoveryRegisterResponse.getRandomNodePort());

            RegisterRequest registerRequest = new RegisterRequest(this.toString().hashCode(), this.getIpAddress(), this.getPortNumber());
            randomNodeChannel.getTcpSenderThread().sendData(registerRequest.getbytes());

            while (getFingerTableEntry(0) == null) {
                Thread.sleep(100);
            }


            PredecessorRequest predecessorRequest = new PredecessorRequest(this.toString());

            Peer successor = peerDestring(getFingerTableEntry(0));
            TCPChannel successorNodeChannel = getOrCreateConnection(successor.getIpAddress(), successor.getPortNumber());
            successorNodeChannel.getTcpSenderThread().sendData(predecessorRequest.getbytes());

            while (getPredecessor() == null) {
                Thread.sleep(100);
            }


            FileTransferRequest fileTransferRequest = new FileTransferRequest(this.getIpAddress(), this.getPortNumber());
            successorNodeChannel.getTcpSenderThread().sendData(fileTransferRequest.getbytes());
            while (!allFilesTransferred.get()) {
                Thread.sleep(100);
            }


            //then query for most other finger table entries and wait for all to be full
            for (int i = 1; i < 32; i++) {

                int powerOfTwo = 1 << i;
                int fingerEntry = this.toString().hashCode() + powerOfTwo;

                if (isInBetweenInteger(fingerEntry, this.toString().hashCode(), getFingerTableEntry(0).hashCode())) {
                    updateFingerTable(i, getFingerTableEntry(0));
                } else {

                    //forward query to successor
                    LookupForward lookupForward = new LookupForward(this.getIpAddress(), this.getPortNumber(), i);
                    successorNodeChannel.getTcpSenderThread().sendData(lookupForward.getbytes());
                }

                while (getFingerTableEntry(i) == null) {
                    Thread.sleep(20);
                }
            }

            Set<String> otherKnownNodes = new HashSet<>();
            otherKnownNodes.addAll(fingerTable);
            otherKnownNodes.add(getPredecessor());
            otherKnownNodes.remove(this.toString());
            for (String otherKnownPeerString : otherKnownNodes) {
                Peer otherKnownPeer = peerDestring(otherKnownPeerString);
                TCPChannel otherKnownPeerConnection = getOrCreateConnection(otherKnownPeer.getIpAddress(), otherKnownPeer.getPortNumber());
                otherKnownPeerConnection.getTcpSenderThread().sendData(new Join(this.toString(), 5).getbytes());
            }

            //now do a final check on this node to ensure any fingers that it itself should be holding are corrected
            checkForFingerTableUpdatesAndUpdateIfNeeded(this.toString());

        } else if (event instanceof DiscoveryRegisterResponseOnePeer) {
            for (int i = 0; i < 32; i++) {
                updateFingerTable(i, new Peer(this.getIpAddress(), this.getPortNumber()).toString());
            }

            setPredecessor(this.toString());

        } else if (event instanceof DownloadForward) {

            DownloadForward downloadForward = (DownloadForward) event;


            int downloadTargetId = downloadForward.getFile().hashCode();
            SuccessorReturn downloadSuccessorReturn = findSuccessorInteger(downloadTargetId);

            if (downloadSuccessorReturn.getSuccessorReturnEnum().equals(SuccessorReturnEnum.DONE)) {

                    //else forward a request to download the file to this machine, to the final destination
                    Peer peerWithFile = peerDestring(downloadSuccessorReturn.getReturnId());
                    TCPChannel peerWithFileChannel = getOrCreateConnection(peerWithFile.getIpAddress(), peerWithFile.getPortNumber());

                    //add this peer to the path taken
                    List<String> pathTaken = downloadForward.getPathTaken();
                    pathTaken.add(this.toString());
                    DownloadRequest downloadRequest = new DownloadRequest(downloadForward.getIpAddress(), downloadForward.getPortNumber(), downloadForward.getFile(), pathTaken.size(), pathTaken);
                    peerWithFileChannel.getTcpSenderThread().sendData(downloadRequest.getbytes());

            } else {

                //forward the file lookup, eventually coming back to this machine that requested the download
                Peer closestPrecedingPeer = peerDestring(downloadSuccessorReturn.getReturnId());
                TCPChannel closestPrecedingPeerChannel = getOrCreateConnection(closestPrecedingPeer.getIpAddress(), closestPrecedingPeer.getPortNumber());

                //add this peer to the path taken
                List<String> pathTaken = downloadForward.getPathTaken();
                pathTaken.add(this.toString());
                downloadForward.setLengthPathTaken(pathTaken.size());
                downloadForward.setPathTaken(pathTaken);
                closestPrecedingPeerChannel.getTcpSenderThread().sendData(downloadForward.getbytes());
            }


        } else if (event instanceof DownloadRequest) {

            DownloadRequest downloadRequest = (DownloadRequest) event;
            TCPChannel originalRequestingNode = getOrCreateConnection(downloadRequest.getIpAddress(), downloadRequest.getPortNumber());

            boolean fileFound = false;

            for (String file : this.files) {
                if (file.equals(downloadRequest.getFile())) {
                    fileFound = true;
                }
            }

            if (fileFound) {
                String filePath = "/tmp/" + this.toString().hashCode() + "/" + downloadRequest.getFile();
                byte[] fileAsBytes = Files.readAllBytes(Paths.get(filePath));


                List<String> finalPathTaken = downloadRequest.getPathTaken();
                finalPathTaken.add(this.toString());

                DownloadResponse downloadResponse = new DownloadResponse(downloadRequest.getFile(), fileAsBytes.length, fileAsBytes, finalPathTaken.size(), finalPathTaken);
                originalRequestingNode.getTcpSenderThread().sendData(downloadResponse.getbytes());

            } else {
                FileNotFound fileNotFound = new FileNotFound();
                originalRequestingNode.getTcpSenderThread().sendData(fileNotFound.getbytes());
            }


        } else if (event instanceof DownloadResponse) {

            DownloadResponse downloadResponse = (DownloadResponse) event;

            Files.write(Paths.get(downloadResponse.getFileName()), downloadResponse.getFile());


            for (String peer : downloadResponse.getPathTaken()) {
                System.out.println(peer.hashCode());
            }


        } else if (event instanceof Exit) {
            Exit exit = (Exit) event;

            //check if finger table updates are needed from an exiting node
            if (exit.getNumNotificationForwardsLeft() > 0
                    && !exit.getExitingPeer().equals(this.toString())) {

                int numForwardsLeft= exit.getNumNotificationForwardsLeft();
                numForwardsLeft -= 1;

                Set<String> otherKnownNodes = new HashSet<>();
                otherKnownNodes.addAll(fingerTable);
                otherKnownNodes.add(getPredecessor());
                otherKnownNodes.remove(this.toString());
                for (String otherKnownPeerString : otherKnownNodes) {
                    Peer otherKnownPeer = peerDestring(otherKnownPeerString);
                    TCPChannel otherKnownPeerConnection = getOrCreateConnection(otherKnownPeer.getIpAddress(), otherKnownPeer.getPortNumber());
                    otherKnownPeerConnection.getTcpSenderThread().sendData(new Exit(exit.getExitingPeersSuccessor(), exit.getExitingPeer(), numForwardsLeft).getbytes());
                }

                checkForFingerTableUpdatesOnExit(exit.getExitingPeer(), exit.getExitingPeersSuccessor());

            }


        } else if (event instanceof ExitResponse) {

            ExitResponse exitResponse = (ExitResponse) event;
            if (exitResponse.getSuccessOrFailure() == 1) {

                Thread.sleep(1000);

                registryTcpChannel.getTcpReceiverThread().getDataInputStream().close();
                registryTcpChannel.getTcpSenderThread().getDataOutputStream().close();
                registryTcpChannel.getSocket().close();

                System.exit(1);
            } else {
                System.out.println("Did not receive a successful deregister response from the registry.");
            }

        } else if (event instanceof FileNotFound) {

            FileNotFound fileNotFound = (FileNotFound) event;
            System.out.println("Error: file not found");

        } else if (event instanceof FileTransferRequest) {

            FileTransferRequest fileTransferRequest = (FileTransferRequest) event;
            Peer joiningPeer = new Peer(fileTransferRequest.getIpAddress(), fileTransferRequest.getPortNumber());
            int joiningPeerId = joiningPeer.toString().hashCode();

            TCPChannel joiningPeerChannel = getOrCreateConnection(joiningPeer.getIpAddress(), joiningPeer.getPortNumber());

            List<String> filesToRemove = new ArrayList<>();
            for (String file : this.files) {
                if (!isInBetweenInteger(file.hashCode(), joiningPeerId, this.toString().hashCode())) {
                    String filePath = "/tmp/" + this.toString().hashCode() + "/" + file;
                    byte[] allBytes = Files.readAllBytes(Paths.get(filePath));

                    UploadPlacement uploadPlacement = new UploadPlacement(file, allBytes.length, allBytes);
                    joiningPeerChannel.getTcpSenderThread().sendData(uploadPlacement.getbytes());

                    filesToRemove.add(file);
                }
            }

            for (String fileToRemove : filesToRemove) {
                removeFile(fileToRemove);

                String fileToRemovePath = "/tmp/" + this.toString().hashCode() + "/" + fileToRemove;
                Path pathToFile = Paths.get(fileToRemovePath);
                Files.delete(pathToFile);
            }

            FileTransferResponse fileTransferResponse = new FileTransferResponse();
            joiningPeerChannel.getTcpSenderThread().sendData(fileTransferResponse.getbytes());

        } else if (event instanceof FileTransferResponse) {

            FileTransferResponse fileTransferResponse = (FileTransferResponse) event;
            allFilesTransferred.set(true);

        } else if (event instanceof Join) {
            Join join = (Join) event;

            //check if finger table updates are needed from a joining node
            if (join.getNumNotificationForwardsLeft() > 0
                && !join.getNewPeer().equals(this.toString())) {

                int numForwardsLeft = join.getNumNotificationForwardsLeft();
                numForwardsLeft -= 1;

                Set<String> otherKnownNodes = new HashSet<>();
                otherKnownNodes.addAll(fingerTable);
                otherKnownNodes.add(getPredecessor());
                otherKnownNodes.remove(this.toString());
                for (String otherKnownPeerString : otherKnownNodes) {
                    Peer otherKnownPeer = peerDestring(otherKnownPeerString);
                    TCPChannel otherKnownPeerConnection = getOrCreateConnection(otherKnownPeer.getIpAddress(), otherKnownPeer.getPortNumber());
                    otherKnownPeerConnection.getTcpSenderThread().sendData(new Join(join.getNewPeer(), numForwardsLeft).getbytes());
                }

                checkForFingerTableUpdatesAndUpdateIfNeeded(join.getNewPeer());
            }

        } else if (event instanceof LookupForward) {

            LookupForward lookupForward = (LookupForward) event;

            Peer initialPeer = new Peer(lookupForward.getIpAddress(), lookupForward.getPortNumber());
            int powerOfTwo = 1 << lookupForward.getFinger();
            int fingerEntry = initialPeer.toString().hashCode() + powerOfTwo;

            SuccessorReturn successorReturn = findSuccessorInteger(fingerEntry);

            if (successorReturn.getSuccessorReturnEnum().equals(SuccessorReturnEnum.DONE)) {
                String successor = successorReturn.getReturnId();

                UpdateFingerTable updateFingerTable = new UpdateFingerTable(successor, lookupForward.getFinger());
                TCPChannel requestorChannel = getOrCreateConnection(lookupForward.getIpAddress(), lookupForward.getPortNumber());
                requestorChannel.getTcpSenderThread().sendData(updateFingerTable.getbytes());

            } else {
                //forward it
                Peer closestPreceedingPeer = peerDestring(successorReturn.getReturnId());

                TCPChannel closestPrecedingPeerChannel = getOrCreateConnection(closestPreceedingPeer.getIpAddress(), closestPreceedingPeer.getPortNumber());
                closestPrecedingPeerChannel.getTcpSenderThread().sendData(lookupForward.getbytes());
            }


        } else if (event instanceof PredecessorRequest) {

            PredecessorRequest predecessorRequest = (PredecessorRequest) event;
            PredecessorUpdate predecessorUpdate = new PredecessorUpdate(getPredecessor());
            Peer newNode = peerDestring(predecessorRequest.getNewNode());
            TCPChannel newNodesConnection = getOrCreateConnection(newNode.getIpAddress(), newNode.getPortNumber());
            newNodesConnection.getTcpSenderThread().sendData(predecessorUpdate.getbytes());

            setPredecessor(predecessorRequest.getNewNode());

        } else if (event instanceof PredecessorUpdate) {

            PredecessorUpdate predecessorUpdate = (PredecessorUpdate) event;
            setPredecessor(predecessorUpdate.getPredecessor());

        } else if (event instanceof PredecessorExitUpdate) {

            PredecessorExitUpdate predecessorExitUpdate = (PredecessorExitUpdate) event;
            setPredecessor(predecessorExitUpdate.getPredecessor());

            Peer leavingNode = peerDestring(predecessorExitUpdate.getLeavingNode());
            TCPChannel leavingNodeChannel = getOrCreateConnection(leavingNode.getIpAddress(), leavingNode.getPortNumber());
            leavingNodeChannel.getTcpSenderThread().sendData(new PredecessorExitUpdateResponse().getbytes());

        } else if (event instanceof PredecessorExitUpdateResponse) {

            PredecessorExitUpdateResponse predecessorExitUpdateResponse = (PredecessorExitUpdateResponse) event;
            predecessorUpdated.set(true);

        } else if (event instanceof RegisterRequest) {
            RegisterRequest registerRequest = (RegisterRequest) event;
            String newPeer = new Peer(registerRequest.getIpAddress(), registerRequest.getPortNumber()).toString();

            //populate successor in new node's finger table
            int successorFingerEntry = newPeer.hashCode() + 1;
            SuccessorReturn successorReturn = findSuccessorInteger(successorFingerEntry);

            if (successorReturn.getSuccessorReturnEnum().equals(SuccessorReturnEnum.DONE)) {
                String successor = successorReturn.getReturnId();

                UpdateFingerTable updateFingerTable = new UpdateFingerTable(successor, 0);

                TCPChannel registeringPeersConnection = getOrCreateConnection(registerRequest.getIpAddress(), registerRequest.getPortNumber());
                registeringPeersConnection.getTcpSenderThread().sendData(updateFingerTable.getbytes());

            } else {

                Peer peerPredecessor = peerDestring(successorReturn.getReturnId());

                TCPChannel closestPrecedingPeerChannel = getOrCreateConnection(peerPredecessor.getIpAddress(), peerPredecessor.getPortNumber());
                LookupForward lookupForward = new LookupForward(registerRequest.getIpAddress(), registerRequest.getPortNumber(), 0);
                closestPrecedingPeerChannel.getTcpSenderThread().sendData(lookupForward.getbytes());
            }


        } else if (event instanceof SuccessorUpdate) {
            //this is from the new node to it's new predecessor, telling the new predecessor that it's successor is the new node

            SuccessorUpdate successorUpdate = (SuccessorUpdate) event;
            updateFingerTable(0, successorUpdate.getSuccessor());

        } else if (event instanceof UpdateFingerTable) {

            UpdateFingerTable updateFingerTable = (UpdateFingerTable) event;
            updateFingerTable(updateFingerTable.getFinger(), updateFingerTable.getUpdatePeer());


        } else if (event instanceof UploadForward) {

            UploadForward uploadForward = (UploadForward) event;

            int targetFileNameId = uploadForward.getFileName().hashCode();
            SuccessorReturn successorReturn = findSuccessorInteger(targetFileNameId);

            if (successorReturn.getSuccessorReturnEnum().equals(SuccessorReturnEnum.DONE)) {

                UploadPlacement uploadPlacement = new UploadPlacement(uploadForward.getFileName(), uploadForward.getFileLength(), uploadForward.getFile());
                Peer storageSuccessor = peerDestring(successorReturn.getReturnId());
                TCPChannel closestPrecedingPeerChannel = getOrCreateConnection(storageSuccessor.getIpAddress(), storageSuccessor.getPortNumber());

                closestPrecedingPeerChannel.getTcpSenderThread().sendData(uploadPlacement.getbytes());

            } else {
                //forward the file lookup
                Peer closestPrecedingPeer = peerDestring(successorReturn.getReturnId());
                TCPChannel closestPrecedingPeerChannel = getOrCreateConnection(closestPrecedingPeer.getIpAddress(), closestPrecedingPeer.getPortNumber());

                closestPrecedingPeerChannel.getTcpSenderThread().sendData(uploadForward.getbytes());
            }

        } else if (event instanceof UploadPlacement) {

            UploadPlacement uploadPlacement = (UploadPlacement) event;
            addFile(uploadPlacement.getFileName());

            uploadFileToTempFolder(uploadPlacement.getFileName(), uploadPlacement.getFile());

        } else {
            System.out.println("Received unknown event.");
        }
    }


    private void uploadFileToTempFolder(String fileName, byte[] fileBytes) throws IOException {
        String filePath = "/tmp/" + this.toString().hashCode() + "/" + fileName;
        File physicalFile = new File(filePath);
        String directoryPath = physicalFile.getParent();
        File directory = new File(directoryPath);
        if (directory.exists()) {
            FileOutputStream fileOutputStream = new FileOutputStream(physicalFile);
            fileOutputStream.write(fileBytes);
        } else {
            directory.mkdirs();
            FileOutputStream fileOutputStream = new FileOutputStream(physicalFile);
            fileOutputStream.write(fileBytes);
        }
    }

    private TCPChannel getOrCreateConnection(String ipAddress, int port) throws IOException {
        TCPChannel existingTcpChannel = tcpChannels.get(new Peer(ipAddress, port).toString());
        if (existingTcpChannel == null) {
            Socket socketToOtherPeerToConnectWith = new Socket(ipAddress, port);
            TCPChannel tcpChannelOtherPeer = new TCPChannel(socketToOtherPeerToConnectWith, this);

            Thread receivingThread = new Thread(tcpChannelOtherPeer.getTcpReceiverThread());
            receivingThread.start();

            Thread sendingThread = new Thread(tcpChannelOtherPeer.getTcpSenderThread());
            sendingThread.start();

            tcpChannels.put(ipAddress + ":" + port, tcpChannelOtherPeer);

            existingTcpChannel = tcpChannelOtherPeer;
        }
        return existingTcpChannel;
    }

    private synchronized void updateFingerTable(int i, String peer) {
        this.fingerTable.set(i, peer);
        if (i == 0) {
        }
    }

    private synchronized void checkForFingerTableUpdatesAndUpdateIfNeeded(String newNode) {
        for (int i=0; i<32; i++) {
            int powerOfTwo = 1 << i;
            int fingerEntry = this.toString().hashCode() + powerOfTwo;
            int currentFingerValue = getFingerTableEntry(i).hashCode();
            if (isInBetweenInteger(newNode.hashCode(), fingerEntry, currentFingerValue)) {
                updateFingerTable(i, newNode);
            }
        }
    }

    private synchronized void checkForFingerTableUpdatesOnExit(String exitingPeer, String exitingPeersSuccessor) {
        for (int i=0; i<32; i++) {
            if (getFingerTableEntry(i).equals(exitingPeer)) {
                updateFingerTable(i, exitingPeersSuccessor);
            }
        }
    }


    private synchronized void addFile(String fileName) {
        this.files.add(fileName);
    }

    private synchronized void removeFile(String fileName) {
        this.files.remove(fileName);
    }

    private synchronized String getFingerTableEntry(int i) {
        return this.fingerTable.get(i);
    }

    private synchronized String getPredecessor() {
        return this.predecessor;
    }

    private synchronized void setPredecessor(String newPredecessor) {
        this.predecessor = newPredecessor;
    }


    private Peer peerDestring(String peer) {
        String[] ipAddressAndPort = peer.split(":");
        String ipAddress = ipAddressAndPort[0];
        int portNumber = Integer.parseInt(ipAddressAndPort[1]);
        return new Peer(ipAddress, portNumber);
    }

    //main algorithm to find an id's peer node successor
    private SuccessorReturn findSuccessorInteger(int targetInteger) {
        if (targetInteger == this.toString().hashCode()) {
            return new SuccessorReturn(SuccessorReturnEnum.DONE, this.toString());
        } else if (isInBetweenInteger(targetInteger, this.toString().hashCode(), getFingerTableEntry(0).hashCode())
                || targetInteger == getFingerTableEntry(0).hashCode()) {
            return new SuccessorReturn(SuccessorReturnEnum.DONE, getFingerTableEntry(0));
        } else {
            String closestPrecedingPeer = findClosestPrecedingPeerIntegerToTarget(targetInteger);

            if (closestPrecedingPeer.equals(this.toString())) {
                return new SuccessorReturn(SuccessorReturnEnum.FORWARD, getFingerTableEntry(0));
            } else {
                return new SuccessorReturn(SuccessorReturnEnum.FORWARD, closestPrecedingPeer);
            }
        }
    }

    private boolean isInBetweenInteger(int id, int start, int end) {
        if (start < end) {
            return id > start && id < end;
        } else {
            return id > start || id < end;
        }
    }

    private String findClosestPrecedingPeerIntegerToTarget(int targetInteger) {
        for (int i=31; i>=0; i--) {
            String fingerTableEntry = getFingerTableEntry(i);
            if (fingerTableEntry != null && isInBetweenInteger(fingerTableEntry.hashCode(), this.toString().hashCode(), targetInteger)) {
                return fingerTableEntry;
            }
        }
        return this.toString();
    }


    private void registerWithDiscovery() throws IOException, InterruptedException {

        Socket socketToHost = new Socket(this.discoveryIPAddress, this.discoveryPortNumber);
        TCPChannel tcpChannel = new TCPChannel(socketToHost, this);

        Thread receivingThread = new Thread(tcpChannel.getTcpReceiverThread());
        receivingThread.start();

        Thread sendingThread = new Thread(tcpChannel.getTcpSenderThread());
        sendingThread.start();

        registryTcpChannel = tcpChannel;

        RegisterRequest registerRequest = new RegisterRequest(this.toString().hashCode(), this.getIpAddress(), this.getPortNumber());
        registryTcpChannel.getTcpSenderThread().sendData(registerRequest.getbytes());
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        Peer peerObject = (Peer) object;
        return portNumber == peerObject.portNumber && Objects.equals(ipAddress, peerObject.ipAddress);
    }

    @Override
    public String toString() {
        return ipAddress + ":" + portNumber;
    }
}
