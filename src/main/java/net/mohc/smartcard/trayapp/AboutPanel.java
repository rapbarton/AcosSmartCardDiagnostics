package net.mohc.smartcard.trayapp;

import java.awt.Component;
import java.util.Calendar;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.JLabel;

import org.apache.log4j.Logger;

import net.mohc.smartcard.utils.ImageHelper;

public class AboutPanel extends PopupMenuItemPanel {
	private static final long serialVersionUID = 2803699942090873875L;
	private transient ImageHelper imageHelper;
  private static ResourceBundle bundle = null;

	public AboutPanel () {
		super();
		imageHelper = new ImageHelper();
		initialise();
	}
	
	@Override
	public String getTitle() {
		return "About Smart Card Tool Tray Application";
	}

	@Override
	public String getMenuName() {
		return "About";
	}

	@Override
	public String getIconName() {
		return "About16";
	}
	
	private void initialise() {
		add("vtop hfill", new JLabel(imageHelper.getIcon("about")));
		add("br", createMainInfo());
	}	

	private Component createMainInfo() {
		StringBuilder content = new StringBuilder();
		content.append("<html><h1>Smart Cards for Prescribing</h1>");
		content.append("<p>Digital signing of prescriptions for OPMS<br/><br/></p>");
		content.append("<p>Version: ");
		content.append(getVersion());
		content.append("</p>");
		content.append("<p>JVM: ");
		content.append(getJVMVersion());
		content.append("</p>");
		content.append("<p><br/><br/><small>MOHC LTD ");
		content.append(getCurrentYear());
		content.append("<br/></small></p>");
		content.append("<br/><br/></html>"); 
		return new JLabel(content.toString());
	}

  private String getJVMVersion() {
  	String rtVersion = System.getProperty("java.runtime.version");
  	if (!rtVersion.startsWith("1.7.0_")) {
  		rtVersion = "<font color='red'>"+rtVersion+"</font>";
  	}
  	String width = Configuration.getInstance().is64bit()?"<font color='red'>64bit</font>":"32bit";
		return rtVersion + " " +width;
	}

	private static ResourceBundle getBundle() {
  	if (bundle == null) {
  		try {
  			bundle = ResourceBundle.getBundle("smartcard");
  		} catch (MissingResourceException e) {
        Logger.getLogger(AboutPanel.class).error("Resource bundle 'smartcard.properties' was not found or error while reading current version. ");
      }
  	}
  	return bundle;
  }
	
	public static String getVersion () {
		String version = "Unknown";
    try {
    	version = getBundle().getString("smartcard.version");
    } catch (NullPointerException ex) {
    	Logger.getLogger(AboutPanel.class).error("Error reading 'smartcard.version'");
    }
		return version;
	}

	private int getCurrentYear() {
		return Calendar.getInstance().get(Calendar.YEAR);
	}
}
