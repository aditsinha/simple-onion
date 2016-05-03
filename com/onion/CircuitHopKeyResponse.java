package com.onion;

import java.util.*;
import java.io.*;
import java.security.*;

import javax.crypto.*;

import org.bouncycastle.util.io.pem.*;
import org.bouncycastle.crypto.*;

public class CircuitHopKeyResponse implements Serializable {
	Key rsaPrivateKey;

	public CircuitHopKeyResponse (Key rsaPrivateKey) {
		this.rsaPrivateKey = rsaPrivateKey;
	}

	public Key getKey() {
		return rsaPrivateKey;
	} 
}
