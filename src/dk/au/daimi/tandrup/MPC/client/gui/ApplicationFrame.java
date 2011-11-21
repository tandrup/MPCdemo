package dk.au.daimi.tandrup.MPC.client.gui;

import java.awt.Dimension;

import javax.swing.JFrame;

public class ApplicationFrame extends JFrame {
	static final long serialVersionUID = 1;
	private ApplicationPanel appPanel;
	
	public ApplicationFrame() {
		super();
		init();
	}
	private void init() {
		// Components
		appPanel = new ApplicationPanel();
		setContentPane(appPanel);
		
		setTitle("Secure MPC Client");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(900,500));
		pack();
		setVisible(true);
	}
	
	public ApplicationPanel getApplicationPanel() {
		return appPanel;
	}
}
