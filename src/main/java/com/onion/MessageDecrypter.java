package com.onion;

import java.util.*;
import java.io.*;
import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import org.bouncycastle.util.io.pem.*;
import org.bouncycastle.crypto.*;

public class MessageDecrypter {

    private Key rsaPrivateKey;
    private Map<Integer, Key> connectionKeys;

    public CircuitHopRequestMessage unwrapAsymmetric(byte[] recv) {
	Cipher cipher;
	try {
	    cipher = Cipher.getInstance("RSA/ECB/PKCSPadding", "BC");
	    cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
	} catch (GeneralSecurityException e) {
	    e.printStackTrace();
	    return null;
	}	

	byte[] decrypted = CipherUtils.applyCipher(recv, cipher);

	try {
	    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decrypted));
	    return (CircuitHopRequestMessage) ois.readObject();
	} catch (ClassNotFoundException | IOException e) {
	    // wrong type of object?
	    return null;
	}
    }

    public OnionMessage unwrapOnionLayer(byte[] recv, int connectionNumber) {
	Cipher cipher;
	try {
	    Key key = connectionKeys.get(connectionNumber);
	    cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
	    cipher.init(Cipher.DECRYPT_MODE, key);
	} catch (GeneralSecurityException e) {
	    e.printStackTrace();
	    return null;
	}

	byte[] decrypted = CipherUtils.applyCipher(recv, cipher);

	try {
	    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decrypted));
	    return (OnionMessage) ois.readObject();
	            
	} catch (IOException | ClassNotFoundException e) {
	    // wrong type of object?
	    return null;
	            
	}
	    
    }
    
}
