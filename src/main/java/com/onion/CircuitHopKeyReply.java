package com.onion;

import java.util.*;
import java.security.*;
import java.io.*;

public class CircuitHopKeyReply implements Serializable {
    public CircuitHopKeyReply(Key key) {
	this.key = key;
    }

    private Key key; 

    public Key getKey() {
	return key; 
    }
}
