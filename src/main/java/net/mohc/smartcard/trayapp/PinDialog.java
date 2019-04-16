package net.mohc.smartcard.trayapp;

import java.awt.Image;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import net.mohc.smartcard.utils.ImageHelper;
import net.mohc.smartcard.utils.RiverLayout;

public class PinDialog {
	private PinDialog () {
		initialise();
	}	
	private static PinDialog instance = null;
	private static final String okOption = "Unlock";
	
	private JOptionPane pane = null;  
	private JDialog dialogue = null;
	private JPasswordField pinField;
	private ImageHelper imageHelper;
	
	public static char[] showPinDialog() {
		if (null == instance) {
			instance = new PinDialog();
		}		
		if (instance.dialogue.isVisible()) {
			instance.dialogue.setVisible(false);
		}		
		return instance.waitForPin();
	}
	
	private char[] waitForPin() {
		pinField.setText("");
		dialogue.setVisible(true);
		boolean result = okOption.equals(instance.pane.getValue());
		char [] pin;
		if (result) {
			pin = pinField.getPassword();
		} else {
			pin = null;
		}
		return pin;
	}
	
	private void initialise() {
		imageHelper = new ImageHelper();
		Object[] options = { okOption };
		Object[] message = new Object[1];
		pinField = new JPasswordField(10);
		final JPanel tp = new JPanel(new RiverLayout(5, 5));
		tp.add("hfill", new JLabel("<html><b>Enter card pin<br>and click \"Unlock\"</b></html>"));
		tp.add("", pinField);
		message[0] = tp;
		
		Image imageCard = imageHelper.getImage("smart-card-16");
		final JFrame tmpFrame = new JFrame();
    tmpFrame.setIconImage(imageCard);
    
		Icon icon = imageHelper.getIcon("Padlock32");
		pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.DEFAULT_OPTION, icon , options, options[0]);  
		dialogue = pane.createDialog(tmpFrame, "Pin required");  
		dialogue.setAlwaysOnTop(true);

		pinField.addAncestorListener(new AncestorListener() {
			public void ancestorAdded(AncestorEvent event) {
				tmpFrame.toFront();
				dialogue.toFront();
				dialogue.requestFocusInWindow();
				pinField.requestFocusInWindow();
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if(!pinField.isRequestFocusEnabled()) { 
							pinField.setRequestFocusEnabled(true); 
						}
						pinField.requestFocus();
					}
				});
			}
			public void ancestorMoved(AncestorEvent event) {/*Not interested*/}
			public void ancestorRemoved(AncestorEvent event) {/*Not interested*/}
		});

		tp.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						dialogue.toFront();
						dialogue.requestFocus();
					}
				});
				dialogue.toFront();
				dialogue.requestFocus();
			}
			@Override
			public void componentResized(ComponentEvent e) {/*Not interested*/}
			@Override
			public void componentMoved(ComponentEvent e) {/*Not interested*/}
			@Override
			public void componentHidden(ComponentEvent e) {/*Not interested*/}
		});
		
	}
	
}
