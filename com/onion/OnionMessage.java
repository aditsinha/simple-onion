package com.onion;

import java.io.*;
import java.net.*;

public class OnionMessage implements Serializable {
    public OnionMessage(MsgType type, byte[] data) {
	this.data = data;
	this.type = type;
    }

    public int connectionId;
    byte[] data;

    public int getConnectionId() {
	return connectionId;
	    
    }

    public byte[] getData() {
	return data;
    }

    // Format: length of msg connectionID MsgType DATA
    // MsgType is serialized as the ordinal value
    public byte[] pack() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(CipherUtils.serialize(new Integer(data.length)));
            baos.write(CipherUtils.serialize(new Integer(this.type.ordinal())));
            baos.write(data, 0, data.length);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public static OnionMessage unpack(Socket sck) {
        try {
            ObjectInputStream ois = new ObjectInputStream(sck.getInputStream());
            Integer length = (Integer) ois.readObject();
            MsgType type = OnionMessage.MsgType.values()[(Integer) ois.readObject()];
            byte[] data = new byte[length];
            ois.read(data, 0, length);
            return new OnionMessage(type, data);    
        } catch (Exception e) {
            return null;
        }
    }
}
