package dk.au.daimi.tandrup.MPC.demo.gui;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;

import att.grappa.Edge;
import att.grappa.Element;
import att.grappa.Graph;
import att.grappa.GraphEnumeration;
import att.grappa.GrappaBox;
import att.grappa.GrappaConstants;
import att.grappa.GrappaListener;
import att.grappa.GrappaPanel;
import att.grappa.GrappaPoint;
import att.grappa.GrappaSupport;
import att.grappa.Node;
import att.grappa.Subgraph;
import dk.au.daimi.tandrup.MPC.math.fields.Field;
import dk.au.daimi.tandrup.MPC.math.fields.FieldElement;
import dk.au.daimi.tandrup.MPC.net.Activity;
import dk.au.daimi.tandrup.MPC.net.IChannelData;
import dk.au.daimi.tandrup.MPC.net.Participant;
import dk.au.daimi.tandrup.MPC.protocols.PreProcessResult;
import dk.au.daimi.tandrup.MPC.protocols.gates.AbstractGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.AffineGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.MultGate;
import dk.au.daimi.tandrup.MPC.protocols.gates.OutputGate;

public class CircuitBuilderModel {
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private Graph graph = new Graph("circuit", true, false);
	private DemoClient client;
	private static final Activity activity = new Activity("CircuitBuilder"); 

	private VisualGateContainer gates = new VisualGateContainer();

	private VisualGate curSelGate;

	public CircuitBuilderModel(DemoClient client) {
		this.client = client;

		graph.setEditable(true);
		graph.setMenuable(false);

		java.awt.geom.Rectangle2D bb = graph.getBoundingBox().getBounds();
		graph.setAttribute(GrappaConstants.MINBOX_ATTR, new GrappaBox(bb.getX(),bb.getY(),2*bb.getWidth(),bb.getHeight()));

	}

	public Graph getGraph() {
		return graph;
	}

	public GrappaListener getGrappaListener(JButton deleteButton, JComboBox inputProviderComboBox) {
		return new GrapEditor(deleteButton, inputProviderComboBox);
	}

	private class GrapEditor implements GrappaListener {
		private JButton deleteButton;
		private JComboBox inputProviderComboBox;

		public GrapEditor(JButton deleteButton, JComboBox inputProviderComboBox) {
			this.deleteButton = deleteButton;
			this.inputProviderComboBox = inputProviderComboBox;
		}

		private void clearSelection() {
			Enumeration elements = graph.nodeElements();
			while (elements.hasMoreElements())
				((Node)elements.nextElement()).setAttribute("style", "solid");			

			inputProviderComboBox.setEnabled(false);
			deleteButton.setEnabled(false);
		}

		public void grappaClicked(Subgraph subgraph, Element elem, GrappaPoint arg2, int arg3, int arg4, GrappaPanel arg5) {
			clearSelection();
			curSelGate = null;

			if (elem == null)
				return;

			if (elem.getType() == GrappaConstants.EDGE) {
				Edge edge = (Edge)elem;

				VisualGate tailGate = gates.getGate(edge.getTail());
				VisualGate headGate = gates.getGate(edge.getHead()); 

				headGate.removeIngressGate(tailGate);

				updateGraph();

			} else if (elem.getType() == GrappaConstants.NODE) {
				elem.setAttribute("style", "filled");
				graph.repaint();

				Node node = (Node)elem;
				curSelGate = gates.getGate(node);

				deleteButton.setEnabled(true);

				if (curSelGate.getType() == VisualGate.Type.INPUT) {
					updateInputProviderBox(inputProviderComboBox);
					inputProviderComboBox.setEnabled(true);
				}
			}
		}

		public void grappaDragged(Subgraph graph, GrappaPoint arg1, int arg2, Element elem, GrappaPoint arg4, int arg5, GrappaBox arg6, GrappaPanel arg7) {

		}

		public void grappaPressed(Subgraph arg0, Element arg1, GrappaPoint arg2, int arg3, GrappaPanel arg4) {

		}

		public void grappaReleased(Subgraph subgraph, Element elem, GrappaPoint arg2, int arg3, Element pressedElem, GrappaPoint arg5, int arg6, GrappaBox arg7, GrappaPanel arg8) {
			if (elem == pressedElem)
				return;

			clearSelection();

			if (elem == null || pressedElem == null)
				return;

			if (elem.getType() == GrappaConstants.NODE && pressedElem.getType() == GrappaConstants.NODE) {
				Node tailNode = (Node)pressedElem;
				Node headNode = (Node)elem;

				VisualGate tailGate = gates.getGate(tailNode);
				VisualGate headGate = gates.getGate(headNode);

				// Input / random gates does not allow ingress gates
				if (headGate.getType() == VisualGate.Type.INPUT || headGate.getType() == VisualGate.Type.RANDOM)
					return;

				// Output gates does not allow egress gates				
				if (tailGate.getType() == VisualGate.Type.OUTPUT)
					return;

				headGate.addIngressGate(tailGate);

				updateGraph();
			}
		}

		public String grappaTip(Subgraph arg0, Element elem, GrappaPoint arg2, int arg3, GrappaPanel arg4) {
			if (elem != null && elem.getType() == GrappaConstants.EDGE) 
				return "Click to remove edge";

			if (elem != null && elem.getType() == GrappaConstants.NODE) 
				return "Click and drag to other gate to add edge";

			return "Circuit builder";
		}

	}

	public void redrawCircuit() {
		try {
			URL url = new URL("http://www.research.att.com/~john/cgi-bin/format-graph");
			URLConnection urlConn = url.openConnection();
			urlConn.setDoInput(true);
			urlConn.setDoOutput(true);
			urlConn.setUseCaches(false);
			urlConn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
			if(!GrappaSupport.filterGraph(graph,urlConn)) {
				System.err.println("ERROR: somewhere in filterGraph");
			}
			graph.repaint();
			//StringWriter theGraph = new StringWriter();
			//graph.printGraph(theGraph);
			//textarea.append(theGraph.toString());
			//textarea.select(0,0);
			//theGraph.close();

		} catch (IOException ex) {
			logger.throwing(this.getClass().getName(), "validate", ex);
		}
	}

	public void validateCircuit() {
		GraphEnumeration gEnum = graph.elements(GrappaConstants.NODE);
		while (gEnum.hasMoreElements()) {
			Node node = (Node)gEnum.nextElement();
			VisualGate gate = gates.getGate(node);

			if (gate == null) {
				graph.removeNode(node.getName());
				continue;
			}

			switch (gate.getType()) {
			case INPUT:
				if (gate.getIngressGates().size() == 0 && !gates.getInputProvider(gate).equals("")) {
					node.setAttribute("color", "green");
				} else {
					node.setAttribute("color", "red");
				}
				break;
			case OUTPUT:
				if (gate.getIngressGates().size() == 1 && gate.getEgressGates().size() == 0) {
					node.setAttribute("color", "green");
				} else {
					node.setAttribute("color", "red");
				}
				break;
			case ADDITION:
				if (gate.getIngressGates().size() >= 2) {
					node.setAttribute("color", "green");
				} else {
					node.setAttribute("color", "red");
				}
				break;
			case MULTIPLICATION:
				if (gate.getIngressGates().size() == 2) {
					node.setAttribute("color", "green");
				} else {
					node.setAttribute("color", "red");
				}
				break;
			case RANDOM:
				if (gate.getIngressGates().size() == 0) {
					node.setAttribute("color", "green");
				} else {
					node.setAttribute("color", "red");
				}
				break;
			}
		}
	}

	public void updateGraph() {
		removeEdges();
		cleanUpUnusedNodes();

		paintGates(gates.getIGates());
		paintGates(gates.getOGates());
		paintGates(gates.getAGates());
		paintGates(gates.getMGates());
		paintGates(gates.getRGates());

		validateCircuit();
		redrawCircuit();
	}

	private void cleanUpUnusedNodes() {
		GraphEnumeration nodes = graph.elements(GrappaConstants.NODE);
		while (nodes.hasMoreElements()) {
			Node node = (Node)nodes.nextGraphElement();
			if (!gates.contains(node)) {
				graph.removeNode(node.getName());
			}
		}
	}

	private void removeEdges() {
		GraphEnumeration edges = graph.elements(GrappaConstants.EDGE);
		while (edges.hasMoreElements()) {
			Element elem = edges.nextGraphElement();
			graph.removeEdge(elem.getName());
		}
	}

	private void paintGates(Collection<VisualGate> gates) {
		for (VisualGate gate : gates) {
			paintGateAndEdges(gate);
		}
	}

	private Node paintGateAndEdges(VisualGate gate) {
		Node node = paintGate(gate);

		// Add edges
		for (VisualGate ingressGate : gate.getIngressGates()) {
			Node ingressNode = paintGate(ingressGate);

			Edge edge = new Edge(graph, ingressNode, node);
			graph.addEdge(edge);
		}

		return node;
	}

	private Node paintGate(VisualGate gate) {
		Node node = graph.findNodeByName(gate.getGraphNodeName());

		// Create node
		if (node == null) {
			node = new Node(graph, gate.getGraphNodeName());
			node.setAttribute("label", gate.getType().getLabel() + " " + gates.getGateNo(gate));
			graph.addNode(node);
			gates.addNode(node, gate);
		}

		return node;
	}

	public int getAffineCount() { return gates.getAGates().size(); }
	public int getInputCount() { return gates.getIGates().size(); }
	public int getMultCount() { return gates.getMGates().size(); }
	public int getOutputCount() { return gates.getOGates().size(); }
	public int getRandomCount() { return gates.getRGates().size(); }

	public void addRandomGate() {
		gates.addGate(new VisualGate(VisualGate.Type.RANDOM, gates.generateIdent()));
	}

	public void addMultGate() {
		gates.addGate(new VisualGate(VisualGate.Type.MULTIPLICATION, gates.generateIdent()));
	}

	public void addAffineGate() {
		gates.addGate(new VisualGate(VisualGate.Type.ADDITION, gates.generateIdent()));
	}

	public void addInputGate() {
		VisualGate gate = new VisualGate(VisualGate.Type.INPUT, gates.generateIdent());
		gates.addGate(gate);
		gates.setInputProvider(gate, "");
	}

	public void addOutputGate() {
		gates.addGate(new VisualGate(VisualGate.Type.OUTPUT, gates.generateIdent()));
	}

	public void deleteSelectedGate() {
		if (curSelGate != null)
			gates.removeGate(curSelGate);

		updateGraph();
	}

	public void broadcast() {
		try {
			client.getChannel().broadcast(activity, gates);
		} catch (IOException ex) {
			logger.throwing(this.getClass().getName(), "broadcast", ex);
		} catch (GeneralSecurityException ex) {
			logger.throwing(this.getClass().getName(), "broadcast", ex);
		}
	}

	public void receive() {
		try {
			IChannelData chData = client.getChannel().receive(activity);
			gates = (VisualGateContainer)chData.getObject();
			updateGraph();
		} catch (IOException ex) {
			logger.throwing(this.getClass().getName(), "broadcast", ex);
		} catch (GeneralSecurityException ex) {
			logger.throwing(this.getClass().getName(), "broadcast", ex);
		} catch (ClassNotFoundException ex) {
			logger.throwing(this.getClass().getName(), "broadcast", ex);
		}
	}

	public void updateInputProviderBox(JComboBox inputProviderComboBox) {
		inputProviderComboBox.removeAllItems();
		inputProviderComboBox.addItem("");

		for (Participant part : client.getChannel().listConnectedParticipants()) {
			inputProviderComboBox.addItem(part.getCertificateSubjectDN());
		}

		if (curSelGate != null)
			inputProviderComboBox.setSelectedItem(gates.getInputProvider(curSelGate));
	}


	public void changeInputProvider(String dn) {
		if (curSelGate != null) {
			if (dn.length() > 0) {
				logger.config("Setting " + curSelGate + " to " + dn);
				gates.setInputProvider(curSelGate, dn);
				
				validateCircuit();
				redrawCircuit();
			}
		}
	}

	private AbstractGate visual2abstract(PreProcessResult res, VisualGate vGate) {
		int id = gates.getGateNo(vGate);
		switch (vGate.getType()) {
		case INPUT:
			return res.getInputGate(id);
		case OUTPUT:
			return res.getOutputGate(id);
		case ADDITION:
			return res.getAffineGate(id);
		case MULTIPLICATION:
			return res.getMultGate(id);
		case RANDOM:
			return res.getRandomGate(id);
		}
		throw new IllegalStateException("Unknown v gate type");
	}

	private List<AbstractGate> visual2abstract(PreProcessResult res, SortedSet<VisualGate> vGates) {
		List<AbstractGate> retVal = new ArrayList<AbstractGate>(vGates.size());

		for (VisualGate vGate : vGates) {
			retVal.add(visual2abstract(res, vGate));
		}

		return retVal;
	}

	public void configureResult(PreProcessResult res) {
		Field field = client.getFieldF();

		for (VisualGate vGate : gates.getAGates()) {
			AffineGate aGate = res.getAffineGate(gates.getGateNo(vGate));
			List<AbstractGate> gates = visual2abstract(res, vGate.getIngressGates());

			AbstractGate[] gid = gates.toArray(new AbstractGate[0]);
			FieldElement[] a = new FieldElement[gid.length];

			for (int i = 0; i < a.length; i++) {
				a[i] = field.one();
			}

			aGate.configure(field.zero(), gid, a);
		}

		for (VisualGate vGate : gates.getMGates()) {
			MultGate mGate = res.getMultGate(gates.getGateNo(vGate));
			List<AbstractGate> gates = visual2abstract(res, vGate.getIngressGates());
			mGate.setInputGates(gates.get(0), gates.get(1));
		}

		for (VisualGate vGate : gates.getOGates()) {
			OutputGate oGate = res.getOutputGate(gates.getGateNo(vGate));
			List<AbstractGate> gates = visual2abstract(res, vGate.getIngressGates());
			oGate.setInput(gates.get(0));
		}
	}

	public Participant[] getInputProviders() {
		Participant[] parts = new Participant[getInputCount()];

		for (VisualGate gate : gates.getIGates()) {
			int id = gates.getGateNo(gate);
			String dn = gates.getInputProvider(gate);

			for (Participant part : client.getChannel().listConnectedParticipants()) {
				if (dn.equals(part.getCertificateSubjectDN())) {
					parts[id] = part;
					break;
				}
			}
			if (parts[id] == null) {
				IllegalStateException ex = new IllegalStateException("Couldn't find participant for: " + dn);
				logger.throwing(this.getClass().getName(), "getInputProviders", ex);
				throw ex;
			}
		}

		logger.config("Returning these input providers: " + Arrays.toString(parts));

		return parts;
	}


}
