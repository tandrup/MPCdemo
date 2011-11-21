package dk.au.daimi.tandrup.MPC.net;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class RSAPerformance {
	public static final int BUFFER_SIZE = 102400;
	public static final int REPEAT = 1000;
	
	public static byte[] cipherKey;
	public static byte[] ciphertext;
	
	public static void main(String[] args) throws Exception {
		byte[] plaintext = new byte[BUFFER_SIZE];
		new Random().nextBytes(plaintext);

		KeyStore keyStore = SecurityManager.getJavaKeyStoreFromFile("server1.store");

		X509Certificate rsaCert = (X509Certificate)keyStore.getCertificate("server1");
		PrivateKey rsaKey = (PrivateKey)keyStore.getKey("server1", "secret".toCharArray());

	    KeyGenerator keygen = KeyGenerator.getInstance("RC4");
	    SecretKey aesKey = keygen.generateKey();

	    long startTime = System.currentTimeMillis();
	    for (int i = 0; i < REPEAT; i++) {
	    	Cipher rsaCipher = Cipher.getInstance("RSA");
	    	Cipher aesCipher = Cipher.getInstance("RC4/ECB/NoPadding");
	    	
	    	rsaCipher.init(Cipher.ENCRYPT_MODE, rsaCert);
	    	aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);

	    	byte[] cipherKey = rsaCipher.doFinal(aesKey.getEncoded());
	    	byte[] ciphertext = aesCipher.doFinal(plaintext);
	    	
	    	rsaCipher.init(Cipher.DECRYPT_MODE, rsaKey);
	    	aesCipher.init(Cipher.DECRYPT_MODE, aesKey);

	    	//byte[] decipherKey = 
	    	rsaCipher.doFinal(cipherKey);
	    	//byte[] deciphertext = 
	    	aesCipher.doFinal(ciphertext);
	    }
	    long stopTime = System.currentTimeMillis();
	    
	    double rate = (double)BUFFER_SIZE * REPEAT / (stopTime - startTime);
	    System.out.println(rate);
	}
}
