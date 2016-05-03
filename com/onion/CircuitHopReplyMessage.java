package com.onion;

import java.io.*;

public class CircuitHopReplyMessage implements Serializable {
    public CircuitHopReplyMessage(int hopConnectionId) {
	this.hopConnectionId = hopConnectionId;
    }

    private int hopConnectionId;

    public int getHopConnectionId() {
	return hopConnectionId;
	    
    }
    
}

