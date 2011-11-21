
package dk.au.daimi.tandrup.MPC.demo.gui;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JViewport;

import att.grappa.GrappaPanel;

import java.awt.Rectangle;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.JButton;

import javax.swing.JLabel;
import javax.swing.JComboBox;

public class CircuitBuilder extends JFrame {
	private static final long serialVersionUID = 1L;

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private JPanel jContentPane = null;

	private GrappaPanel grappaPanel = null;

	private JScrollPane graphScrollPane = null;

	private JLabel addGateLabel = null;

	private JButton validateButton = null;

	private JButton addRandomButton = null;

	private JButton addMultButton = null;

	private JButton addAffineButton = null;

	private JButton addInputButton = null;

	private JButton addOutputButton = null;

	private JLabel selectedGateLabel = null;
	
	private JLabel inputProviderLabel = null;

	private JComboBox inputProviderComboBox = null;

	private JButton broadcastCircuitButton = null;

	private JButton receiveCircuitButton = null;

	private JButton deleteButton = null;

	private CircuitBuilderModel model;

	/**
	 * This is the default constructor
	 */
	public CircuitBuilder(CircuitBuilderModel model) {
		super();
		this.model = model;
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(683, 516);
		this.setContentPane(getJContentPane());
		this.setTitle("Circuit Builder");
		this.setResizable(false);
		logger.config("Initialized Circuit Builder");
	}

	private void switchMode(boolean val) {
		addRandomButton.setEnabled(val);
		addInputButton.setEnabled(val);
		addOutputButton.setEnabled(val);
		addAffineButton.setEnabled(val);
		addMultButton.setEnabled(val);
		deleteButton.setEnabled(val);
	}
	
	public void preprocessMode() {
		switchMode(true);
	}

	public void evaluationMode() {
		switchMode(false);
	}
	
	/**
	 * This method initializes addRandomButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getAddRandomButton() {
		if (addRandomButton == null) {
			addRandomButton = new JButton();
			addRandomButton.setBounds(new Rectangle(10, 30, 160, 29));
			addRandomButton.setText("Random gate");
			addRandomButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					model.addRandomGate();
					model.updateGraph();
				}
			});
		}
		return addRandomButton;
	}

	/**
	 * This method initializes addAffineButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getAddAffineButton() {
		if (addAffineButton == null) {
			addAffineButton = new JButton();
			addAffineButton.setBounds(new Rectangle(10, 60, 160, 29));
			addAffineButton.setText("Addition gate");
			addAffineButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					model.addAffineGate();
					model.updateGraph();
				}
			});
		}
		return addAffineButton;
	}

	/**
	 * This method initializes addMultButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getAddMultButton() {
		if (addMultButton == null) {
			addMultButton = new JButton();
			addMultButton.setBounds(new Rectangle(10, 90, 160, 29));
			addMultButton.setText("Multiplication gate");
			addMultButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					model.addMultGate();
					model.updateGraph();
				}
			});
		}
		return addMultButton;
	}

	/**
	 * This method initializes addInputButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getAddInputButton() {
		if (addInputButton == null) {
			addInputButton = new JButton();
			addInputButton.setBounds(new Rectangle(10, 120, 160, 29));
			addInputButton.setText("Input gate");
			addInputButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					model.addInputGate();
					model.updateGraph();
				}
			});
		}
		return addInputButton;
	}

	/**
	 * This method initializes addOutputButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getAddOutputButton() {
		if (addOutputButton == null) {
			addOutputButton = new JButton();
			addOutputButton.setBounds(new Rectangle(10, 150, 160, 29));
			addOutputButton.setText("Output gate");
			addOutputButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					model.addOutputGate();
					model.updateGraph();
				}
			});
		}
		return addOutputButton;
	}

	/**
	 * This method initializes deleteButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getDeleteButton() {
		if (deleteButton == null) {
			deleteButton = new JButton();
			deleteButton.setBounds(new Rectangle(10, 200, 160, 29));
			deleteButton.setText("Delete");
			deleteButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					model.deleteSelectedGate();
				}
			});
		}
		return deleteButton;
	}

	/**
	 * This method initializes graphScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getGraphScrollPane() {
		if (graphScrollPane == null) {
			graphScrollPane = new JScrollPane(getGrappaPanel(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			graphScrollPane.setBounds(new Rectangle(180, 10, 495, 477));
			graphScrollPane.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
		}
		return graphScrollPane;
	}

	/**
	 * This method initializes grappaPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private GrappaPanel getGrappaPanel() {
		if (grappaPanel == null) {
			grappaPanel = new GrappaPanel(model.getGraph());
			grappaPanel.setBounds(new Rectangle(45, 47, 347, 152));
			grappaPanel.addGrappaListener(model.getGrappaListener(getDeleteButton(), getInputProviderComboBox()));
			model.getGraph().repaint();
		}
		return grappaPanel;
	}

	/**
	 * This method initializes inputProviderComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getInputProviderComboBox() {
		if (inputProviderComboBox == null) {
			inputProviderComboBox = new JComboBox();
			inputProviderComboBox.setBounds(new Rectangle(10, 250, 160, 27));
			inputProviderComboBox.setEnabled(false);

			inputProviderComboBox.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					if (inputProviderComboBox.getSelectedItem() != null) {
						String dn = (String)inputProviderComboBox.getSelectedItem();
						model.changeInputProvider(dn);
					}
				}
			});
		}
		return inputProviderComboBox;
	}

	/**
	 * This method initializes broadcastCircuitButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBroadcastCircuitButton() {
		if (broadcastCircuitButton == null) {
			broadcastCircuitButton = new JButton();
			broadcastCircuitButton.setBounds(new Rectangle(10, 390, 160, 29));
			broadcastCircuitButton.setText("Broadcast circuit");
			broadcastCircuitButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					model.broadcast();
				}
			});
		}
		return broadcastCircuitButton;
	}

	/**
	 * This method initializes receiveCircuitButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getReceiveCircuitButton() {
		if (receiveCircuitButton == null) {
			receiveCircuitButton = new JButton();
			receiveCircuitButton.setBounds(new Rectangle(10, 420, 160, 29));
			receiveCircuitButton.setText("Receive circuit");
			receiveCircuitButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					model.receive();
				}
			});
		}
		return receiveCircuitButton;
	}

	/**
	 * This method initializes ValidateButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getValidateButton() {
		if (validateButton == null) {
			validateButton = new JButton();
			validateButton.setBounds(new Rectangle(10, 450, 160, 29));
			validateButton.setText("Validate");
			validateButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					model.validateCircuit();
					model.redrawCircuit();
				}
			});
		}
		return validateButton;
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			addGateLabel = new JLabel();
			addGateLabel.setBounds(new Rectangle(10, 10, 160, 16));
			addGateLabel.setText("Add gate:");
			
			selectedGateLabel = new JLabel();
			selectedGateLabel.setBounds(new Rectangle(10, 185, 160, 16));
			selectedGateLabel.setText("Selected gate:");

			inputProviderLabel = new JLabel();
			inputProviderLabel.setBounds(new Rectangle(10, 235, 160, 16));
			inputProviderLabel.setText("Input provider:");
			
			jContentPane = new JPanel();
			jContentPane.setLayout(null);
			jContentPane.add(getGraphScrollPane(), null);
			jContentPane.add(addGateLabel, null);
			jContentPane.add(getAddRandomButton(), null);
			jContentPane.add(getAddAffineButton(), null);
			jContentPane.add(getAddMultButton(), null);
			jContentPane.add(getAddInputButton(), null);
			jContentPane.add(getAddOutputButton(), null);
			jContentPane.add(selectedGateLabel, null);
			jContentPane.add(getDeleteButton(), null);
			jContentPane.add(inputProviderLabel, null);
			jContentPane.add(getInputProviderComboBox(), null);
			jContentPane.add(getBroadcastCircuitButton(), null);
			jContentPane.add(getReceiveCircuitButton(), null);
			jContentPane.add(getValidateButton(), null);
		}
		return jContentPane;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
