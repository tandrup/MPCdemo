package dk.au.daimi.tandrup.MPC.net.ssl;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.Participant;

public class LinkedListMessageQueue implements SignedMessageQueue {
	private final Logger logger = Logger.getLogger(this.getClass().getName());
	
	private Queue<SignedMessage> queue = new LinkedList<SignedMessage>();

	private void logMessage(String direction, SignedMessage msg) {
		/*
		try {
			logger.info(direction + " message (" + msg.getActivity() + ") " + msg.getObject() + " from " + msg.getSender());
		} catch (Exception e) {
			logger.warning(direction + " message corrupt");
		}*/
	}

	public synchronized int size() {
		return queue.size();
	}

	public synchronized void dumpQueue() {
		System.out.println("Queue: ");
		for (SignedMessage sMsg : queue) {
			System.out.println(sMsg + " from " + sMsg.getSender());
		}
	}
	
	public synchronized void add(SignedMessage msg) {
		logMessage("Added", msg);
		queue.offer(msg);
		notifyAll();
	}

	public SignedMessage receive() {
		return receive(new AllFilter());
	}

	public SignedMessage receive(long timeout) {
		return receive(new AllFilter(), timeout);
	}

	public SignedMessage receive(Participant from) {
		return receive(new ParticipantFilter(from));
	}

	public SignedMessage receive(Participant from, long timeout) {
		return receive(new ParticipantFilter(from), timeout);
	}

	public SignedMessage receive(Activity activity) {
		return receive(new ActivityFilter(activity));
	}

	public SignedMessage receive(Activity activity, long timeout) {
		return receive(new ActivityFilter(activity), timeout);
	}

	public SignedMessage receive(Participant from, Activity activity) {
		return receive(new CombinedFilter(activity, from));
	}

	public SignedMessage receive(Participant from, Activity activity, long timeout) {
		return receive(new CombinedFilter(activity, from), timeout);
	}

	protected synchronized SignedMessage receive(Filter<SignedMessage> filter) {
		try {
			while (true) {
				for (SignedMessage sMsg : queue) {
					if (filter.isAcceptable(sMsg)) {
						queue.remove(sMsg);
						logMessage("Received", sMsg);
						return sMsg;
					}
				}
				wait();
			}
		} catch (InterruptedException ex) {
			logger.warning(ex.toString());
			return null;
		}
	}

	protected synchronized SignedMessage receive(Filter<SignedMessage> filter, long timeout) {
		if (timeout <= 0)
			return receive(filter);
		
		try {
			long startTime = System.currentTimeMillis();
			while (System.currentTimeMillis() - startTime < timeout) {
				for (SignedMessage sMsg : queue) {
					if (filter.isAcceptable(sMsg)) {
						queue.remove(sMsg);
						logMessage("Received", sMsg);
						return sMsg;
					}
				}
				wait(timeout);
			}
			logger.warning("Timeout occured. Filter " + filter + ". Timeout: " + timeout);			
			return null;
		} catch (InterruptedException ex) {
			logger.warning(ex.toString());
			return null;
		}
	}

	private interface Filter<T> {
		public boolean isAcceptable(T val);
	}
	
	private class ActivityFilter implements Filter<SignedMessage>  {
		private Activity activity;
		
		public ActivityFilter(Activity activity) {
			super();
			this.activity = activity;
		}

		public boolean isAcceptable(SignedMessage val) {
			try {
				return activity.equals(val.getActivity());
			} catch (Exception e) {
				return false;
			}
		}
		
		@Override
		public String toString() {
			return "ActivityFilter(" + activity + ")";
		}
	}

	private class AllFilter implements Filter<SignedMessage>  {
		public boolean isAcceptable(SignedMessage val) {
			return true;
		}

		@Override
		public String toString() {
			return "AllFilter";
		}
	}
	
	private class ParticipantFilter implements Filter<SignedMessage>  {
		private Participant from;
		
		public ParticipantFilter(Participant from) {
			super();
			this.from = from;
		}

		public boolean isAcceptable(SignedMessage val) {
			try {
				return val.getSender().equals(from);
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public String toString() {
			return "ParticipantFilter(" + from + ")";
		}
	}
	
	private class CombinedFilter implements Filter<SignedMessage> {
		private ActivityFilter activityFilter;
		private ParticipantFilter partFilter;
		
		public CombinedFilter(Activity activity, Participant from) {
			super();
			this.activityFilter = new ActivityFilter(activity);
			this.partFilter = new ParticipantFilter(from);
		}

		public boolean isAcceptable(SignedMessage val) {
			return activityFilter.isAcceptable(val) && partFilter.isAcceptable(val);
		}

		@Override
		public String toString() {
			return "CombinedFilter(" + activityFilter + ", " + partFilter + ")";
		}
	}
}
