package dk.au.daimi.tandrup.MPC.net;

import java.io.IOException;
import java.io.Serializable;

public interface IChannelData {
	public Serializable getObject() throws IOException, ClassNotFoundException;
	public Participant getSender();
	public Activity getActivity() throws IOException, ClassNotFoundException;
}
