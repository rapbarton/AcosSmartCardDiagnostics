package net.mohc.smartcard.trayapp;

import java.awt.Image;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import net.mohc.smartcard.utils.ImageHelper;
import net.mohc.smartcard.utils.RiverLayout;

public class CertChoiceDialog {

	public static String showChoices(ArrayList<String> aliases) {
		ImageHelper imageHelper = new ImageHelper();
		String okOption = "Select";
		Object[] options = { okOption };
		Object[] message = new Object[1];
		final JComboBox<String> choiceField = new JComboBox<>(aliases.toArray(new String[0]));
		final JPanel tp = new JPanel(new RiverLayout(5, 5));
		tp.add("hfill", new JLabel("<html><b>There is more than one certificate on this card, please choose and click \"Select\"</b></html>"));
		tp.add("br", new JLabel("Certificates:"));
		tp.add("tab", choiceField);
		message[0] = tp;
		
		Image imageCard = imageHelper.getImage("smart-card-16");
		final JFrame tmpFrame = new JFrame();
    tmpFrame.setIconImage(imageCard);
    
		Icon icon = imageHelper.getIcon("Certificate");
		JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.DEFAULT_OPTION, icon , options, options[0]);  
		final JDialog dialogue = pane.createDialog(tmpFrame, "Multiple Certificates");  
		dialogue.setAlwaysOnTop(true);

		choiceField.addAncestorListener(new AncestorListener() {
			public void ancestorAdded(AncestorEvent event) {
				tmpFrame.toFront();
				dialogue.toFront();
				choiceField.requestFocusInWindow();
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

		String alias;
		if (result) {
			alias = (String)choiceField.getSelectedItem();
		} else {
			alias = aliases.get(0);
		}
		return alias;
	}
}
