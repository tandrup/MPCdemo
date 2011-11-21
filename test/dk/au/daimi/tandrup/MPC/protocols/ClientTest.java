package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.LongField;
import dk.au.daimi.tandrup.MPC.math.fields.polynomial.PolynomialField;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.SecurityManager;
import dk.au.daimi.tandrup.MPC.net.ssl.Endpoint;
import dk.au.daimi.tandrup.MPC.net.ssl.SSLCommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.gates.AbstractGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.AffineGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.InputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.LocalInputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.MultGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.OutputGate;

public class ClientTest {
	protected static final Logger logger = Logger.getLogger(ClientTest.class.getName());

	public static void main(String[] args) throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException, ExecutionException {
		// Setup logging
		Handler[] handlers = Logger.getLogger("").getHandlers();
		for ( int index = 0; index < handlers.length; index++ ) {
			handlers[index].setLevel( Level.FINEST );
		}
		//Logger.getLogger("dk.au.daimi.tandrup.MPC.net.ssl.SSLCommunicationChannel").setLevel(Level.CONFIG);
		//Logger.getLogger("dk.au.daimi.tandrup.MPC.net.ssl").setLevel(Level.FINER);

		
		logger.info("Test started");

		SSLCommunicationChannel channel = null;
		int argOffset = 0;

		try {
			Activity testActivity = new Activity("Test");
			Random random = SecureRandom.getInstance("SHA1PRNG");

			int n = (args.length - 3) / 2 + 1;
			int t = n / 3;

			if (t >= n / 3.0)
				t = n / 3 - 1;
			
			if (t < 1)
				throw new IllegalArgumentException("Threshold to small " + t + " network: " + n);

			logger.info("Network size: " + n + ", threshold: " + t);
			
			String keyStoreFile = args[argOffset++];
			String localAlias = args[argOffset++];
			
			KeyStore store = SecurityManager.getJavaKeyStoreFromFile(keyStoreFile);

			// Creating endpoint list
			List<Endpoint> endpoints = new ArrayList<Endpoint>(n-1);
			for (int i = 0; i < n-1; i++) {
				InetAddress addr = InetAddress.getByName(args[argOffset++]);
				String alias = args[argOffset++];

				int port = 8001;
				
				X509Certificate cert = (X509Certificate)store.getCertificate(alias);
				
				endpoints.add(new Endpoint(addr, port, cert.getSubjectX500Principal().getName()));
			}

			channel = new SSLCommunicationChannel(8001, store, random);

			X509Certificate cert = (X509Certificate)store.getCertificate(localAlias);
			PrivateKey key = (PrivateKey)store.getKey(localAlias, "secret".toCharArray());
			channel.init(endpoints, cert, key, 10000);

			channel.startSession();

			logger.info("Session started");

			int mCount = Integer.parseInt(args[argOffset++]);
			int rCount = mCount;
			int aCount = 1;
			int oCount = 4;
			Participant[] inputProviders = new Participant[3];
			int i = 0;
			for (Participant participant : channel.listConnectedParticipants()) {
				inputProviders[i++] = participant;
				if (i == inputProviders.length)
					break;
			}

			Field fieldF = new LongField(11);
			FieldPolynomial1D basePoly = new FieldPolynomial1D(new FieldElement[] {fieldF.element(7), fieldF.element(1), fieldF.element(1)});
			Field fieldG = new PolynomialField(basePoly);

			Disputes disputes = new Disputes();
			PreProcess preProcess = new PreProcess(new Random(), channel, t, testActivity.subActivity("PreProc"), rCount, inputProviders, 0, aCount, oCount, fieldF, fieldG, disputes);
			PreProcess preProcessMult = new PreProcess(new Random(), channel, t, testActivity.subActivity("PreMult"), 0, new Participant[] {}, mCount, 0, 0, fieldF, fieldG, disputes);
			Evaluate evaluate = new Evaluate(channel, t, testActivity.subActivity("Eval"));
			
			long preProcessStart = System.currentTimeMillis();
			preProcess.run();
			long preProcessStop = System.currentTimeMillis();

			logger.info("Preprocessing done");
			
			long preProcessMultStart = System.currentTimeMillis();
			preProcessMult.run();
			long preProcessMultStop = System.currentTimeMillis();
			
			logger.info("Preprocessing for Multiplication done");
			
			InputGate i1 = preProcess.getResult().getInputGate(0);
			InputGate i2 = preProcess.getResult().getInputGate(1);
			MultGate m1 = preProcessMult.getResult().getMultGate(0);
			MultGate m2 = preProcessMult.getResult().getMultGate(1);
			AffineGate a1 = preProcess.getResult().getAffineGate(0);
			OutputGate o1 = preProcess.getResult().getOutputGate(0);
			OutputGate o2 = preProcess.getResult().getOutputGate(1);
			OutputGate o3 = preProcess.getResult().getOutputGate(2);
			OutputGate o4 = preProcess.getResult().getOutputGate(3);

			MultGate[] ms = new MultGate[preProcessMult.getResult().getMultGateCount()];
			for (int j = 0; j < ms.length; j++) {
				ms[j] = preProcessMult.getResult().getMultGate(j);
				ms[j].setInputGates(i1, preProcess.getResult().getRandomGate(j));
			}
			
			if (i1 instanceof LocalInputGate)
				((LocalInputGate)i1).setInput(fieldF.element(8));
			if (i2 instanceof LocalInputGate)
				((LocalInputGate)i2).setInput(fieldF.element(3));

			m1.setInputGates(i1, i2);
			m2.setInputGates(i1, i1);
			
			a1.configure(fieldF.element(7), new AbstractGate[] { m1, m2, i1, i2 }, new FieldElement[] { fieldF.element(1), fieldF.element(1), fieldF.element(4), fieldF.element(2) });
			
			o1.setInput(m1);
			o2.setInput(m2);
			o3.setInput(a1);
			o4.setInput(i1);

			long evaluateInputStart = System.currentTimeMillis();
			evaluate.evaluate(new AbstractGate[] { i1, i2 });
			long evaluateInputStop = System.currentTimeMillis();

			long evaluateMultStart = System.currentTimeMillis();
			evaluate.evaluate(ms);
			long evaluateMultStop = System.currentTimeMillis();

			long evaluateOutputStart = System.currentTimeMillis();
			evaluate.evaluate(new AbstractGate[] { o1, o2, o3, o4 });
			long evaluateOutputStop = System.currentTimeMillis();
			
			logger.info("Test done!\n" +
					"Result:\n" +
					"  o1 = " + o1.output() + "\n" +
					"  o2 = " + o2.output() + "\n" +
					"  o3 = " + o3.output() + "\n" +
				    "  o4 = " + o4.output() + "\n" +
				    "Time statistic:\n" +
				    "  PreProcessing: " + (preProcessStop - preProcessStart) + " ms\n" +
				    "  PreProcessingMult: " + (preProcessMultStop - preProcessMultStart) + " ms\n" +
				    "  EvaluateInput: " + (evaluateInputStop - evaluateInputStart) + " ms\n" + 
				    "  EvaluateMult: " + (evaluateMultStop - evaluateMultStart) + " ms\n" + 
				    "  EvaluateOutput: " + (evaluateOutputStop - evaluateOutputStart) + " ms"
				    );
			
		} finally {
			if (channel != null)
				channel.close();
		}
	}
}
