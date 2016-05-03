package com.onion;

import java.io.*;
import java.nio.*;
import java.net.*;

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

    // Format: length of msg connectionID MsgType DATA
    // MsgType is serialized as the ordinal value
    public byte[] pack() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ByteBuffer bb = ByteBuffer.allocate(4).putInt(data.length);
            baos.write(CipherUtils.serialize(new Integer(this.type.ordinal())));
            baos.write(data, 0, data.length);
            byte[] baosBytes = baos.toByteArray();
            bb.put(baosBytes, 0, baosBytes.length);
            return bb.array();
        } catch (Exception e) {
            return null;
        }
    }

    public static OnionMessage unpack(Socket sck) {
        try {
            InputStream is = sck.getInputStream();
            byte[] len = new byte[4];
            is.read(len, 0, len.length);
            int length = ByteBuffer.wrap(len).getInt();

            ObjectInputStream ois = new ObjectInputStream(sck.getInputStream());
            ois = new ObjectInputStream(sck.getInputStream());
            MsgType type = OnionMessage.MsgType.values()[(Integer) ois.readObject()];
            byte[] data = new byte[length];
            is.read(data, 0, length);
            return new OnionMessage(type, data);    
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
