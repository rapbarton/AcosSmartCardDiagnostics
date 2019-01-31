package net.mohc.smartcard;

import java.awt.BorderLayout;
import java.util.Calendar;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class AboutPanel extends JPanel {
	private static final long serialVersionUID = 2803699942090873875L;

	public AboutPanel () {
		super();
		initialise();
	}

	private void initialise() {
		JLabel text = new JLabel("<html><h1>Smart Card Application</h1><p>Provides OPMS access to prescribing card</p><p>MOHC LTD (c)" + getCurrentYear() + "</p><br/><br/></html>");
		this.add(text, BorderLayout.CENTER);
	}

	private int getCurrentYear() {
		return Calendar.getInstance().get(Calendar.YEAR);
	}
}
