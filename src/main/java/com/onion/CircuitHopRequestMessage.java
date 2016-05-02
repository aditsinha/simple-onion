package com.onion;

import java.security.*;
import java.io.*;
import java.util.*;

public class CircuitHopRequestMessage implements Serializable {
    public CircuitHopRequestMessage(int nextNode, Key secretKey, List<Key> wrapKeys) {
	this.nextNode = nextNode;
	this.secretKey = secretKey;
	this.wrapKeys = wrapKeys;
    }

    private int nextNode;
    private Key secretKey;
    private List<Key> wrapKeys;
    
    public int getNextNode() {
	return nextNode;
    }

    public Key getSecretKey() {
	return secretKey;
    }

    public List<Key> getWrapKeys() {
	return wrapKeys;
    }
}
