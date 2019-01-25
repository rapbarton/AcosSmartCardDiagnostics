package smartcard;

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
		JLabel text = new JLabel("<html><h1>Prescribing Card<h1><p>MOHC LTD (c)" + getCurrentYear() + "</p></html>");
		this.add(text, BorderLayout.CENTER);
	}

	private int getCurrentYear() {
		return Calendar.getInstance().get(Calendar.YEAR);
	}
}
