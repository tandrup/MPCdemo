package dk.au.daimi.tandrup.MPC.protocols;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.FieldPolynomial2D;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.CommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.messages.VssCoordinateRequest;
import dk.au.daimi.tandrup.MPC.protocols.messages.VssShare;
import dk.au.daimi.tandrup.MPC.protocols.messages.VssVerification;

public class Vss extends AbstractRandomProtocol {
	private Field field;
	private Participant dealer;
	private Collection<? extends Participant> participants;
	
	public Vss(Activity activity, Random random, CommunicationChannel channel, int threshold, Field field, Participant dealer, Collection<? extends Participant> participants) {
		super(random, channel, threshold, activity);
		this.field = field;
		this.dealer = dealer;
		this.participants = participants;
	}

	public Participant getDealer() {
		return dealer;
	}
	
	public FieldElement share(FieldElement secret) throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException {
		FieldPolynomial2D poly2D = new FieldPolynomial2D(secret, threshold, random);
		
		for (Participant part : participants) {
			FieldPolynomial1D fi = poly2D.evalPartial2(field.element(part.getID()));
			FieldPolynomial1D gi = poly2D.evalPartial1(field.element(part.getID()));
			channel.send(part, activity.subActivity("Share"), new VssShare(fi, gi));
		}
		
		return receive();
	}
	
	public FieldElement receive() throws IOException, GeneralSecurityException, ClassNotFoundException, InterruptedException {
		// Create list of other participants
		Collection<Participant> others = new ArrayList<Participant>(participants);
		others.remove(channel.localParticipant());
		
		// Receive polynomials from dealer
		IChannelData shareData = channel.receive(activity.subActivity("Share"), dealer);
		
		VssShare share = (VssShare)shareData.getObject(); //FIXME Handle wrong type
		
		// Send verification messages to all
		for (Participant part : others) {
			FieldElement sij = share.f.eval(field.element(part.getID()));
			channel.send(part, activity.subActivity("Verify"), new VssVerification(sij));
		}
		
		// Receive verification messages from all
		Collection<IChannelData> verificationDatas = channel.receiveFromEachParticipant(activity.subActivity("Verify"), others);
		
		Set<VssCoordinate> invalidValues = new HashSet<VssCoordinate>();
		
		// Verify received data
		for (IChannelData verificationData : verificationDatas) {
			VssVerification verification = (VssVerification)verificationData.getObject();
			FieldElement y = field.element(verificationData.getSender().getID());
			if (!verification.sij.equals(share.g.eval(y))) {
				invalidValues.add(new VssCoordinate(verificationData.getSender().getID(), channel.localParticipant().getID()));
			}
		}

		Activity errorActivity = activity.subActivity("Error Handling");

		// Do error correction
		VssCoordinateRequest myCoorRequest = new VssCoordinateRequest(invalidValues, invalidValues.size() > threshold);
		channel.broadcast(
				errorActivity, 
				myCoorRequest);

		// Request sets
		Set<VssCoordinate> vssCoordinateRequests = new HashSet<VssCoordinate>();
		Set<Integer> polynomialRequests = new HashSet<Integer>();
		
		// Add my local requests
		if (!myCoorRequest.getVssCoordinates().isEmpty()) {
			vssCoordinateRequests.addAll(myCoorRequest.getVssCoordinates());
			if (myCoorRequest.sendPolynomials()) 
				polynomialRequests.add(channel.localParticipant().getID());
		}
		
		// Receive error messages from all
		Collection<IChannelData> errorDatas = channel.receiveFromEachParticipant(errorActivity, others);
		
		// Verify received data
		for (IChannelData coorData : errorDatas) {
			VssCoordinateRequest coorRequest = (VssCoordinateRequest)coorData.getObject();
			if (!coorRequest.getVssCoordinates().isEmpty()) {
				vssCoordinateRequests.addAll(coorRequest.getVssCoordinates());
				if (coorRequest.sendPolynomials()) 
					polynomialRequests.add(coorData.getSender().getID());
			}
		}
		
		if (!vssCoordinateRequests.isEmpty()) {
			// FIXME Handle coordinate requests
			throw new IllegalStateException("Unhandled protocol error. Received coordinate requests");
		}
		
		return share.f.eval(field.zero());
	}
}
