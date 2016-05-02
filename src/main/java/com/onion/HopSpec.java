package com.onion;

import java.security.*;
import javax.crypto.spec.*;

import org.bouncycastle.jce.spec.*;

public class HopSpec {
    int connectionId;
    Key key;

    public int getConnectionId() {
	return connectionId;
    }

    public Key getKey() {
	return key;
	    
    }

    public void setConnectionId(int id) {
	connectionId = id;
    }

    public void setKey(Key key) {
	this.key = key;
    }    
}
