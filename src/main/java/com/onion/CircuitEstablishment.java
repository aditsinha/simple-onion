package com.onion;

import java.util.*;
import java.io.*;
import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import org.bouncycastle.util.io.pem.*;
import org.bouncycastle.crypto.*;

public class CircuitEstablishment {

    private List<Integer> hops;
    private int destination;

    private HopSpec outstandingHop;
    public List<HopSpec> establishedHops;

    public boolean isConnectionEstablished;

    private Key hopPublicKey, hopSymmetricKey;

    public CircuitEstablishment(List<Integer> hops, int destination) {
	this.hops = hops;
	isConnectionEstablished = false;
	establishedHops = new ArrayList<>(hops.size());
	    
    }

    public byte[] encryptAsymmetric(byte[] msg, PublicKey key) {
	Cipher cipher;
	try {
	    cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
	    cipher.init(Cipher.ENCRYPT_MODE, key);
	} catch (GeneralSecurityException e) {
	    e.printStackTrace();
	    return null;
	}

	return CipherUtils.applyCipher(msg, cipher);
    }

    public void processMessage(byte[] msg) {
	CircuitHopReplyMessage reply;
	// expect to receive a CircuitHopReplyMessage
	try {
	    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(msg));
	    reply = (CircuitHopReplyMessage) ois.readObject();
	            
	} catch (IOException | ClassNotFoundException e) {
	    // this is bad
	    e.printStackTrace();
	    return;
	}

	outstandingHop.setConnectionId(reply.getHopConnectionId());
	establishedHops.add(outstandingHop);
	outstandingHop = null;
	    
    }

    public OnionMessage getFirstMessage() {
	return new OnionMessage(OnionMessage.MsgType.KEY_REQUEST, CipherUtils.serialize(new CircuitHopKeyRequest(keyList)));
    }
    
}
