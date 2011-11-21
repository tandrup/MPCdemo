package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;

import dk.au.daimi.tandrup.MPC.math.BerlekampWelch;
import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.Lagrange;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.exceptions.CorruptParticipantException;
import dk.au.daimi.tandrup.MPC.protocols.exceptions.OpenContributionException;
import dk.au.daimi.tandrup.MPC.protocols.messages.OpenResult;

public class Open extends AbstractProtocol {
	private final Participant pKing;
	private final int d;
	private final FieldElement xi;

	public Open(CommunicationChannel channel, int threshold, Activity activity, Participant pKing, int d, FieldElement xi) {
		super(channel, threshold, activity);
		this.pKing = pKing;
		this.d = d;
		this.xi = xi;
	}

	public FieldElement run() throws IOException, GeneralSecurityException, ClassNotFoundException {
		Activity inputActivity = activity.subActivity("Input");
		Activity resultActivity = activity.subActivity("Result");

		if (channel.localParticipant().equals(pKing)) {
			Collection<? extends Participant> participants = channel.listConnectedParticipants();
			FieldElement shares[] = new FieldElement[participants.size()];
			FieldElement shareIDs[] = new FieldElement[participants.size()];

			int i = 0;
			for (Participant participant : participants) {
				FieldElement share;
				if (participant.equals(pKing)) {
					share = xi;
				} else {
					IChannelData chData = channel.receive(inputActivity, participant);
					Object chObject = chData.getObject();
					if (!(chObject instanceof FieldElement))
						throw new CorruptParticipantException(participant);
					share = (FieldElement)chObject;
				}
				shares[i] = share;
				shareIDs[i] = share.field().element(participant.getID());
				i++;
			}

			FieldPolynomial1D poly;

			poly = Lagrange.interpolate(shareIDs, shares);

			if (poly.degree() > d) {
				if (d > threshold) {
					channel.broadcast(resultActivity, new OpenResult());			
					throw new OpenContributionException(pKing, shareIDs, shares);
				} else {
					poly = BerlekampWelch.interpolate(shareIDs, shares, d);
				}
			}

			FieldElement result = poly.coefficient(0);

			channel.broadcast(resultActivity, new OpenResult(result));

			return result;
		} else {
			channel.send(pKing, inputActivity, xi);

			IChannelData chData = channel.receive(resultActivity, pKing);
			
			Object chObject = chData.getObject();
			if (!(chObject instanceof OpenResult))
				throw new CorruptParticipantException(pKing);
			
			OpenResult msg = (OpenResult)chObject;

			if (msg.isError())
				if (d > threshold) {
					throw new OpenContributionException(pKing);
				} else {
					logger.warning("pKing is corrupt. " + pKing + " send contribution exception, but he should be able to reconstruct");
					return xi.field().zero();
				}
			else
				return msg.getResult();
		}
	}
}
