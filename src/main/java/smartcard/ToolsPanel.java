package smartcard;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ToolsPanel extends JPanel {
	private static final long serialVersionUID = 2803699942090873875L;

	private JButton refreshButton;
	private JButton signTestStringButton;
	private JComboBox<File> dllList;
	private JTextField terminalStatusField;
	private JTextField providerField;
	private SmartCardController smartCardController;
	private JTextField cardPresentStatusField;
	private JTextField cardConnectedStatusField;
	private JTextField certificateStatusField;
	private JTextField signatureField;

	
	public ToolsPanel () {
		super(new RiverLayout());
		initialise();
	}

	private void initialise() {
		initControllers();
		initLayout();
		initActions();
	}

	private void initControllers() {
		smartCardController = SmartCardController.getInstance();
	}

	private void initLayout() {
		add("vtop", new JLabel("Information"));
		providerField = createJTextField("Provider");
		terminalStatusField = createJTextField("Terminal Status");
		cardPresentStatusField = createJTextField("Card in slot");
		cardConnectedStatusField = createJTextField("Card connection");
		certificateStatusField = createJTextField("Certificate");
		add("br", refreshButton = new JButton("Refresh"));
		signatureField = createJTextField("Resultant Signature");
		add("br", signTestStringButton = new JButton("Sign using private key"));
	}

	private JTextField createJTextField(String nameOfField) {
		JTextField aNewField;
		add("br", new JLabel(nameOfField));
		add("tab", aNewField = new JTextField("",50));
		aNewField.setEditable(false);
		return aNewField;
	}

	private void initActions() {
		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doRefreshAction();
			}
		});
		signTestStringButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doSigningAction();
			}
		});
	}

	protected void doRefreshAction() {
		smartCardController.connectToCardAndFindKeys();
		updateFieldsFromSmartCardController();
	}

	private void updateFieldsFromSmartCardController() {
		terminalStatusField.setText(smartCardController.getTerminalStatus());
		cardPresentStatusField.setText(smartCardController.getCardPresentStatus());
		cardConnectedStatusField.setText(smartCardController.getCardConnectedStatus());
		certificateStatusField.setText(smartCardController.getCertificateStatus());
		providerField.setText(smartCardController.getProviderName());
		signatureField.setText("");
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
