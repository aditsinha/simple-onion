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

    public static String ASYM_ALGORITHM = "RSA/ECB/PKCSPadding";
    public static String SYM_ALGORITHM = "AES/ECB/PKCS7Padding";

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
	    cipher.init(mode, key);
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

    // assume that the first key's hop is in hops.get(0)
    public static OnionMessage onionEncryptMessage(OnionMessage msg, List<Key> hops) {
	boolean isPoison = (msg.getType() == OnionMessage.MsgType.POISON);

	for (int i = hops.size() - 1; i >= 0; i--) {
	    msg = new OnionMessage(isPoison ? OnionMessage.MsgType.POISON : OnionMessage.MsgType.DATA,
				   applyCipher(msg.pack(), SYM_ALGORITHM,
					       Cipher.ENCRYPT_MODE, hops.get(i)));
	}

	return msg;
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
