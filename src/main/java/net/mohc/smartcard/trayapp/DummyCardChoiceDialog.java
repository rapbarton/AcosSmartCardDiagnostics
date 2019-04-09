package net.mohc.smartcard.trayapp;

import java.awt.Image;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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

public class DummyCardChoiceDialog {
	private DummyCardChoiceDialog () {}

	public static File showChoices(File[] files) {
		ImageHelper imageHelper = new ImageHelper();
		ArrayList<String> filenames = new ArrayList<>();
		HashMap<String, File> namesToFile = new HashMap<>();
		for (File file : files) {
			String name = file.getName();
			filenames.add(name);
			namesToFile.put(name, file);
		}
		String okOption = "Select";
		Object[] options = { okOption };
		Object[] message = new Object[1];
		final JComboBox<String> choiceField = new JComboBox<>(filenames.toArray(new String[0]));
		final JPanel tp = new JPanel(new RiverLayout(5, 5));
		tp.add("hfill", new JLabel("<html><b>There is more than one card inserted, please choose and click \"Select\"</b></html>"));
		tp.add("br", new JLabel("Virtual cards by filename: "));
		tp.add("tab", choiceField);
		message[0] = tp;
		
		Image imageCard = imageHelper.getImage("smart-card-16");
		final JFrame tmpFrame = new JFrame();
    tmpFrame.setIconImage(imageCard);
    
		Icon icon = imageHelper.getIcon("Certificate");
		JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.DEFAULT_OPTION, icon , options, options[0]);  
		final JDialog dialogue = pane.createDialog(tmpFrame, "Multiple Cards");  
		dialogue.setAlwaysOnTop(true);

		choiceField.addAncestorListener(new AncestorListener() {
			public void ancestorAdded(AncestorEvent event) {
				tmpFrame.toFront();
				dialogue.toFront();
				choiceField.requestFocusInWindow();
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
		
		dialogue.setVisible(true);
		boolean result = okOption.equals(pane.getValue());

		File file;
		if (result) {
			String filename = (String)choiceField.getSelectedItem();
			file = namesToFile.get(filename);
		} else {
			file = files[0];
		}
		return file;
	}
}
