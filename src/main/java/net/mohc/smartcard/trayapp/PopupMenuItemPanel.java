package net.mohc.smartcard.trayapp;

import javax.swing.JPanel;

import net.mohc.smartcard.utils.RiverLayout;

public abstract class PopupMenuItemPanel extends JPanel {
	private static final long serialVersionUID = 7963252075704989006L;

	public PopupMenuItemPanel() {
		super(new RiverLayout());
	}
	
	public abstract String getTitle();
	public abstract String getMenuName();
	public abstract String getIconName();	
	
}
