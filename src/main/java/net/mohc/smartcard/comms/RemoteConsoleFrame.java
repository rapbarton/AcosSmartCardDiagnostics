package net.mohc.smartcard.comms;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Map.Entry;

import static net.mohc.smartcard.trayapp.SmartCardConstants.*;

public class RemoteConsoleFrame extends JFrame
                                implements RemoteControlReplyHandler {

	private static final long serialVersionUID = 2413544793004589640L;
	private static final String CONNECTED_REPLY = "Connected:";
	private static final String SESSIONID_TAG = "[SESSIONID]";
  private transient BasicCommsService commsService;
  private static final String CONNECTED = "Connected";
  private static final String IDLE = "Not connected";

  private transient Logger logger;
  JPanel contentPane;
  JTextField commandTextField = new JTextField();
  JComboBox<Option> commands = new JComboBox<>(getCommands());
  JButton selectCommandButton = new JButton();
  JButton clearCommandTextButton = new JButton();
  JScrollPane scrollPane1 = new JScrollPane();
  JTextArea consoleArea = new JTextArea();
  JButton sendCommandButton = new JButton();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JButton connectButton = new JButton();
  JTextField connectedStatusOutput = new JTextField();
  String sessionId = "";
  RemoteConsoleConnectionMonitor monitor;
	boolean isConnected = false;

  /**Construct the frame*/
  public RemoteConsoleFrame() {
  	this.logger = Logger.getLogger(this.getClass());
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    try {
      init();
      commsService = BasicCommsService.getInstance(false);
      monitor = RemoteConsoleConnectionMonitor.startMonitoring(this);
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
  }
  
  private static class Option {
  	String description;
  	String command;
  	public String toString() {
  		return description;
  	}
  	public String getCommand() {
  		return command;
  	}
  	public Option (String description, String command) {
  		this.command = command;
  		this.description = description;
  	}
  }
  
  private Option[] getCommands() {
		return new Option[] {
				new Option("Test",TACommand.TEST),
				new Option("Is card inserted?",TACommand.CARD_STATUS),
				new Option("Log in",TACommand.LOGIN),
				new Option("Certificate Owner",TACommand.CERT_OWNER),
				new Option("Certificate Details",TACommand.CERT_DETAIL),
				new Option("Certificate Encoded",TACommand.CERT_ENCODED),
				new Option("Log out",TACommand.QUIT+":signout"),
				new Option("Sign",TACommand.SIGN+":"+SESSIONID_TAG+":123456:Test string"),
				new Option("Close tray app",TACommand.QUIT+":shutdown")
		};
	}
	/**Component initialisation*/
  private void init() {
    sendCommandButton.setActionCommand("New");
    sendCommandButton.setText("Send Command");
    sendCommandButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doSendCommandAction();
      }
    });
    contentPane = (JPanel) this.getContentPane();
    contentPane.setLayout(gridBagLayout1);
    this.setSize(new Dimension(600, 400));
    this.setTitle("SmartCard Tray Application Remote Console");
    connectButton.setText("Connect");
    connectButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doConnectAction();
      }
    });
    connectedStatusOutput.setEditable(false);
    connectedStatusOutput.setText(IDLE);
    commandTextField.setSelectionStart(0);
    commandTextField.setText("Enter command text here...");
    selectCommandButton.setToolTipText("Prepare command");
    selectCommandButton.setText("Refresh");
    selectCommandButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doSelectCommandAction();
      }
    });
    clearCommandTextButton.setToolTipText("Clear command");
    clearCommandTextButton.setText("Clear");
    clearCommandTextButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	commandTextField.setText("");
      }
    });
    commands.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					doSelectCommandAction();
				}
			}
		});
    
    consoleArea.setBorder(BorderFactory.createLoweredBevelBorder());
    consoleArea.setBackground(UIManager.getColor("info"));
    consoleArea.setEditable(false);
    contentPane.add(connectButton, new GridBagConstraints(0, 0, 1, 1, 0.2, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 30), 0, 0));
    contentPane.add(sendCommandButton, new GridBagConstraints(0, 3, 1, 1, 0.2, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
    contentPane.add(connectedStatusOutput, new GridBagConstraints(4, 0, 1, 1, 0.6, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
    contentPane.add(commandTextField, new GridBagConstraints(0, 2, 5, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
    contentPane.add(new JLabel("Select a command:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
        ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
    contentPane.add(commands, new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0
        ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
    contentPane.add(selectCommandButton, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    contentPane.add(clearCommandTextButton, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    contentPane.add(scrollPane1, new GridBagConstraints(0, 4, 5, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 40));
    scrollPane1.getViewport().add(consoleArea, null);
  }
  /**Overridden so we can exit when window is closed*/
  @Override
  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      System.exit(0);
    }
  }

  void doConnectAction() {
		commsService.startComms();
  }

  void doSendCommandAction() {
    try {
    	commsService.sendCommand(new TACommand(commandTextField.getText()), createHandler());
      connectedStatusOutput.setText("Message sent");
    } catch(Exception ex) {
      connectedStatusOutput.setText("Error");
      logger.error("ERROR: " + ex.toString());
    }
  }

  void doSelectCommandAction() {
  	Option option = (Option)commands.getSelectedItem();
  	String command = option.getCommand();
  	if (command.contains(SESSIONID_TAG) && !sessionId.isEmpty()) {
  		command = command.replace(SESSIONID_TAG, sessionId);
    }  	
    commandTextField.setText(command);
  }

  public void processReply (String sMsg) {
    if (sMsg.equals("TEST OK")) {
      connectedStatusOutput.setText(CONNECTED);
    }
    if (sMsg.startsWith(CONNECTED_REPLY)) {
    	sessionId = sMsg.replace(CONNECTED_REPLY, "").trim();
    }
    consoleArea.append(sMsg + "\n");
  }

  private TAResponseHandler createHandler() {
  	return new TAResponseHandler() {
			
			@Override
			public void messageResponse(final Map<String, String> responses) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						consoleArea.append(convertResponsesToString(responses).trim() + "\n");
					}
				});
			}
			
			@Override
			public void messageError(final String errorMessage) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						consoleArea.append("ERROR: " + errorMessage + "\n");
					}
				});
			}
		};
  }
  
	private String convertResponsesToString(Map<String, String> responses) {
		if (responses.isEmpty()) {
			return "Missing response";
		}
		if (responses.size() == 1 && responses.containsKey(KEY_PRIMARY_RESPONSE)) {
			String txt = responses.get(KEY_PRIMARY_RESPONSE);
			return txt.isEmpty()?"Empty response":txt;
		}
		StringBuilder sb = new StringBuilder();
		if (responses.containsKey(KEY_PRIMARY_RESPONSE)) {
			sb.append("Primary = ");
			sb.append(responses.get(KEY_PRIMARY_RESPONSE));
			sb.append("\n");
		}
		for (Entry<String, String> response : responses.entrySet()) {
			if (!KEY_PRIMARY_RESPONSE.equals(response.getKey())) {
				if (response.getKey().equals("sessionId")) {
					sessionId = response.getValue();
				}
				sb.append(response.getKey());
				sb.append(" = ");
				sb.append(response.getValue());
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	protected void doCheck() {
		if (null == commsService) return;
		boolean currentlyConnected = commsService.isAvailable();
		boolean changed = isConnected != currentlyConnected;
		if (changed) {
			isConnected = currentlyConnected;
			connectedStatusOutput.setText(currentlyConnected?CONNECTED:IDLE);
			consoleArea.append("Service is now " + (currentlyConnected?CONNECTED:IDLE) + "\n");
		}
	}

  
}
