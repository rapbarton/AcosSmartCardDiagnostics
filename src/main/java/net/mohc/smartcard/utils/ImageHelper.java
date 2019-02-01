package net.mohc.smartcard.utils;

import java.awt.Image;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class ImageHelper {
	
	public Icon getIcon(String imageName) {
		return getImageIcon(imageName);
	}

	public Image getImage(String imageName) {
		return getImageIcon(imageName).getImage();
	}

	public ImageIcon getImageIcon(String imageName) {
		URL imageUrl = getClass().getClassLoader().getResource("resources/img/" + imageName + ".png");
	  if (imageUrl != null) {
	  	return (new ImageIcon(imageUrl));
	  }
		imageUrl = getClass().getClassLoader().getResource("img/" + imageName + ".png");
	  if (imageUrl != null) {
	  	return (new ImageIcon(imageUrl));
	  }	  
		return null;
	}
	
	
}
