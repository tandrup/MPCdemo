package dk.au.daimi.tandrup.MPC.threading;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UnboundedThreadPool extends ThreadPoolExecutor {
	private Collection<WeakReference<BoundedSharedThreadPool>> listenerRefs = new HashSet<WeakReference<BoundedSharedThreadPool>>();

	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = lock.readLock();
	private final Lock write = lock.writeLock();

	private AtomicBoolean monitorRunning = new AtomicBoolean(false);
	
	public UnboundedThreadPool() {
		super(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
	}

	protected void afterExecute(Runnable r, Throwable t) {
		read.lock();
		try {
			for (WeakReference<BoundedSharedThreadPool> listenerRef : listenerRefs) {
				try {
					BoundedSharedThreadPool listener = listenerRef.get();
					if (listener != null)
						listener.executionCompleted();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		} finally {
			read.unlock();
		}
		
		// Start monitor thread for monitoring references
		if (!monitorRunning.getAndSet(true))
			(new MonitorThread()).start();
	}

	void addListener(BoundedSharedThreadPool listener) {
		write.lock();
		try {
			listenerRefs.add(new WeakReference<BoundedSharedThreadPool>(listener));
		} finally {
			write.unlock();
		}
	}

	void removeListener(BoundedSharedThreadPool listener) {
		WeakReference<BoundedSharedThreadPool> listenerRef = null;

		read.lock();
		try {
			for (WeakReference<BoundedSharedThreadPool> ref : listenerRefs) {
				if (ref.get() == listener) {
					listenerRef = ref;
					break;
				}
			}
		} finally {
			read.unlock();
		}

		if (listenerRef != null) {
			write.lock();
			try {
				listenerRefs.remove(listenerRef);
			} finally {
				write.unlock();
			}
		}
	}

	// Monitor for cleaning up dead references, every 10 second
	private class MonitorThread extends Thread {
		public MonitorThread() {
			super("Unbounded-MonitorThread");
		}
		
		public void run() {
			monitorRunning.set(true);
			while (!isTerminated()) {
				try {
					Thread.sleep(10000);

					Collection<WeakReference<BoundedSharedThreadPool>> deadRefs = new ArrayList<WeakReference<BoundedSharedThreadPool>>();

					read.lock();
					try {
						for (WeakReference<BoundedSharedThreadPool> listenerRef : listenerRefs) {
							if (listenerRef.get() == null)
								deadRefs.add(listenerRef);
						}
					} finally {
						read.unlock();
					}

					if (deadRefs.size() > 0) {
						// Remove dead references from set
						write.lock();
						try {
							for (WeakReference<BoundedSharedThreadPool> reference : deadRefs) {
								listenerRefs.remove(reference);
							}
						} finally {
							write.unlock();
						}
					}

					// If no active threads quit monitoring
					if (getActiveCount() == 0)
						break;

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			monitorRunning.set(false);
		}
	}
}
