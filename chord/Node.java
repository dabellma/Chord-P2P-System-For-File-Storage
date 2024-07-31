package csx55.chord;


import csx55.chord.transport.TCPChannel;
import csx55.chord.wireformats.Event;

import java.io.IOException;

public interface Node {
    void onEvent(Event event, TCPChannel tcpChannel) throws IOException, InterruptedException;
}
