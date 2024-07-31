package csx55.chord.wireformats;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class EventFactory {
    private static final EventFactory instance = new EventFactory();

    private EventFactory() {}

    public static EventFactory getInstance() {
        return instance;
    }

    public Event createEvent(byte[] incomingMessage) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(incomingMessage);
        DataInputStream dataInputStream = new DataInputStream(baInputStream);

        //protocol that I always send what type of message it is to then process it
        int messageType = dataInputStream.readInt();

        if (messageType == Protocol.DISCOVERY_REGISTER_RESPONSE.getValue()) {
            DiscoveryRegisterResponse discoveryRegisterResponse = new DiscoveryRegisterResponse(incomingMessage);
            return discoveryRegisterResponse;
        } else if (messageType == Protocol.DISCOVERY_REGISTER_RESPONSE_ONE_PEER.getValue()) {
            DiscoveryRegisterResponseOnePeer discoveryRegisterResponseOnePeer = new DiscoveryRegisterResponseOnePeer(incomingMessage);
            return discoveryRegisterResponseOnePeer;
        }  else if (messageType == Protocol.DOWNLOAD_FORWARD.getValue()) {
            DownloadForward downloadForward = new DownloadForward(incomingMessage);
            return downloadForward;
        } else if (messageType == Protocol.DOWNLOAD_REQUEST.getValue()) {
            DownloadRequest downloadRequest = new DownloadRequest(incomingMessage);
            return downloadRequest;
        } else if (messageType == Protocol.DOWNLOAD_RESPONSE.getValue()) {
            DownloadResponse downloadResponse = new DownloadResponse(incomingMessage);
            return downloadResponse;
        } else if (messageType == Protocol.EXIT.getValue()) {
            Exit exit = new Exit(incomingMessage);
            return exit;
        } else if (messageType == Protocol.EXIT_REQUEST.getValue()) {
            ExitRequest exitRequest = new ExitRequest(incomingMessage);
            return exitRequest;
        } else if (messageType == Protocol.EXIT_RESPONSE.getValue()) {
            ExitResponse exitResponse = new ExitResponse(incomingMessage);
            return exitResponse;
        } else if (messageType == Protocol.FILE_NOT_FOUND.getValue()) {
            FileNotFound fileNotFound = new FileNotFound(incomingMessage);
            return fileNotFound;
        } else if (messageType == Protocol.FILE_TRANSFER_REQUEST.getValue()) {
            FileTransferRequest fileTransferRequest = new FileTransferRequest(incomingMessage);
            return fileTransferRequest;
        } else if (messageType == Protocol.FILE_TRANSFER_RESPONSE.getValue()) {
            FileTransferResponse fileTransferResponse = new FileTransferResponse(incomingMessage);
            return fileTransferResponse;
        } else if (messageType == Protocol.LOOKUP_FORWARD.getValue()) {
            LookupForward lookupForward = new LookupForward(incomingMessage);
            return lookupForward;
        } else if (messageType == Protocol.JOIN.getValue()) {
            Join join = new Join(incomingMessage);
            return join;
        } else if (messageType == Protocol.PREDECESSOR_REQUEST.getValue()) {
            PredecessorRequest predecessorRequest = new PredecessorRequest(incomingMessage);
            return predecessorRequest;
        } else if (messageType == Protocol.PREDECESSOR_UPDATE.getValue()) {
            PredecessorUpdate predecessorUpdate = new PredecessorUpdate(incomingMessage);
            return predecessorUpdate;
        } else if (messageType == Protocol.PREDECESSOR_EXIT_UPDATE.getValue()) {
            PredecessorExitUpdate predecessorExitUpdate = new PredecessorExitUpdate(incomingMessage);
            return predecessorExitUpdate;
        } else if (messageType == Protocol.PREDECESSOR_EXIT_UPDATE_RESPONSE.getValue()) {
            PredecessorExitUpdateResponse predecessorExitUpdateResponse = new PredecessorExitUpdateResponse(incomingMessage);
            return predecessorExitUpdateResponse;
        } else if (messageType == Protocol.REGISTER_REQUEST.getValue()) {
            RegisterRequest registerRequest = new RegisterRequest(incomingMessage);
            return registerRequest;
        } else if (messageType == Protocol.SUCCESSOR_UPDATE.getValue()) {
            SuccessorUpdate successorUpdate = new SuccessorUpdate(incomingMessage);
            return successorUpdate;
        } else if (messageType == Protocol.UPDATE_FINGER_TABLE.getValue()) {
            UpdateFingerTable updateFingerTable = new UpdateFingerTable(incomingMessage);
            return updateFingerTable;
        } else if (messageType == Protocol.UPLOAD_FORWARD.getValue()) {
            UploadForward uploadForward = new UploadForward(incomingMessage);
            return uploadForward;
        } else if (messageType == Protocol.UPLOAD_PLACEMENT.getValue()) {
            UploadPlacement uploadPlacement = new UploadPlacement(incomingMessage);
            return uploadPlacement;
        } else {
            System.out.println("Unrecognized event in event factory");

        }
        return null;
    }

}
