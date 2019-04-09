package net.mohc.smartcard.trayapp;

import java.awt.Component;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import javax.smartcardio.CardTerminal;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.mohc.smartcard.utils.ImageHelper;
import net.mohc.smartcard.utils.RiverLayout;

public class CardReaderChoiceDialog {
	private static final String DEFAULT_CHOICE = "Auto-select";
	private CardReaderChoiceDialog () {}

	public static String showChoices(List<CardTerminal> terminals, String defaultChoice) {
		
		List<Object> choices = new ArrayList<>();
		choices.add(DEFAULT_CHOICE);
		choices.addAll(terminals);
		
		ImageHelper imageHelper = new ImageHelper();
		String okOption = "Select";
		Object[] options = { okOption };
		Object[] message = new Object[1];
		final JComboBox<Object> choiceField = new JComboBox<>(choices.toArray());
		choiceField.setRenderer(new CardReaderRenderer());
		choiceField.setSelectedItem(defaultChoice);
		final JPanel tp = new JPanel(new RiverLayout(5, 5));
		tp.add("hfill", new JLabel("<html><b>You may fix which card reader to use or select \""+DEFAULT_CHOICE+"\" to use the recommended reader</b></html>"));
		tp.add("br", new JLabel("Available readers: "));
		tp.add("tab", choiceField);
		message[0] = tp;
		
		Image imageCard = imageHelper.getImage("genericReader_16");
		final JFrame tmpFrame = new JFrame();
    tmpFrame.setIconImage(imageCard);
    
		Icon icon = imageHelper.getIcon("genericReader_70");
		JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.DEFAULT_OPTION, icon , options, options[0]);  
		final JDialog dialogue = pane.createDialog(tmpFrame, "Select Reader");  
		dialogue.setVisible(true);
		boolean result = okOption.equals(pane.getValue());

		String choice = defaultChoice;
		if (result) {
			Object selected = choiceField.getSelectedItem();
			if (selected instanceof CardTerminal) {
				choice = ((CardTerminal)selected).getName();
			} 
		}
		if (choice.equals(DEFAULT_CHOICE)) {
			choice = "";
		} 
		return choice;
	}
	
	private static class CardReaderRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;
	  private transient SmartCardController controller = SmartCardController.getInstance();
	  private transient ImageHelper imageHelper = new ImageHelper();

	  @Override
	  public Component getListCellRendererComponent(JList<?> list, Object value, int index,
	                                                boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value instanceof CardTerminal) {
      	CardTerminal terminal = (CardTerminal) value;
      	this.setText(controller.getNameOfTerminal(terminal));
      	this.setIcon(controller.getIconForTerminal(terminal));
      } else {
      	this.setText(value.toString());
      	this.setIcon(imageHelper.getIcon("Auto_70"));
      }
      return this;
	  }
	}
}
