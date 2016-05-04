package com.onion;

import java.util.*;
import java.security.*;
import java.io.*;

/*
    CircuitHopKeyRequest.java

    Represents the circuit hop key request message that is wrapped into
    an onion message to be sent. Described further in the write up.
*/


public class CircuitHopKeyRequest implements Serializable {
    public CircuitHopKeyRequest(List<Key> keys) {
	this.keys = keys;
    }

    private List<Key> keys;

    public List<Key> getKeys() {
	return keys; 
    }
}
