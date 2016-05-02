package com.onion;

import java.util.*;

public class CircuitHopKeyRequest {
    public CircuicHopKeyRequest(List<Key> keys) {
	this.keys = keys;
    }

    private List<Key> keys;

    public List<Key> getKeys() {
	return keys; 
    }
}
