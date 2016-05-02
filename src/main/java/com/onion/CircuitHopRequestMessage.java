package com.onion;

import java.security.*;
import java.io.*;

public class CircuitHopRequestMessage implements Serializable {
    public CircuitHopRequestMessage(int nextNode, Key secretKey) {
	this.nextNode = nextNode;
	this.secretKey = secretKey;
    }

    private int nextNode;
    private Key secretKey;
    
    public int getNextNode() {
	return nextNode;
    }
}
