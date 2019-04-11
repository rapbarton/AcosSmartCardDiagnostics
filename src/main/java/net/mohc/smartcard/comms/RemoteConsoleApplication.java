package net.mohc.smartcard.comms;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.UIManager;

import org.apache.log4j.Logger;

public class RemoteConsoleApplication {
	boolean packFrame = false;

  /**Construct the application*/
  public RemoteConsoleApplication() {
    RemoteConsoleFrame frame = new RemoteConsoleFrame();
    //Validate frames that have preset sizes
    //Pack frames that have useful preferred size info, e.g. from their layout
    if (packFrame) {
      frame.pack();
    }
    else {
      frame.validate();
    }
    //Center the window
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = frame.getSize();
    if (frameSize.height > screenSize.height) {
      frameSize.height = screenSize.height;
    }
    if (frameSize.width > screenSize.width) {
      frameSize.width = screenSize.width;
    }
    frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    frame.setVisible(true);
  }
  /**Main method*/
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e) {
      Logger.getLogger(RemoteConsoleApplication.class).error("Couldn't set look and feel");
    }
    new RemoteConsoleApplication();
  }
}
