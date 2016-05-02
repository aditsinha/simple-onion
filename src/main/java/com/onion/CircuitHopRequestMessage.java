package com.onion;

import java.security.*;
import java.io.*;

public class CircuitHopRequestMessage implements Serializable {
    public CircuitHopRequestMessage(int nextNode, Key key) {
	this.nextNode = nextNode;
	this.key = key;
    }

    int nextNode;
    Key key;
    
    public int getNextNode() {
	return nextNode;
    }

    public Key getKey() {
	return key;
    }
}
