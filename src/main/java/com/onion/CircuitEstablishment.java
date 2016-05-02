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

    public List<Key> keyList;

    private boolean isConnectionEstablished;

    private Key hopPublicKey, hopSymmetricKey;

    public CircuitEstablishment(List<Integer> hops, int destination) {
	this.hops = hops;
	isConnectionEstablished = false;
	keyList = new ArrayList<>();
    }

    public OnionMessage processMessage(OnionMessage msg) {
	switch (msg.getType()) {
	case HOP_REPLY:
	    // established connection.  make the next hop
	    keyList.add(hopSymmetricKey);
	    hopPublicKey = null;
	    hopSymmetricKey = null;
	    if (keyList.size() == hops.size()) {
		isConnectionEstablished = true;
	    }
	    return new OnionMessage(OnionMessage.MsgType.KEY_REQUEST,
				    CipherUtils.serialize(new CircuitHopKeyRequest(keyList)));
	case KEY_REPLY:
	    CircuitHopKeyReply reply = (CircuitHopKeyReply) CipherUtils.deserialize(msg.getData());
	    hopPublicKey = reply.getKey();
	    hopSymmetricKey = new SecretKeySpec(CipherUtils.getRandomBytes(16), "AES");
	    int nextHop = (keyList.size() < hops.size() - 1) ? hops.get(keyList.size() + 1) : destination;
	    byte[] requestMessageData =
		CipherUtils.serialize(new CircuitHopRequestMessage(nextHop,
								   hopSymmetricKey,
								   keyList));
	    return new OnionMessage(OnionMessage.MsgType.HOP_REQUEST,
				    CipherUtils.applyCipher(requestMessageData, CipherUtils.ASYM_ALGORITHM,
							    Cipher.ENCRYPT_MODE,
							    hopPublicKey));
	default:
	    System.out.println("ERROR UNEXPECTED MESSAGE TYPE: " + msg.getType());
	    return null;
	}
    }

    public OnionMessage getFirstMessage() {
	return new OnionMessage(OnionMessage.MsgType.KEY_REQUEST, CipherUtils.serialize(new CircuitHopKeyRequest(keyList)));
    }
}
