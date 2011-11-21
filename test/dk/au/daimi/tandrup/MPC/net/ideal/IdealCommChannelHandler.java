package dk.au.daimi.tandrup.MPC.net.ideal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.net.messages.TransferMessage;
import dk.au.daimi.tandrup.MPC.net.ssl.Endpoint;
import dk.au.daimi.tandrup.MPC.net.ssl.SignedMessage;

public class IdealCommChannelHandler {
	private static final boolean DEBUG = false;

	private static final boolean MEASSURE_SIZE = false;

	private Map<Participant, IdealCommChannel> channels = Collections.synchronizedMap(new HashMap<Participant, IdealCommChannel>());

	private Object statSyncRoot = new Object();
	private long msgCounter = 0;
	private long byteCounter = 0;
	private Map<Class, LongPair> msgPerClass = new HashMap<Class, LongPair>();
	private Map<Activity, LongPair> msgPerActivity = new HashMap<Activity, LongPair>();

	public IdealCommChannel newChannel(Participant owner) {
		IdealCommChannel retVal = new IdealCommChannel(owner);
		channels.put(owner, retVal);
		return retVal;
	}

	public String getStatistics() {
		synchronized (statSyncRoot) {
			/*
			for (Map.Entry<Class, LongPair> entry : msgPerClass.entrySet()) {
				System.out.println(entry.getKey() + ":\t" + entry.getValue());
			}
			for (Map.Entry<Activity, LongPair> entry : msgPerActivity.entrySet()) {
				System.out.println(entry.getKey() + ":\t" + entry.getValue());
			}*/
			return msgCounter + "\t" + byteCounter;
		}
	}

	public void resetStatistics() {
		synchronized (statSyncRoot) {
			msgCounter = 0;
			byteCounter = 0;
			msgPerClass.clear();
			msgPerActivity.clear();
		} 
	}

	private void receivedMessage(IChannelData sMsg, int byteCount) {
		synchronized (statSyncRoot) {
			msgCounter++;
			byteCounter += byteCount;
			/*
			try {
				Class c = sMsg.getObject().getClass();
				LongPair count = msgPerClass.get(c);
				if (count == null) {
					msgPerClass.put(c, new LongPair(1, byteCount));
				} else {
					count.msgCount++;
					count.byteCount += byteCount;
				}

				Activity a = sMsg.getActivity().stripDetails(4);
//				Activity a = sMsg.getActivity().stripParents(1);
				count = msgPerActivity.get(a);
				if (count == null) {
					msgPerActivity.put(a, new LongPair(1, byteCount));
				} else {
					count.msgCount++;
					count.byteCount += byteCount;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			 */
		} 
	}

	public class IdealCommChannel implements CommunicationChannel {
		private Participant owner;
		private Queue<IChannelData> ingressData = new LinkedList<IChannelData>();

		private IdealCommChannel(Participant owner) {
			this.owner = owner;
		}

		private synchronized void input(IChannelData sMsg) {
			if (DEBUG) {
				try {
					System.out.println(sMsg.getSender().getID() + ">" + owner.getID() + ": input(" + sMsg.getActivity() + ") : " + sMsg.getObject());
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}

			Object obj = sMsg;
			int msgLen = 0;

			if (MEASSURE_SIZE) {
				try {
					ByteArrayOutputStream outByte = new ByteArrayOutputStream();
					ObjectOutputStream outObj = new ObjectOutputStream(outByte);
					outObj.writeObject(sMsg);
					outObj.close();

					byte[] encoding = outByte.toByteArray();

					msgLen = encoding.length;

					ByteArrayInputStream inByte = new ByteArrayInputStream(encoding);
					ObjectInputStream inObj = new ObjectInputStream(inByte);

					obj = inObj.readObject();

					inObj.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			// update stat.
			receivedMessage(sMsg, msgLen);

			// put message in queue
			ingressData.offer((IChannelData)obj);
			notifyAll();
		}

		public void broadcast(Activity activity, Serializable obj) throws IOException, GeneralSecurityException {
			IChannelData msg = new MsgWrapper(obj, owner, activity);
			for (IdealCommChannel ch : channels.values()) {
				if (ch != this)
					ch.input(msg);
			}
		}

		public void close() throws IOException, GeneralSecurityException {
			// TODO Auto-generated method stub			
		}

		public void init(Collection<Endpoint> serverParticipants, X509Certificate userCert, PrivateKey userKey, long timeout) throws IOException, GeneralSecurityException {
			// Do nothing
		}

		public synchronized Collection<? extends Participant> listConnectedParticipants() {
			return new HashSet<Participant>(channels.keySet());
		}

		public synchronized IChannelData receive(Activity activity) throws IOException, GeneralSecurityException, ClassNotFoundException {
			try {
				while (true) {
					for (IChannelData data : ingressData) {
						if (data.getActivity().equals(activity)) {
							return data;
						}
					}
					wait();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		public synchronized IChannelData receive(Activity activity, Participant participant) throws IOException, GeneralSecurityException, ClassNotFoundException {
			try {
				while (true) {
					for (IChannelData data : ingressData) {
						if (data.getSender().equals(participant) && data.getActivity().equals(activity)) {
							if (DEBUG) {
								try {
									System.out.println(data.getSender().getID() + ">" + owner.getID() + ": output(" + data.getActivity() + ") : " + data.getObject());
								} catch (ClassNotFoundException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							return data;
						}
					}
					wait(1000);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		public Collection<IChannelData> receiveFromEachParticipant(Activity activity) throws IOException, GeneralSecurityException, ClassNotFoundException {
			return receiveFromEachParticipant(activity, channels.keySet());
		}

		public Collection<IChannelData> receiveFromEachParticipant(Activity activity, Collection<? extends Participant> participants) throws IOException, GeneralSecurityException, ClassNotFoundException {
			Collection<IChannelData> retVal = new ArrayList<IChannelData>();

			for (Participant part : participants) {
				if (part.equals(owner))
					continue;

				retVal.add(receive(activity, part));

			}

			return retVal;
		}

		public void send(Participant recv, Activity activity, Serializable obj) throws IOException, GeneralSecurityException {
			channels.get(recv).input(new MsgWrapper(obj, owner, activity));
		}

		public void startSession() throws IOException, GeneralSecurityException {
			// TODO Auto-generated method stub
		}

		public void transfer(Participant recv, Activity activity, IChannelData msg) throws IOException, GeneralSecurityException {
			send(recv, activity, new TransferMessage((SignedMessage)msg));
		}

		public Participant localParticipant() {
			return owner;
		}
	}

	public Participant createNewParticipant(int index) {
		return new IdealParticipant(index);
	}
}

class MsgWrapper implements IChannelData, Serializable {
	private static final long serialVersionUID = 1L;

	private Serializable msg;
	private Participant sender;
	private Activity activity;

	MsgWrapper(Serializable msg, Participant sender, Activity activity) {
		if (msg == null)
			throw new IllegalArgumentException("msg is null");
		this.msg = msg;
		this.sender = sender;
		this.activity = activity;
	}

	public Serializable getObject() throws IOException, ClassNotFoundException {
		return msg;
	}

	public Participant getSender() {
		return sender;
	}

	public Activity getActivity() {
		return activity;
	}
}

class IdealParticipant implements Participant, Serializable {
	private static final long serialVersionUID = 1L;

	private int id;

	IdealParticipant(int id) {
		this.id = id;
	}

	public X509Certificate getCertificate() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getCertificateSubjectDN() {
		return "IdealCert" + id;
	}

	public InetAddress getInetAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getPort() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getID() {
		return id;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public String toString() {
		return "Part" + id;
	}

	@Override
	public boolean equals(Object arg0) {
		if (this == arg0)
			return true;

		if (arg0 instanceof Participant) {
			return equals((Participant)arg0);
//			return this.id == other.id;
		}
		return super.equals(arg0);
	}

	public int compareTo(Participant other) {
		return this.getCertificateSubjectDN().compareTo(other.getCertificateSubjectDN());
	}

	public boolean equals(Participant other) {
		return (this.getCertificateSubjectDN().equals(other.getCertificateSubjectDN()));
	}
}

class LongPair {
	long msgCount;
	long byteCount;

	public LongPair() {
		this.msgCount = 0;
		this.byteCount = 0;
	}

	public LongPair(long msgCount, long byteCount) {
		this.msgCount = msgCount;
		this.byteCount = byteCount;
	}

	@Override
	public String toString() {
		return msgCount + "\t" + byteCount;
	}
}