package net.mohc.smartcard.trayapp;

import java.awt.Component;
import java.util.Calendar;

import javax.swing.JLabel;

import net.mohc.smartcard.utils.ImageHelper;

public class AboutPanel extends PopupMenuItemPanel {
	private static final long serialVersionUID = 2803699942090873875L;
	private ImageHelper imageHelper;

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
		String content = "<html><h1>Smart Card for Prescribing</h1><p>Digital signing of prescriptions for OPMS<br/></p><br/><br/></html>"; 
		return new JLabel(content);
	}

	private int getCurrentYear() {
		return Calendar.getInstance().get(Calendar.YEAR);
	}
}
