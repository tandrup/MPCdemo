package dk.au.daimi.tandrup.MPC.math;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.BigIntegerFieldElement;

public class ShamirSecretSharingTest {
	BigInteger modulo = BigInteger.valueOf(101);
	Random randomGen = new Random();	
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSplitJoin() {
		BigIntegerFieldElement secret = new BigIntegerFieldElement(BigInteger.valueOf(43), modulo);
		
		FieldElement[] shares = ShamirSecretSharing.split(secret, 5, 15, randomGen);
		
		assertEquals("Shares length", 15, shares.length);
		
		FieldElement[] receivedShares = new FieldElement[5];
		FieldElement[] receivedShareIndexes = new FieldElement[5];
		
		for (int i = 0; i < receivedShares.length; i++) {
			receivedShares[i] = shares[i*2+1];
			receivedShareIndexes[i] = secret.field().element(i*2+2);
		}
		
		FieldElement receivedSecret = ShamirSecretSharing.join(receivedShares, receivedShareIndexes, 5);
		
		assertEquals("Received Secret", new BigIntegerFieldElement(BigInteger.valueOf(43), modulo), receivedSecret);

		receivedShares = new FieldElement[8];
		receivedShareIndexes = new FieldElement[8];
		
		for (int i = 0; i < receivedShares.length; i++) {
			receivedShares[i] = shares[i+1];
			receivedShareIndexes[i] = secret.field().element(i+2);
		}
		
		receivedSecret = ShamirSecretSharing.join(receivedShares, receivedShareIndexes, 5);
		
		assertEquals("Received Secret", new BigIntegerFieldElement(BigInteger.valueOf(43), modulo), receivedSecret);
	}

	@Test
	public void testSplitJoinX() {
		FieldElement[] coeffs = new BigIntegerFieldElement[3];
		coeffs[0] = new BigIntegerFieldElement(BigInteger.valueOf(65), modulo);
		coeffs[1] = new BigIntegerFieldElement(BigInteger.valueOf(32), modulo);
		coeffs[2] = new BigIntegerFieldElement(BigInteger.valueOf(12), modulo);
		Field field = coeffs[0].field();
		FieldPolynomial1D poly = new FieldPolynomial1D(coeffs);
		
		FieldElement[] shares = new FieldElement[5];
		FieldElement[] shareIndexes = new FieldElement[5];
		
		for (int i = 0; i < shares.length; i++) {
			shareIndexes[i] = field.element(i*2+2);
			shares[i] = poly.eval(shareIndexes[i]);
		}
		
		FieldElement x;
		FieldElement receivedSecret;
		
		x = new BigIntegerFieldElement(BigInteger.valueOf(0), modulo);
		receivedSecret = ShamirSecretSharing.join(shares, shareIndexes, x);
		assertEquals("Received Secret", new BigIntegerFieldElement(BigInteger.valueOf(65), modulo), receivedSecret);

		x = new BigIntegerFieldElement(BigInteger.valueOf(1), modulo);
		receivedSecret = ShamirSecretSharing.join(shares, shareIndexes, x);
		assertEquals("Received Secret", poly.eval(x), receivedSecret);

		x = new BigIntegerFieldElement(BigInteger.valueOf(24), modulo);
		receivedSecret = ShamirSecretSharing.join(shares, shareIndexes, x);
		assertEquals("Received Secret", poly.eval(x), receivedSecret);

		shares = new FieldElement[8];
		shareIndexes = new FieldElement[8];
		
		for (int i = 0; i < shares.length; i++) {
			shareIndexes[i] = field.element(i+2);
			shares[i] = poly.eval(shareIndexes[i]);
		}
		
		receivedSecret = ShamirSecretSharing.join(shares, shareIndexes, new BigIntegerFieldElement(BigInteger.valueOf(0), modulo));
		
		assertEquals("Received Secret", new BigIntegerFieldElement(BigInteger.valueOf(65), modulo), receivedSecret);
	}
}
