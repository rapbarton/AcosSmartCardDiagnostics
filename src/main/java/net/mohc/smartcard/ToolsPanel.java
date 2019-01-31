package net.mohc.smartcard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.mohc.utils.RiverLayout;

public class ToolsPanel extends JPanel {
	private static final long serialVersionUID = 2803699942090873875L;

	private JButton refreshButton;
	private JButton connectButton;
	private JButton signTestStringButton;
	private JTextField terminalStatusField;
	private SmartCardController smartCardController;
	private JTextField cardPresentStatusField;
	private JTextField cardSessionStatusField;
	private JTextField certificateStatusField;
	private JTextField signatureField;
	private JLabel cardPicture;

	
	public ToolsPanel () {
		super(new RiverLayout());
		initialise();
	}

	private void initialise() {
		initControllers();
		initLayout();
		initActions();
		smartCardController.getStatus().addStatusChangeListener(new StatusChangeListener() {
			public void actionPerformed(ActionEvent e) {
				doRefreshAction();
			}
		});
		doRefreshAction();
	}

	private void initControllers() {
		smartCardController = SmartCardController.getInstance();
	}

	private void initLayout() {
		add("vtop hfill", new JLabel("<html><h1>Prescribing Card Diagnostics</h1>"));
		cardPicture = new JLabel("");
		cardPicture.setAlignmentY(SwingConstants.RIGHT);
		add("br tab", cardPicture);
		terminalStatusField = createJTextField("Terminal Status");
		cardPresentStatusField = createJTextField("Card in slot");
		cardSessionStatusField = createJTextField("Session");
		certificateStatusField = createJTextField("Certificate/Status");
		//add("br", refreshButton = new JButton("Refresh"));
		add("br", connectButton = new JButton("Open keystore"));
		add("tab", signTestStringButton = new JButton("Test signing using card"));
		signatureField = createJTextField("Resultant Signature");
	}

	private JTextField createJTextField(String nameOfField) {
		JTextField aNewField;
		add("br", new JLabel(nameOfField));
		add("tab", aNewField = new JTextField("",50));
		aNewField.setEditable(false);
		return aNewField;
	}

	private void initActions() {
//		refreshButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent arg0) {
//				doRefreshAction();
//			}
//		});
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
		
	}

	protected void doRefreshAction() {
		//smartCardController.connectToCardAndFindKeys();
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

}
