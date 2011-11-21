package dk.au.daimi.tandrup.MPC.demo.gui;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import dk.au.daimi.tandrup.MPC.net.ssl.Endpoint;

public class EndpointsTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	private List<Endpoint> endpoints = new ArrayList<Endpoint>();

	public EndpointsTableModel() {
		/*
		try {
			endpoints.add(new Endpoint(InetAddress.getLocalHost(), 8321, "CN=Mads Tandrup, OU=Daimi"));
			endpoints.add(new Endpoint(InetAddress.getByName("scapa"), 8321, "CN=hans hansen, OU=Daimi"));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}*/
	}
	
	public int getColumnCount() {
		return 3;
	}

	public String getColumnName(int col) {
		switch (col) {
		case 0:
			return "Hostname / IP address";
		case 1:
			return "Port no.";
		case 2:
			return "Certificate DN";
		}
		throw new IllegalArgumentException("Invalid column no. " + col);
	}

	@Override
	public Class<?> getColumnClass(int col) {
		switch (col) {
		case 0:
			return String.class;
		case 1:
			return Integer.class;
		case 2:
			return String.class;
		}
		throw new IllegalArgumentException("Invalid column no. " + col);
	}

	public int getRowCount() {
		return endpoints.size();
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return true;
	}

	public Object getValueAt(int row, int col) {
		if (endpoints.size() <= row)
			throw new IllegalArgumentException("Invalid row no. " + row);

		Endpoint endpoint = endpoints.get(row);

		switch (col) {
		case 0:
			return endpoint.getAddress().getHostName();
		case 1:
			return endpoint.getPort();
		case 2:
			return endpoint.getCertificate();
		}

		throw new IllegalArgumentException("Invalid column no. " + col);
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		if (endpoints.size() <= row)
			throw new IllegalArgumentException("Invalid row no. " + row);

		Endpoint endpoint = endpoints.get(row);

		switch (col) {
		case 0:
			try {
				endpoint.setAddress(InetAddress.getByName((String)aValue));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		case 1:
			endpoint.setPort((Integer)aValue);
			return;
		case 2:
			endpoint.setCertificate((String)aValue);
			return;
		}

		throw new IllegalArgumentException("Invalid column no. " + col);
	}
	
	public void addNewEmpty() {
		try {
			endpoints.add(new Endpoint(InetAddress.getLocalHost(), 8001, "Unknown"));
			fireTableRowsInserted(endpoints.size() - 2, endpoints.size() - 1);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public List<Endpoint> getEndpoints() {
		return endpoints;
	}
}
