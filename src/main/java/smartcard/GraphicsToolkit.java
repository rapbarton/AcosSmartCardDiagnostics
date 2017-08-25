package smartcard;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;

public class GraphicsToolkit {
	public static Point getCentreOfMainScreen() {
	  GraphicsDevice gsMain = getMainGraphicsDevice();
	  Point locationOfMainScreen = getLocationOfScreen(gsMain);
	  Dimension size = getSizeOfScreen(gsMain);
	  int x = locationOfMainScreen.x + size.width / 2;
	  int y = locationOfMainScreen.y + size.height / 2;
	  return new Point(x,y);
	}

	private static GraphicsDevice getFirstWidestScreen(GraphicsDevice[] gs) {
		int maxWidth = 0;
		int index = 0;
	  for (int i = 0; i < gs.length; i++) {
	  	int width = gs[i].getDisplayMode().getWidth();
	  	if (maxWidth < width) {
	  		maxWidth = width;
	  		index = i;
	  	}
	  }
		return gs[index];
	}
	
	private static GraphicsDevice getMainGraphicsDevice() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	  GraphicsDevice[] gs = ge.getScreenDevices();
	  GraphicsDevice gsMain = getFirstWidestScreen(gs);
	  return gsMain;
	}
	
	private static Point getLocationOfScreen(GraphicsDevice gs) {
		GraphicsConfiguration[] gc = gs.getConfigurations();
		Rectangle bounds = gc[0].getBounds();
		return bounds.getLocation();
	}

	private static Dimension getSizeOfScreen(GraphicsDevice gs) {
		int height = gs.getDisplayMode().getHeight();
		int width = gs.getDisplayMode().getWidth();
		return new Dimension(width, height);
	}

	public static Point calculateTopLeft(Point centre, Dimension size) {
		int x = centre.x - size.width / 2;
		int y = centre.y - size.height / 2;
		return new Point (x, y);
	}

	public static Dimension cropToMainScreenSize(Dimension dimension) {
	  GraphicsDevice gsMain = getMainGraphicsDevice();
	  Dimension size = getSizeOfScreen(gsMain);
	  int x = Math.min(dimension.width, size.width);
	  int y = Math.min(dimension.height, size.height);
	  Dimension revised = new Dimension(x, y);
		return revised;
	}

}
