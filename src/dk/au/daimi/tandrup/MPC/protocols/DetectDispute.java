package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.messages.DisputeMessage;

public class DetectDispute extends AbstractProtocol {
	private int shareCount;
	private Disputes disputes;
	private Corruption corruption;
	private DisputeMessage myDisputeMessage;
	private Map<Participant, DisputeMessage> disputeMessages;

	public DetectDispute(CommunicationChannel channel, int threshold, Activity activity, int l, Disputes disputes, Corruption corruption, DisputeMessage myDisputeMessage) {
		super(channel, threshold, activity);
		this.disputes = disputes;
		this.corruption = corruption;
		this.myDisputeMessage = myDisputeMessage;
		
		int n = channel.listConnectedParticipants().size();
		shareCount = (int)Math.ceil((double)l / (n - threshold));
	}

	public void run() throws IOException, GeneralSecurityException, ClassNotFoundException {
		disputeMessages = new HashMap<Participant, DisputeMessage>();

		channel.broadcast(activity, myDisputeMessage);
		disputeMessages.put(channel.localParticipant(), myDisputeMessage);
		
		Collection<IChannelData> chDatas = channel.receiveFromEachParticipant(activity);
		for (IChannelData data : chDatas) {
			Object obj = data.getObject();
			if (obj instanceof DisputeMessage)
				disputeMessages.put(data.getSender(), (DisputeMessage)obj);
			else 
				corruption.add(data.getSender());
		}
		
		// Step 2
		if (step2())
			return;
		
		// Step 3
		if (step3())
			return;

		step4();
	}
	
	private boolean step2() {
		boolean corrupted = false;
		
		for (Participant part : corruption.getHonest(channel.listConnectedParticipants())) {
			DisputeMessage dMsg = disputeMessages.get(part);

			for (int i = 0; i < shareCount; i++) {
				FieldPolynomial1D poly = dMsg.getMyShares(i);
				if (poly.degree() > threshold) {
					corrupted = true;
					corruption.add(part);
					break;
				}
			}
		}
		
		return corrupted;
	}

	private boolean step3() {
		boolean corrupted = false;
		
		for (Participant pi : corruption.getHonest(channel.listConnectedParticipants())) {
			for (Participant pj : corruption.getHonest(channel.listConnectedParticipants())) {
				DisputeMessage di = disputeMessages.get(pi);
				DisputeMessage dj = disputeMessages.get(pj);

				for (int i = 0; i < shareCount; i++) {
					FieldElement si = di.getShare(i, pi, pj);
					FieldElement sj = dj.getShare(i, pi, pj);
					if (!si.equals(sj)) {
						corrupted = true;
						disputes.add(pi, pj);
						break;
					}
				}
			}
		}
		
		return corrupted;
	}

	private void step4() {
		//FIXME
	}
}
