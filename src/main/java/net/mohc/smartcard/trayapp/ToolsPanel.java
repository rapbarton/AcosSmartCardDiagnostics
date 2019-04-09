package net.mohc.smartcard.trayapp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.smartcardio.CardTerminal;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.mohc.smartcard.manage.PinUtilities;
import net.mohc.smartcard.utils.RiverLayout;

public class ToolsPanel extends JPanel {
	private static final long serialVersionUID = 2803699942090873875L;

	private JButton connectButton;
	private JButton chooseReaderButton;
	private JButton signTestStringButton;
	private JTextField terminalStatusField;
	private transient SmartCardController smartCardController;
	private JTextField cardPresentStatusField;
	private JTextField cardSessionStatusField;
	private JTextField certificateStatusField;
	private JTextField signatureField;
	private JLabel cardPicture;
	private JButton experimentalButton;

	
	public ToolsPanel () {
		super(new RiverLayout());
		initialise();
	}

	private void initialise() {
		initControllers();
		initButtons();
		initLayout();
		initActions();
		smartCardController.getStatus().addStatusChangeListener(new StatusChangeListener() {
			public void actionPerformed(ActionEvent e) {
				doRefreshAction();
			}
		});
		doRefreshAction();
	}

	private void initButtons() {
		connectButton = new JButton("Open keystore");
		signTestStringButton = new JButton("Test signing using card");
		experimentalButton = new JButton("Experimental");
		chooseReaderButton = new JButton("Choose reader");
	}

	private void initControllers() {
		smartCardController = SmartCardController.getInstance();
	}

	private void initLayout() {
		add("vtop hfill", new JLabel("<html><h1>Prescribing Card Diagnostics</h1>"));
		cardPicture = new JLabel("");
		cardPicture.setAlignmentY(SwingConstants.RIGHT);
		add("br", chooseReaderButton);
		add("tab", cardPicture);
		terminalStatusField = createJTextField("Terminal Status");
		cardPresentStatusField = createJTextField("Card in slot");
		cardSessionStatusField = createJTextField("Session");
		certificateStatusField = createJTextField("Certificate/Status");
		add("br", connectButton);
		add("tab", signTestStringButton);
		signatureField = createJTextField("Resultant Signature");
		add("br", experimentalButton);
	}

	private JTextField createJTextField(String nameOfField) {
		JTextField aNewField = new JTextField("",50);
		add("br", new JLabel(nameOfField));
		add("tab", aNewField);
		aNewField.setEditable(false);
		return aNewField;
	}

	private void initActions() {
		connectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doConnectAction();
			}
		});
		signTestStringButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doSigningAction();
			}
		});

		experimentalButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doExperimentAction();
			}
		});
		
		chooseReaderButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doChooseReaderAction();
			}
		});
		
	}

	protected void doChooseReaderAction() {
		List<CardTerminal> allTerminals = smartCardController.findAvailableTerminals();
//		List<String> allTerminalNames = new ArrayList<>();
//		for (CardTerminal cardTerminal : allTerminals) {
//			allTerminalNames.add(cardTerminal.getName());
//		}
		String result = CardReaderChoiceDialog.showChoices(allTerminals, smartCardController.getDefaultCardTerminalName());
		smartCardController.setDefaultCardTerminalName(result);
	}

	protected void doRefreshAction() {
		Logger.getLogger("REFRESH");
		updateFieldsFromSmartCardController();
	}

	private void updateFieldsFromSmartCardController() {
		terminalStatusField.setText(smartCardController.getTerminalStatus());
		cardPresentStatusField.setText(smartCardController.getCardPresentStatus());
		cardSessionStatusField.setText(smartCardController.getCardSessionStatus());
		certificateStatusField.setText(smartCardController.getCertificateStatus());
		cardPicture.setIcon(smartCardController.getIconForSelectedTerminal());
		cardPicture.setText(smartCardController.getNameOfSelectedTerminal());
		boolean loggedIn = smartCardController.isKeyStoreOpen();
		signTestStringButton.setEnabled(loggedIn);
		connectButton.setEnabled(!loggedIn && smartCardController.isCardPresent());
		signatureField.setText("");
	}
	
	protected void doConnectAction() {
		smartCardController.openKeystore();
		updateFieldsFromSmartCardController();
	}

	protected void doSigningAction() {
		if (!smartCardController.isInitialised()) {
			signatureField.setText("Not initialised");
			return;
		}
		String signature = smartCardController.doTestSignature();
		signatureField.setText(signature);
	}

	protected void doExperimentAction() {
		PinUtilities pu = new PinUtilities();
		pu.doExperiment(smartCardController);
	}
}
