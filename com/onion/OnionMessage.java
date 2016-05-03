package com.onion;

import java.io.*;

public class OnionMessage implements Serializable {
    public OnionMessage(MsgType type, byte[] data) {
	this.data = data;
	this.type = type;
    }

    public static enum MsgType {
	KEY_REQUEST,
	KEY_REPLY,
	HOP_REQUEST,
	HOP_REPLY,
	POISON,
	DATA,
    }

    MsgType type;
    byte[] data;

    public MsgType getType() {
	return type;
	    
    }

    public byte[] getData() {
	return data;
    }

    public byte[] pack() {
	return null;
    }
}
