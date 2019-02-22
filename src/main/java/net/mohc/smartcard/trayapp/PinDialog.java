package net.mohc.smartcard.trayapp;

import java.awt.Component;
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

	public static char[] showPinDialog(Component parent) {
		ImageHelper imageHelper = new ImageHelper();
		String okOption = "Unlock";
		Object[] options = { okOption };
		Object[] message = new Object[1];
		final JPasswordField pinField = new JPasswordField(10);
		final JPanel tp = new JPanel(new RiverLayout(5, 5));
		tp.add("hfill", new JLabel("<html><b>Enter card pin<br>and click \"Unlock\"</b></html>"));
		tp.add("", pinField);
		message[0] = tp;
		
		Image imageCard = imageHelper.getImage("smart-card-16");
		final JFrame tmpFrame = new JFrame();
    tmpFrame.setIconImage(imageCard);
    
		Icon icon = imageHelper.getIcon("Padlock32");
		JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.DEFAULT_OPTION, icon , options, options[0]);  
		final JDialog dialogue = pane.createDialog(tmpFrame, "Pin required");  
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
			public void ancestorMoved(AncestorEvent event) {}
			public void ancestorRemoved(AncestorEvent event) {}
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
			public void componentResized(ComponentEvent e) {}
			@Override
			public void componentMoved(ComponentEvent e) {}
			@Override
			public void componentHidden(ComponentEvent e) {}
		});
		
		dialogue.setVisible(true);
		boolean result = okOption.equals(pane.getValue());

		char [] pin;
		if (result) {
			pin = pinField.getPassword();
		} else {
			pin = null;
		}
		return pin;
	}
}
