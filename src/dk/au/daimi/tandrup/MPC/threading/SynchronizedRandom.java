package dk.au.daimi.tandrup.MPC.threading;

import java.util.Random;

public class SynchronizedRandom extends Random {
	private static final long serialVersionUID = 1L;

	private Random inner;
	
	public SynchronizedRandom(Random innerRandom) {
		this.inner = innerRandom;
	}

	@Override
	public synchronized boolean nextBoolean() {
		return inner.nextBoolean();
	}

	@Override
	public synchronized void nextBytes(byte[] bytes) {
		inner.nextBytes(bytes);
	}

	@Override
	public synchronized double nextDouble() {
		return inner.nextDouble();
	}

	@Override
	public synchronized float nextFloat() {
		return inner.nextFloat();
	}

	@Override
	public synchronized double nextGaussian() {
		return inner.nextGaussian();
	}

	@Override
	public synchronized int nextInt() {
		return inner.nextInt();
	}

	@Override
	public synchronized int nextInt(int n) {
		return inner.nextInt(n);
	}

	@Override
	public synchronized long nextLong() {
		return inner.nextLong();
	}

	@Override
	public synchronized void setSeed(long seed) {
		inner.setSeed(seed);
	}
}
