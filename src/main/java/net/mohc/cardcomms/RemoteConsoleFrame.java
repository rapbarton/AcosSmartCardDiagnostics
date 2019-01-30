package net.mohc.cardcomms;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.apache.log4j.Logger;

import java.net.* ;

public class RemoteConsoleFrame extends JFrame
                                implements RemoteControlReplyHandler {

  private static final String CONNECTED_REPLY = "Connected:";
	private static final String SESSIONID_TAG = "[SESSIONID]";
	private static final int DEFAULT_PORT = 9311;
  private CommsClient rcc;
//  private RemoteMessage remoteMessage = new RemoteMessage();
  private static final String CONNECTED = "Connected";
  private static final String IDLE = "Not connected";

  Logger logger;
  JPanel contentPane;
  JTextArea jTextArea1 = new JTextArea();
  JComboBox<Option> commands = new JComboBox<>(getCommands());
  JButton jButton1 = new JButton();
  JButton jButton2 = new JButton();
  //JButton jButton3 = new JButton();
  JScrollPane jScrollPane1 = new JScrollPane();
  JTextArea jTextAreaReply = new JTextArea();
  JButton jButtonNew = new JButton();
  JTextField jTextFieldIP = new JTextField();
  JLabel jLabelIP = new JLabel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JButton jButtonConnect = new JButton();
  JTextField jTextFieldConnected = new JTextField();
  String sessionId = "";

  /**Construct the frame*/
  public RemoteConsoleFrame() {
  	this.logger = Logger.getLogger(this.getClass());
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    try {
      jbInit();
      rcc = new CommsClient(DEFAULT_PORT, this);
    } catch (CommsException rce) {
      logger.error(rce.getMessage());
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
				new Option("Test","Test"),
				new Option("Is card inserted?","CardPresentStatus"),
				new Option("Log in","Connect"),
				new Option("Certificate","CertificateStatus"),
				new Option("Log out","Quit:signout"),
				new Option("Sign","Sign:"+SESSIONID_TAG+":123456:Test string"),
				new Option("Close tray app","Quit:shutdown")
		};
	}
	/**Component initialization*/
  private void jbInit() throws Exception  {
    jButtonNew.setActionCommand("New");
    jButtonNew.setText("Send Command");
    jButtonNew.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jButtonNew_actionPerformed(e);
      }
    });
    //setIconImage(Toolkit.getDefaultToolkit().createImage(RemoteConsoleFrame.class.getResource("[Your Icon]")));
    contentPane = (JPanel) this.getContentPane();
    contentPane.setLayout(gridBagLayout1);
    this.setSize(new Dimension(600, 400));
    this.setTitle("SmartCard Tray Application Remote Console");
    jLabelIP.setHorizontalAlignment(SwingConstants.TRAILING);
    jLabelIP.setText("IP Address");
    jTextFieldIP.setText("LOCALHOST");
    jButtonConnect.setText("Connect");
    jButtonConnect.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jButtonConnect_actionPerformed(e);
      }
    });
    jTextFieldConnected.setEditable(false);
    jTextFieldConnected.setText(IDLE);
    jTextArea1.setSelectionStart(0);
    jTextArea1.setText("Enter command text here...");
    jButton1.setToolTipText("Prepare command");
    jButton1.setText("Select");
    jButton1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jButton1_actionPerformed(e);
      }
    });
    jButton2.setToolTipText("Clear command");
    jButton2.setText("Clear");
    jButton2.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jButton2_actionPerformed(e);
      }
    });
//    jButton3.setToolTipText("Log in to card");
//    jButton3.setText("Login");
//    jButton3.addActionListener(new java.awt.event.ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        jButton3_actionPerformed(e);
//      }
//    });
    jTextAreaReply.setBorder(BorderFactory.createLoweredBevelBorder());
    jTextAreaReply.setBackground(UIManager.getColor("info"));
    jTextAreaReply.setEditable(false);
    contentPane.add(jLabelIP, new GridBagConstraints(0, 0, 1, 1, 0.2, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 30), 0, 0));
    contentPane.add(jButtonNew, new GridBagConstraints(0, 1, 1, 1, 0.2, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
    contentPane.add(jButtonConnect, new GridBagConstraints(4, 0, 1, 1, 0.6, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
    contentPane.add(jTextFieldIP, new GridBagConstraints(1, 0, 3, 1, 0.2, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 15), 80, 0));
    contentPane.add(jTextFieldConnected, new GridBagConstraints(4, 1, 1, 1, 0.6, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 22, 0));
    contentPane.add(jTextArea1, new GridBagConstraints(0, 2, 5, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
    contentPane.add(commands, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    contentPane.add(jButton1, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    contentPane.add(jButton2, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    contentPane.add(jScrollPane1, new GridBagConstraints(0, 3, 5, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 40));
    jScrollPane1.getViewport().add(jTextAreaReply, null);
  }
  /**Overridden so we can exit when window is closed*/
  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      System.exit(0);
    }
  }


  void jButtonConnect_actionPerformed(ActionEvent ev) {
    InetAddress ipAddr;
    try {
      String s = jTextFieldIP.getText();
      if (s.equals("LOCALHOST")) {
        ipAddr = InetAddress.getLocalHost();
      } else {
        ipAddr = InetAddress.getByName(s);
      }
      rcc.connect(ipAddr);
      rcc.sendMessage("Test");

    } catch(Exception ex) {
      jTextFieldConnected.setText("Error");
      System.out.println("ERROR: " + ex.toString());
    }

  }

  void jButtonNew_actionPerformed(ActionEvent e) {
    try {
      rcc.sendMessage(jTextArea1.getText());
      jTextFieldConnected.setText("Message sent");
    } catch(Exception ex) {
      jTextFieldConnected.setText("Error");
      System.out.println("ERROR: " + ex.toString());
    }
  }

  void jButton1_actionPerformed(ActionEvent e) {
  	Option option = (Option)commands.getSelectedItem();
  	String command = option.getCommand();
  	if (command.contains(SESSIONID_TAG) && !sessionId.isEmpty()) {
  		command = command.replace(SESSIONID_TAG, sessionId);
    }  	
    jTextArea1.setText(command);
  }

  void jButton2_actionPerformed(ActionEvent e) {
    jTextArea1.setText("");
  }

/*  void jButton3_actionPerformed(ActionEvent e) {
    jTextArea1.setText("Connect:331627");
  }
*/
  public void processReply (String sMsg) {
    if (sMsg.equals("TEST OK")) {
      jTextFieldConnected.setText(CONNECTED);
    }
    if (sMsg.startsWith(CONNECTED_REPLY)) {
    	sessionId = sMsg.replace(CONNECTED_REPLY, "").trim();
    }
    jTextAreaReply.append(sMsg + "\n");
  }

}
