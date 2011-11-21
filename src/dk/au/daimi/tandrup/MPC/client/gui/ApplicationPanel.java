package dk.au.daimi.tandrup.MPC.client.gui;

import javax.swing.JPanel;

public class ApplicationPanel extends JPanel {
	static final long serialVersionUID = 1;
	private StartupPanel startupPanel;
	
	public ApplicationPanel() {
		super();
		startupPanel = new StartupPanel();
		this.add(startupPanel);
	}
}
