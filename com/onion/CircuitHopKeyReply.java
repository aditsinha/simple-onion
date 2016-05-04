package com.onion;

import java.util.*;
import java.security.*;
import java.io.*;

/*
    CircuitHopKeyReply.java

    Represents the circuit hop key reply message that is wrapped into
    an onion message to be sent. Described further in the write up.
*/


public class CircuitHopKeyReply implements Serializable {
    public CircuitHopKeyReply(Key key) {
	this.key = key;
    }

    private Key key; 

    public Key getKey() {
	return key; 
    }
}
