package com.onion;

import java.util.*;
import java.io.*;
import java.security.*;

import javax.crypto.*;

import org.bouncycastle.util.io.pem.*;
import org.bouncycastle.crypto.*;


public class CipherUtils {

    private static Random rand = new Random();

    public static <T> T readPublicKey(String keyfile, Class<T> clazz) {
	try {
	    PemReader keyReader = new PemReader(new FileReader("keyFile"));
	    Object o = keyReader.readPemObject();

	    System.out.println(o.getClass().getName());

	    return clazz.cast(o);
	            
	} catch (IOException e) {
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

    public static byte[] getRandomBytes(int count) {
	byte[] b = new byte[count];
	rand.nextBytes(b);
	return b;
    }

    public static byte[] onionEncryptMessage(byte[] msg, List<HopSpec> hops) {
	byte[] data = msg;
	for (int i = 0; i < hops.size(); i++) {
	    OnionMessage onionMsg = new OnionMessage(hops.get(i).getConnectionId(), data);
	    ByteArrayOutputStream objectStream;
	    try {
		objectStream = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(objectStream);
		oos.writeObject(onionMsg);
	    } catch (IOException e) {
		e.printStackTrace();
		return null;
	    }

	    Cipher cipher;
	    try {
		cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
		cipher.init(Cipher.ENCRYPT_MODE, hops.get(i).getKey());
	    } catch (GeneralSecurityException e) {
		e.printStackTrace();
		return null;
	    }

	    data = applyCipher(objectStream.toByteArray(), cipher);
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
