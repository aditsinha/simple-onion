package com.onion;

import java.util.*;
import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.math.*;

import javax.crypto.*;

import org.bouncycastle.util.io.pem.*;
import org.bouncycastle.crypto.*;


public class CipherUtils {

    private static Random rand = new Random();

    public static KeyPair generateRsaKeyPair() {
	try {
	    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");

	    keyGen.initialize(new RSAKeyGenParameterSpec(768, BigInteger.valueOf(65537)));
	    return keyGen.generateKeyPair();
	} catch (GeneralSecurityException e) {
	    e.printStackTrace();
	    return null;
	}
    }

    
    public static byte[] applyCipher(byte[] data, Cipher cipher) {
	try {
	    return cipher.doFinal(data);
	} catch (GeneralSecurityException e) {
	    e.printStackTrace();
	    return null;
	}
    }

    public static byte[] applyCipher(byte[] data, String cipherAlgorithm, int mode, Key key) {
	try {
	    Cipher cipher = Cipher.getInstance(cipherAlgorithm, "BC");
	    cipher.init(Cipher.ENCRYPT_MODE, key);
	    return cipher.doFinal(data);
	} catch (GeneralSecurityException e) {
	    e.printStackTrace();
	    return null;
	}
    }

    public static Object deserialize(byte[] data) {
	try {
	    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
	    return ois.readObject();
	} catch (IOException | ClassNotFoundException e) {
	    // this is bad
	    e.printStackTrace();
	    return null;
	}
    }
    
    public static byte[] getRandomBytes(int count) {
	byte[] b = new byte[count];
	rand.nextBytes(b);
	return b;
    }

    public static byte[] onionEncryptMessage(byte[] msg, List<HopSpec> hops) {
	byte[] data = msg;
	for (int i = 0; i < hops.size(); i++) {
	    OnionMessage onionMsg = new OnionMessage(hops.get(i).getConnectionId(), data);
	    byte[] unencrypted = serialize(onionMsg);
	    data = applyCipher(unencrypted, "AES/ECB/PKCS7Padding", Cipher.ENCRYPT_MODE, hops.get(i).getKey());
	}

	return data;
    }

    public static byte[] serialize(Serializable obj) {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	ObjectOutputStream oos = null;
	try {
	    oos = new ObjectOutputStream(baos);
	    oos.writeObject(obj);
	    return baos.toByteArray();
	            
	} catch (IOException e) {
	    return null;
	            
	}
	    
    }
}
