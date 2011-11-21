package dk.au.daimi.tandrup.MPC.demo.gui;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JButton;
import java.awt.Rectangle;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Formatter;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JComboBox;

import dk.au.daimi.tandrup.MPC.math.FieldPolynomial1D;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.math.fields.integer.LongField;
import dk.au.daimi.tandrup.MPC.math.fields.polynomial.PolynomialField;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.net.SecurityManager;
import dk.au.daimi.tandrup.MPC.net.ssl.SSLCommunicationChannel;
import dk.au.daimi.tandrup.MPC.protocols.Disputes;
import dk.au.daimi.tandrup.MPC.protocols.Evaluate;
import dk.au.daimi.tandrup.MPC.protocols.PreProcess;
import dk.au.daimi.tandrup.MPC.protocols.gates.InputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.LocalInputGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.OutputGate;

import javax.swing.JTextArea;
import javax.swing.table.TableColumn;

public class DemoClient extends JFrame {

	private static final long serialVersionUID = 1L;

	private JPanel jContentPane = null;

	private JButton startSessionButton = null;

	private JScrollPane tableScrollPane = null;

	private JTable endpointsTable = null;

	private JButton addNewEndpointButton = null;

	private JButton circBuilderButton = null;

	private JButton preProcessButton = null;

	private JButton evaluateButton = null;

	private JLabel keyStoreFileLabel = null;

	private JButton initChannelButton = null;

	private JLabel userCertificateLabel = null;

	private JComboBox userCertificateComboBox = null;

	private JLabel PortNoLabel = null;

	private JTextField portNoTextField = null;

	private JTextArea logTextArea = null;

	private CircuitBuilder circBuilder = null;
	private CircuitBuilderModel circBuilderModel = null;

	private EndpointsTableModel endpointsModel;
	private KeyStore store;
	private SSLCommunicationChannel channel;
	private Random random;
	private JTextAreaLogHandler logHandler = new JTextAreaLogHandler();  //  @jve:decl-index=0:
	protected final Logger logger = Logger.getLogger(this.getClass().getName());

	private Field fieldF = new LongField(67);
	private FieldPolynomial1D basePoly = new FieldPolynomial1D(new FieldElement[] {fieldF.element(6), fieldF.element(1), fieldF.element(0), fieldF.element(1)});
	private Field fieldG = new PolynomialField(basePoly);
	private Disputes disputes = new Disputes();

	private PreProcess preprocess;
	private Evaluate evaluate;

	private int threshold;

	private JComboBox keystoresComboBox = null;

	public Field getFieldF() {
		return fieldF;
	}

	public SSLCommunicationChannel getChannel() {
		return channel;
	}

	/**
	 * This method initializes cmdStartSession	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getStartSessionButton() {
		if (startSessionButton == null) {
			startSessionButton = new JButton();
			startSessionButton.setBounds(new Rectangle(10, 165, 151, 26));
			startSessionButton.setText("Start session");
			startSessionButton.setEnabled(false);
			startSessionButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					Thread thread = new Thread() {
						public void run() {
							try {
								logger.info("Starting session");
								startSessionButton.setEnabled(false);
								
								channel.startSession();

								logger.info("Session started");

								circBuilderButton.setEnabled(true);
								
							} catch (Exception e) {
								logger.throwing(this.getClass().getName(), "run", e);
							}
						}
					};
					thread.start();
				}
			});
		}
		return startSessionButton;
	}

	/**
	 * This method initializes tableScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getTableScrollPane() {
		if (tableScrollPane == null) {
			tableScrollPane = new JScrollPane();
			tableScrollPane.setBounds(new Rectangle(174, 103, 457, 181));
			tableScrollPane.setViewportView(getEndpointsTable());
		}
		return tableScrollPane;
	}

	/**
	 * This method initializes endpointsTable	
	 * 	
	 * @return javax.swing.JTable	
	 */
	private JTable getEndpointsTable() {
		if (endpointsTable == null) {
			endpointsTable = new JTable();
			endpointsModel = new EndpointsTableModel();
			endpointsTable.setModel(endpointsModel);
		}
		return endpointsTable;
	}

	/**
	 * This method initializes addNewEndpointButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getAddNewEndpointButton() {
		if (addNewEndpointButton == null) {
			addNewEndpointButton = new JButton();
			addNewEndpointButton.setBounds(new Rectangle(10, 105, 151, 26));
			addNewEndpointButton.setText("Add new");
			addNewEndpointButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					endpointsModel.addNewEmpty();
					updateEndpointCerts();
					initChannelButton.setEnabled(true);
				}
			});
		}
		return addNewEndpointButton;
	}


	/**
	 * This method initializes circBuilderButton
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getCircBuilderButton() {
		if (circBuilderButton == null) {
			circBuilderButton = new JButton();
			circBuilderButton.setBounds(new Rectangle(10, 195, 151, 26));
			circBuilderButton.setText("Circuit builder");
			circBuilderButton.setEnabled(false);
			circBuilderButton.setEnabled(true);
			circBuilderButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					circBuilder.setVisible(true);
					preProcessButton.setEnabled(true);
				}
			});
		}
		return circBuilderButton;
	}

	/**
	 * This method initializes PreProcessButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getPreProcessButton() {
		if (preProcessButton == null) {
			preProcessButton = new JButton();
			preProcessButton.setBounds(new Rectangle(10, 225, 151, 26));
			preProcessButton.setText("Preprocess");
			preProcessButton.setEnabled(false);
			preProcessButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					Thread thread = new Thread() {
						public void run() {
							try {
								logger.info("Starting preprocessing");
								preProcessButton.setEnabled(false);
								circBuilderButton.setEnabled(false);
								
								int rCount = circBuilderModel.getRandomCount();
								int mCount = circBuilderModel.getMultCount();
								int aCount = circBuilderModel.getAffineCount();
								int oCount = circBuilderModel.getOutputCount();
								Participant[] inputProviders = circBuilderModel.getInputProviders();
								
								preprocess = new PreProcess(random, channel, threshold, new Activity("Evaluate"), rCount, inputProviders, mCount, aCount, oCount, fieldF, fieldG, disputes);
								preprocess.run();

								logger.info("Preprocessing done");
								evaluateButton.setEnabled(true);
								circBuilder.evaluationMode();

							} catch (Exception e) {
								logger.throwing(this.getClass().getName(), "run", e);
							}
						}
					};
					thread.start();
				}
			});
		}
		return preProcessButton;
	}

	/**
	 * This method initializes EvaluateButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getEvaluateButton() {
		if (evaluateButton == null) {
			evaluateButton = new JButton();
			evaluateButton.setBounds(new Rectangle(10, 255, 151, 26));
			evaluateButton.setText("Evaluate");
			evaluateButton.setEnabled(false);
			evaluateButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					Thread thread = new Thread() {
						public void run() {
							try {
								logger.info("Starting evaluation");
								evaluateButton.setEnabled(false);
								
								evaluate = new Evaluate(channel, threshold, new Activity("Eval"));

								circBuilderModel.configureResult(preprocess.getResult());

								for (int i = 0; i < preprocess.getResult().getInputGateCount(); i++) {
									InputGate ig = preprocess.getResult().getInputGate(i);
									if (ig instanceof LocalInputGate) {
										LocalInputGate lig = (LocalInputGate)ig;
										String s = (String)JOptionPane.showInputDialog(jContentPane, "Please enter input no. " + lig.getID(), "Provide input", JOptionPane.QUESTION_MESSAGE);
										lig.setInput(fieldF.element(Integer.parseInt(s)));
									}
								}
								
								Collection<OutputGate> gates = new ArrayList<OutputGate>();
								for (int i = 0; i < preprocess.getResult().getOutputGateCount(); i++) {
									OutputGate gate = preprocess.getResult().getOutputGate(i);
									if (gate.isConfigured()) 
										gates.add(gate);
								}

								evaluate.evaluate(gates);

								for (OutputGate gate : gates) {
									logger.info("Output " + gate.getID() + " = " + gate.output());
								}

								logger.info("Evaluation done");
								preProcessButton.setEnabled(true);
								circBuilderButton.setEnabled(true);
								circBuilder.preprocessMode();
								
							} catch (Exception e) {
								logger.throwing(this.getClass().getName(), "run", e);
							}
						}
					};
					thread.start();
				}
			});
		}
		return evaluateButton;
	}

	/**
	 * This method initializes InitChannelButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getInitChannelButton() {
		if (initChannelButton == null) {
			initChannelButton = new JButton();
			initChannelButton.setBounds(new Rectangle(10, 135, 151, 26));
			initChannelButton.setText("Initialize channel");
			initChannelButton.setEnabled(false);
			initChannelButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					Thread thread = new Thread() {
						public void run() {
							try {
								addNewEndpointButton.setEnabled(false);
								initChannelButton.setEnabled(false);
								
								X509Certificate cert = null;
								PrivateKey key = null;

								String selectedSubjectDN = (String)userCertificateComboBox.getSelectedItem();

								Enumeration<String> aliases = store.aliases();
								while (aliases.hasMoreElements()) {
									String alias = aliases.nextElement();
									if (store.isKeyEntry(alias)) {
										cert = (X509Certificate)store.getCertificate(alias);
										if (cert.getSubjectX500Principal().getName().equals(selectedSubjectDN))
											key = (PrivateKey)store.getKey(alias, "secret".toCharArray());
									}
								}

								random = SecureRandom.getInstance("SHA1PRNG");

								int portNo = Integer.parseInt(portNoTextField.getText());

								channel = new SSLCommunicationChannel(portNo, store, random);

								channel.init(endpointsModel.getEndpoints(), cert, key, 10000);

								logger.info("Channel initialized");

								startSessionButton.setEnabled(true);

							} catch (Exception ex) {
								logger.throwing(this.getClass().getName(), "run", ex);
							}
						}
					};
					thread.start();
				}
			});
		}
		return initChannelButton;
	}

	/**
	 * This method initializes userCertificateComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getUserCertificateComboBox() {
		if (userCertificateComboBox == null) {
			userCertificateComboBox = new JComboBox();//new CertificatesComboBoxModel());
			userCertificateComboBox.setBounds(new Rectangle(170, 42, 459, 27));
			userCertificateComboBox.setEditable(false);
		}
		return userCertificateComboBox;
	}

	/**
	 * This method initializes portNoTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getPortNoTextField() {
		if (portNoTextField == null) {
			portNoTextField = new JTextField();
			portNoTextField.setBounds(new Rectangle(170, 75, 270, 20));
			portNoTextField.setText("8001");
		}
		return portNoTextField;
	}

	/**
	 * This method initializes logTextArea	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getLogTextArea() {
		if (logTextArea == null) {
			logTextArea = new JTextArea("Log:\n");
			logTextArea.setBounds(new Rectangle(10, 289, 622, 279));
			logHandler.setTextArea(logTextArea);
		}
		return logTextArea;
	}

	private void updateEndpointCerts() {
		try {
			TableColumn certColumn = endpointsTable.getColumnModel().getColumn(2);
			JComboBox comboBox = new JComboBox();
			comboBox.setEditable(true);
			Enumeration<String> aliases = store.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				if (store.isCertificateEntry(alias)) {
					X509Certificate cert = (X509Certificate)store.getCertificate(alias);
					comboBox.addItem(cert.getSubjectX500Principal().getName());
				}
			}

			certColumn.setCellEditor(new DefaultCellEditor(comboBox));
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.throwing(this.getClass().getName(), "updateEndpointCerts", ex);
		}
	}

	/**
	 * This method initializes keystoresComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getKeystoresComboBox() {
		if (keystoresComboBox == null) {
			keystoresComboBox = new JComboBox();
			keystoresComboBox.setBounds(new Rectangle(170, 10, 459, 27));
			keystoresComboBox.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					if (keystoresComboBox.getSelectedItem() != null) {
						try {
							store = SecurityManager.getJavaKeyStoreFromFile((String)keystoresComboBox.getSelectedItem());

							DefaultComboBoxModel model = (DefaultComboBoxModel)userCertificateComboBox.getModel();
							model.removeAllElements();

							Enumeration<String> aliases = store.aliases();
							while (aliases.hasMoreElements()) {
								String alias = aliases.nextElement();
								if (store.isKeyEntry(alias)) {
									X509Certificate cert = (X509Certificate)store.getCertificate(alias);
									model.addElement(cert.getSubjectX500Principal().getName());
								}
							}

							updateEndpointCerts();
						} catch (Exception ex) {
							ex.printStackTrace();
							logger.throwing(this.getClass().getName(), "itemStateChanged", ex);
						}
					}
				}
			});
			keystoresComboBox.addItem("server1.store");
			keystoresComboBox.addItem("server2.store");
			keystoresComboBox.addItem("server3.store");
			keystoresComboBox.addItem("server4.store");
			keystoresComboBox.addItem("server5.store");
			keystoresComboBox.addItem("server6.store");
		}
		return keystoresComboBox;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				DemoClient thisClass = new DemoClient();
				thisClass.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				thisClass.setVisible(true);
			}
		});
	}

	/**
	 * This is the default constructor
	 */
	public DemoClient() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(644, 600);
		this.setResizable(false);
		this.setContentPane(getJContentPane());
		try {
			this.setTitle("MPC Demo App @ " + InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			this.setTitle("MPC Demo App @ UNKNOWN");
		}

		circBuilderModel = new CircuitBuilderModel(this);
		
		circBuilder = new CircuitBuilder(circBuilderModel);
		circBuilder.setVisible(false);

		Formatter formatter = Logger.getLogger("").getHandlers()[0].getFormatter();

		logHandler.setFormatter(formatter);
		Logger.getLogger("").addHandler(logHandler);

		Handler[] handlers = Logger.getLogger("").getHandlers();
		for ( int index = 0; index < handlers.length; index++ ) {
			handlers[index].setLevel( Level.FINEST );
		}

		Logger.getLogger("dk").setLevel(Level.FINE);
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			PortNoLabel = new JLabel();
			PortNoLabel.setBounds(new Rectangle(10, 75, 151, 19));
			PortNoLabel.setText("My port no.:");
			userCertificateLabel = new JLabel();
			userCertificateLabel.setBounds(new Rectangle(10, 45, 153, 21));
			userCertificateLabel.setText("My Certificate:");
			keyStoreFileLabel = new JLabel();
			keyStoreFileLabel.setBounds(new Rectangle(10, 15, 152, 21));
			keyStoreFileLabel.setText("Java Keystore location:");
			jContentPane = new JPanel();
			jContentPane.setLayout(null);
			jContentPane.add(getStartSessionButton(), null);
			jContentPane.add(getTableScrollPane(), null);
			jContentPane.add(getAddNewEndpointButton(), null);
			jContentPane.add(getCircBuilderButton(), null);
			jContentPane.add(getPreProcessButton(), null);
			jContentPane.add(getEvaluateButton(), null);
			jContentPane.add(keyStoreFileLabel, null);
			jContentPane.add(getInitChannelButton(), null);
			jContentPane.add(userCertificateLabel, null);
			jContentPane.add(getUserCertificateComboBox(), null);
			jContentPane.add(PortNoLabel, null);
			jContentPane.add(getPortNoTextField(), null);
			jContentPane.add(getLogTextArea(), null);
			jContentPane.add(getKeystoresComboBox(), null);
		}
		return jContentPane;
	}
	
}  //  @jve:decl-index=0:visual-constraint="5,8"



