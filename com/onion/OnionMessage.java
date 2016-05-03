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
            byte[] thisObject = CipherUtils.serialize(this);
            bb.put(thisObject, 0, thisObject.length);
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

            byte[] object = new byte[length];
            int read = 0;
            while(read < object.length) {
                is.read(object, read, object.length-read);
            }

            return (OnionMessage) CipherUtils.deserialize(object);    
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
