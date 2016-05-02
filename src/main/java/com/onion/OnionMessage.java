package com.onion;

import java.io.*;

public class OnionMessage implements Serializable {
    private int connectionId;
    private MsgType msgType;
    private byte[] data;

    public OnionMessage(int connectionId, byte[] data, MsgType msgType) {
	this.connectionId = connectionId;
	this.data = data;
    this.msgType = msgType;
	    
    }

    public enum MsgType {
        HOP_REQUEST, HOP_REPLY, DATA, POISON, KEY_REQUEST, KEY_REPLY
    }

    public int connectionId;
    byte[] data;

    public int getConnectionId() {
	return connectionId;
	    
    }

    public byte[] getData() {
	return data;
	    
    }

    public MsgType getType() {
    return msgType;
    }

    // Format: length of msg connectionID MsgType DATA
    // MsgType is serialized as the ordinal value
    public byte[] pack() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
        byteStream.write(CipherUtils.serialize(new Integer(byte[].length)));
        byteStream.write(CiptherUtils.serialize(new Integer(this.connectionId)));
        byteStream.write(CipherUtils.serialize(new Integer(this.msgType.ordinal())));
        byteStream.write(data, 0, data.length);
        return baos.toByteArray();
    } catch (Exception e) {
        return null;
    }
   
    }

    public static OnionMessage unpack(Socket sck) {
    try {
        ObjectInputStream ois = new ObjectInputStream(sck.getInputStream());
        Integer length = (Integer) ois.readObject();
        Integer connectionId = (Integer) ois.readObject();
        MsgType type = MsgType.values()[(Integer) ois.readObject()];
        byte[] data = new byte[length];
        ois.read(data, 0, length);
        return new OnionMessage(connectionId, data, msgType);    
    } catch (Exception e) {
        return null;
    }
    
    }
}
