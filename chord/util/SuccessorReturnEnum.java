package csx55.chord.util;

public enum SuccessorReturnEnum {
    FORWARD(0),
    DONE(1);

    private final int value;

    SuccessorReturnEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
