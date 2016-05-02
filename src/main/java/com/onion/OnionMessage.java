package com.onion;

import java.io.*;

public class OnionMessage implements Serializable {
    public OnionMessage(int connectionId, byte[] data) {
	this.connectionId = connectionId;
	this.data = data;
    }

    public int connectionId;
    byte[] data;

    public int getConnectionId() {
	return connectionId;
	    
    }

    public byte[] getData() {
	return data;
    }
}
