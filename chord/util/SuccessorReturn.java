package csx55.chord.util;

public class SuccessorReturn {

    private SuccessorReturnEnum successorReturnEnum;
    private String returnId;

    public SuccessorReturn(SuccessorReturnEnum successorReturnEnum, String returnId) {
        this.successorReturnEnum = successorReturnEnum;
        this.returnId = returnId;
    }

    public String getReturnId() {
        return this.returnId;
    }

    public SuccessorReturnEnum getSuccessorReturnEnum() {
        return this.successorReturnEnum;
    }

}
