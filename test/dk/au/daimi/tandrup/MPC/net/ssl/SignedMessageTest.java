package dk.au.daimi.tandrup.MPC.net.ssl;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.SecurityManager;

public class SignedMessageTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test(timeout=500)
	public void testReadExternal() throws CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException, InvalidKeyException, SignatureException, ClassNotFoundException {
		KeyStore store = SecurityManager.getJavaKeyStoreFromFile("server1.store");

		PrivateKey key = (PrivateKey)store.getKey("server1", "secret".toCharArray());

		SignedMessage sMsg = new SignedMessage(new Activity("T"), "Hej", key);
		sMsg.setMessageID(32);
		
		ByteArrayOutputStream outByte = new ByteArrayOutputStream();
		ObjectOutput outObj = new ObjectOutputStream(outByte);
		
		outObj.writeObject(sMsg);
		
		outObj.close();
		outByte.close();
		
		ByteArrayInputStream inByte = new ByteArrayInputStream(outByte.toByteArray());
		ObjectInput inObj = new ObjectInputStream(inByte);
		
		SignedMessage obj = (SignedMessage)inObj.readObject();
		
		System.out.println(obj);

		assertEquals("Hej", obj.getObject());
		assertEquals(32L, obj.getMessageID());
	}
}
