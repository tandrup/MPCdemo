package dk.au.daimi.tandrup.MPC.demo.gui;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.swing.JTextArea;

public class JTextAreaLogHandler extends Handler {
	private JTextArea textArea;

	public void setTextArea(JTextArea textArea) {
		this.textArea = textArea;
	}
	
	@Override
	public void close() throws SecurityException {
		textArea = null;
	}

	@Override
	public void flush() {
		// Do nothing
	}

	@Override
	public void publish(LogRecord record) {
		// Ensure that this log record should be logged by this Handler
		if (!isLoggable(record))
			return;
		
		// Output the formatted data to the GUI
		if (textArea != null) {
			textArea.setText(getFormatter().format(record) + "\n" + textArea.getText());
		}
	}
}
