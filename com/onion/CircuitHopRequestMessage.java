package com.onion;

import java.security.*;
import java.io.*;
import java.util.*;

public class CircuitHopRequestMessage implements Serializable {

    public CircuitHopRequestMessage(byte[] keyData, byte[] payloadData) {
	this.keyData = keyData;
        this.payloadData = payloadData;
    }

    private byte[] keyData;
    private byte[] payloadData;

    public byte[] getKeyData() {
        return keyData;
    }

    public byte[] getPayloadData() {
        return payloadData;
    }

    public static class Payload implements Serializable {
        public Payload(int nextNode, List<Key> wrapKeys) {
            this.nextNode = nextNode;
            this.wrapKeys = wrapKeys;
        }

        private int nextNode;
        private List<Key> wrapKeys;

        public int getNextNode() {
            return nextNode;
        }

        public List<Key> getWrapKeys() {
            return wrapKeys;
        }
    }
}
