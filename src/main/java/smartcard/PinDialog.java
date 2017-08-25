package smartcard;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class PinDialog {

	public static char[] showPinDialog(Component parent) {
		// Options
		String[] options = { "OK" };
		Object[] message = new Object[1];
		final JPasswordField pinField = new JPasswordField(10);
		JPanel tp = new JPanel(new RiverLayout(5, 5));
		tp.add("hfill", new JLabel("<html><b>Enter card pin<br>and select OK</b></html>"));
		tp.add("", pinField);

		message[0] = tp;

		// hack to set the focus to the password field
		pinField.addAncestorListener(new AncestorListener() {
			public void ancestorAdded(AncestorEvent event) {
				// for a simple component: request
				pinField.requestFocusInWindow();
				// for a compound: go to next focusable which is the editor
				// jpf.transferFocus();
			}

			public void ancestorMoved(AncestorEvent event) {
			}

			public void ancestorRemoved(AncestorEvent event) {
			}
		});

		int result = JOptionPane.showOptionDialog(parent, message,
		    "Enter card PIN", // the title of the dialog window
		    JOptionPane.DEFAULT_OPTION, // option type
		    JOptionPane.QUESTION_MESSAGE, // message type
		    null, // optional icon, use null to use the default icon
		    options, // options string array, will be made into buttons
		    options[0] // option that should be made into a default button
		    );

		char [] pin;

		if (result == 0) {
			pin = pinField.getPassword();
		} else {
			pin = null;
		}

		return pin;
	}

}
