package dk.au.daimi.tandrup.MPC.threading;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BoundedSharedThreadPool implements ExecutorService {

	private static final UnboundedThreadPool innerPool = new UnboundedThreadPool();

	public static void shutdownSharedPool() {
		innerPool.shutdown();
	}
	
	private Queue<Task> tasks = new LinkedList<Task>();

	private final int maxCount;

	private Collection<Task> running = new HashSet<Task>();

	public BoundedSharedThreadPool(String name, int maxCount) {
		this.maxCount = maxCount;

		innerPool.addListener(this);
	}

	void executionCompleted() {
		checkRunning();
	}
	
	private void checkRunning() {
		Collection<Task> deadTasks = new ArrayList<Task>(maxCount);

		synchronized (this) {
			for (Task task : running) {
				if (task.isDone()) {
					deadTasks.add(task);
				}
			}
		}

		for (Task task : deadTasks) {
			signalStop(task);
		}
	}

	public boolean awaitTermination(long arg0, TimeUnit arg1)
	throws InterruptedException {
		throw new IllegalStateException("NOT IMPLEMENTED");
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> arg0)
	throws InterruptedException {
		throw new IllegalStateException("NOT IMPLEMENTED");
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> arg0,
			long arg1, TimeUnit arg2) throws InterruptedException {
		throw new IllegalStateException("NOT IMPLEMENTED");
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> arg0)
	throws InterruptedException, ExecutionException {
		throw new IllegalStateException("NOT IMPLEMENTED");
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> arg0, long arg1,
			TimeUnit arg2) throws InterruptedException, ExecutionException,
			TimeoutException {
		throw new IllegalStateException("NOT IMPLEMENTED");
	}

	public boolean isShutdown() {
		throw new IllegalStateException("NOT IMPLEMENTED");
	}

	public boolean isTerminated() {
		throw new IllegalStateException("NOT IMPLEMENTED");
	}

	public void shutdown() {
		innerPool.removeListener(this);
	}

	public List<Runnable> shutdownNow() {
		throw new IllegalStateException("NOT IMPLEMENTED");
	}

	public synchronized <T> Future<T> submit(Callable<T> arg0) {
		CallTask<T> task = new CallTask<T>(arg0);

		tasks.add(task);

		startNew();

		return task;
	}

	public synchronized Future<?> submit(Runnable arg0) {
		RunTask task = new RunTask(arg0);

		tasks.add(task);

		startNew();

		return task;
	}

	public <T> Future<T> submit(Runnable arg0, T arg1) {
		throw new IllegalStateException("NOT IMPLEMENTED");
	}

	public void execute(Runnable arg0) {
		throw new IllegalStateException("NOT IMPLEMENTED");
	}

	private synchronized void startNew() {
		if (running.size() < maxCount) {
			Task task = tasks.poll();
			if (task != null) {
				task.submit();
				running.add(task);
			}
		}
	}

	private synchronized void signalStop(Task task) {
		running.remove(task);

		startNew();
	}

	private abstract class Task<T> implements Future<T> {
		protected final ReadWriteLock lock = new ReentrantReadWriteLock();
		protected final Condition running = lock.writeLock().newCondition();

		public abstract void submit();

		public abstract Future getInnerFuture();

		public boolean isDone() {
			lock.readLock().lock();
			try {
				if (getInnerFuture() == null)
					return false;
			} finally {
				lock.readLock().unlock();
			}			

			return getInnerFuture().isDone();
		}

		protected void waitForInnerFuture() throws InterruptedException {
			lock.writeLock().lock();
			try {
				while (getInnerFuture() == null) {
					running.await();
				}
			} finally {
				lock.writeLock().unlock();
			}
		}

		public boolean cancel(boolean arg0) {
			throw new IllegalStateException("NOT IMPLEMENTED");
		}

		public boolean isCancelled() {
			throw new IllegalStateException("NOT IMPLEMENTED");
		}

		public T get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
			throw new IllegalStateException("NOT IMPLEMENTED");
		}
	}

	private class RunTask extends Task {

		final Runnable job;
		private Future<?> innerFuture;

		public RunTask(Runnable jobRun) {
			this.job = jobRun;
		}

		public void submit() {
			lock.writeLock().lock();
			lock.readLock().lock();
			try {
				if (innerFuture != null)
					throw new IllegalStateException("Already submitted");

				innerFuture = innerPool.submit(job);
				running.signal();
			} finally {
				lock.readLock().unlock();
				lock.writeLock().unlock();
			}
		}

		public Object get() throws InterruptedException, ExecutionException {
			waitForInnerFuture();

			lock.readLock().lock();
			try {
				return innerFuture.get();
			} finally {
				lock.readLock().unlock();
				signalStop(this);
			}
		}

		public Future getInnerFuture() {
			return innerFuture;
		}
	}

	private class CallTask<T> extends Task<T> {

		final Callable<T> job;
		private Future<T> innerFuture;

		public CallTask(Callable<T> job) {
			this.job = job;
		}

		public Future getInnerFuture() {
			return innerFuture;
		}

		public void submit() {
			lock.writeLock().lock();
			lock.readLock().lock();
			try {
				if (innerFuture != null)
					throw new IllegalStateException("Already submitted");

				innerFuture = innerPool.submit(job);
				running.signal();
			} finally {
				lock.readLock().unlock();
				lock.writeLock().unlock();
			}
		}

		public T get() throws InterruptedException, ExecutionException {
			waitForInnerFuture();

			lock.readLock().lock();
			try {
				return innerFuture.get();
			} finally {
				lock.readLock().unlock();
				signalStop(this);
			}
		}
	}
}

