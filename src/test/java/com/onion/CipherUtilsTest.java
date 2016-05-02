package com.onion;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import java.io.*;
import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import org.bouncycastle.util.io.pem.*;
import org.bouncycastle.crypto.*;
import org.bouncycastle.util.encoders.*;
import org.bouncycastle.jce.provider.*;


public class CipherUtilsTest {

    private static byte[] keyData = Hex.decode("000102030405060708090A0B0C0D0E0F");

    @BeforeClass
    public static void setupClass() {
	Security.addProvider(new BouncyCastleProvider());
    }
    
    @Test
    public void testApplyCipher() throws Exception {
	byte[] data = new byte[200];
	for (int i = 0; i < data.length; i++) {
	    data[i] = (byte)i;
	}

	Key key = new SecretKeySpec(keyData, "AES");
	  
	Cipher c = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
	c.init(Cipher.ENCRYPT_MODE, key);
	byte[] encrypted = CipherUtils.applyCipher(data, c);

	Cipher d = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
	d.init(Cipher.DECRYPT_MODE, key);
	byte[] decrypted = CipherUtils.applyCipher(encrypted, d);

	assertArrayEquals(data, decrypted);
    }

    
    
}
