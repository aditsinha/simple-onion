package com.onion;

import java.util.*;
import java.security.*;
import java.io.*;

public class CircuitHopKeyRequest implements Serializable {
    public CircuitHopKeyRequest(List<Key> keys) {
	this.keys = keys;
    }

    private List<Key> keys;

    public List<Key> getKeys() {
	return keys; 
    }
}
