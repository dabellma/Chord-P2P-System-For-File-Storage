package csx55.chord.wireformats;

public enum Protocol {
    REGISTER_REQUEST(0),
    EXIT_REQUEST(1),
    DISCOVERY_REGISTER_RESPONSE(3),
    DISCOVERY_REGISTER_RESPONSE_ONE_PEER(4),
    EXIT_RESPONSE(5),
    UPDATE_FINGER_TABLE(6),
    LOOKUP_FORWARD(7),
    JOIN(8),
    UPLOAD_FORWARD(9),
    PREDECESSOR_REQUEST(10),
    UPLOAD_PLACEMENT(12),
    DOWNLOAD_REQUEST(13),
    DOWNLOAD_RESPONSE(14),
    DOWNLOAD_FORWARD(15),
    EXIT(16),
    SUCCESSOR_UPDATE(17),
    PREDECESSOR_UPDATE(18),
    PREDECESSOR_EXIT_UPDATE(19),
    PREDECESSOR_EXIT_UPDATE_RESPONSE(20),
    FILE_TRANSFER_REQUEST(21),
    FILE_TRANSFER_RESPONSE(23),
    FILE_NOT_FOUND(24);

    private final int value;

    Protocol(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
