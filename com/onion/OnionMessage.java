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
            byte[] thisObject = CipherUtils.serialize(this);
            ByteBuffer bb = ByteBuffer.allocate(thisObject.length + 4).putInt(thisObject.length);
            bb.put(thisObject, 0, thisObject.length);
            return bb.array();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public static OnionMessage unpack(Socket sck) {
        try {
            Common.log("[OnionMessage]: unpack");
            InputStream is = sck.getInputStream();
            byte[] len = new byte[4];
            int read = 0;
            while(read < 4) {
                read += is.read(len, read, 4 - read);
            }

            int length = ByteBuffer.wrap(len).getInt();

            byte[] object = new byte[length];
            read = 0;
            while(read < length) {
                read += is.read(object, read, length-read);
            }

            return (OnionMessage) CipherUtils.deserialize(object);    
        } catch(SocketException e) {
            // killing the connection may throw this.
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }
}
