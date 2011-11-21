package dk.au.daimi.tandrup.MPC.client.gui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class StartupPanel extends JPanel {
	static final long serialVersionUID = 1;

	public StartupPanel() {
		super();
		
		this.add(new JLabel("Welcome to Secure Servers Inc."));
		this.add(new JButton("Open"));
	}
}
