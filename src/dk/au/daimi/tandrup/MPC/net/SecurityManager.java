package dk.au.daimi.tandrup.MPC.net;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;


public class SecurityManager {
	public static boolean validate(KeyStore store, X509Certificate cert) throws KeyStoreException {
		Enumeration<String> aliases = store.aliases();
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			Certificate[] certs = store.getCertificateChain(alias);
			if (certs != null) {
				for (int i = 0; i < certs.length; i++) {
					X509Certificate storeCert = (X509Certificate)certs[i];
					if (storeCert.getSubjectX500Principal().equals(cert.getIssuerX500Principal()))
						return true;
				}
			}
		}
		return false;
	}

	public static KeyStore getJavaKeyStoreFromFile(String filename) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
		KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
		InputStream in = SecurityManager.class.getClassLoader().getResourceAsStream(filename);
		store.load(in, null);
		in.close();

		return store;
	}
	
	public static String getMsgDigestAlgorithm() {
		return "SHA-256";
	}
	
	public static String getSigningAlgorithm() {
		return "SHA1withRSA";
	}
	
	public static String getCipherAlgorithm() {
		return "RSA";
	}
	
	public static SSLContext getSSLContext(String storeFilename) throws GeneralSecurityException, IOException {
	       KeyStore store = SecurityManager.getJavaKeyStoreFromFile(storeFilename);
	       return getSSLContext(store);
	}
	
	public static SSLContext getSSLContext(KeyStore store) throws GeneralSecurityException {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(store, "secret".toCharArray());
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(store);
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext;
	}
	
	public static int getSessionIDPartLength() {
		return 8;
	}
}
