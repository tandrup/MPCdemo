package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;

import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;

public class Triples extends AbstractProtocol {
	private int l;
	private Field fieldF;
	private RandomPair[] randomPairs;
	
	public Triples(CommunicationChannel channel, int threshold, Activity activity, int l, RandomPair[] randomPairs, Field fieldF) {
		super(channel, threshold, activity);
		this.l = l;
		this.randomPairs = randomPairs;
		this.fieldF = fieldF;
	}

	public Triple[] run() throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException, ExecutionException {
		RandomPair[] rs = new RandomPair[2*l];
		RandomPair[] drs = new RandomPair[l];
		
		System.arraycopy(randomPairs, 0, rs, 0, 2*l);
		System.arraycopy(randomPairs, 2*l, drs, 0, l);
		
		PartialTriple[] partialTriples = new PartialTriple[l];
		for (int i = 0; i < l; i++) {
			partialTriples[i] = 
				new PartialTriple(rs[2*i].getR1(),
								  rs[2*i+1].getR1(),
								  drs[i]);
		}
		
		FieldElement[] Dshares = new FieldElement[l];
		for (int i = 0; i < l; i++) {
			Dshares[i] = fieldF.add(fieldF.multiply(partialTriples[i].a, partialTriples[i].b), partialTriples[i].r.getR2());
		}
		
		OpenRobustWrapper open = new OpenRobustWrapper(channel, threshold, activity.subActivity("OpenWrap"), 2*threshold, Dshares);
		FieldElement[] Ds = open.run();
		
		Triple[] result = new Triple[l];
		for (int i = 0; i < l; i++) {
			FieldElement aShare = partialTriples[i].a;
			FieldElement bShare = partialTriples[i].b;
			FieldElement cShare = Ds[i].subtract(partialTriples[i].r.getR1());
			result[i] = new Triple(aShare, bShare, cShare);
		}
		
		return result;
	}

	private class PartialTriple {
		private FieldElement a, b;
		private RandomPair r;
		public PartialTriple(FieldElement a, FieldElement b, RandomPair r) {
			super();
			this.a = a;
			this.b = b;
			this.r = r;
		}
		public FieldElement getA() { return a; }
		public FieldElement getB() { return b; }
		public RandomPair getR() { return r; }
		
		@Override
		public String toString() {
			return "(" + a + ", " + b + ", " + r + ")";
		}
	}
	
	public class Triple {
		private FieldElement a, b, c;

		public Triple(FieldElement a, FieldElement b, FieldElement c) {
			super();
			this.a = a;
			this.b = b;
			this.c = c;
		}

		public FieldElement getA() {
			return a;
		}

		public FieldElement getB() {
			return b;
		}

		public FieldElement getC() {
			return c;
		}
	}
}
