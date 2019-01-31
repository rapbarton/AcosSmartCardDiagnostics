package net.mohc.utils;

import java.awt.Image;
import java.awt.Toolkit;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class ImageHelper {
	
	public Icon getIcon(String imageName) {
		//TODO Make more robust - find image in resources
		Image image = Toolkit.getDefaultToolkit().getImage("src/main/resources/img/"+imageName+".png");
		return new ImageIcon(image);
	}

	public Image getImage(String imageName) {
		//TODO Make more robust - find image in resources
		return Toolkit.getDefaultToolkit().getImage("src/main/resources/img/"+imageName+".png");
	}

}
