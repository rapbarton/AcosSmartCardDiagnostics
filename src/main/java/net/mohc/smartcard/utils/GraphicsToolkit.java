package net.mohc.smartcard.utils;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JDialog;

public class GraphicsToolkit {
	
	public static GraphicsToolkit getInstance() {
		return new GraphicsToolkit();
	}
	
	private GraphicsToolkit () {}
	
	public Point getCentreOfMainScreen() {
	  GraphicsDevice gsMain = getMainGraphicsDevice();
	  Point locationOfMainScreen = getLocationOfScreen(gsMain);
	  Dimension size = getSizeOfScreen(gsMain);
	  int x = locationOfMainScreen.x + size.width / 2;
	  int y = locationOfMainScreen.y + size.height / 2;
	  return new Point(x,y);
	}

	private GraphicsDevice getFirstWidestScreen(GraphicsDevice[] gs) {
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
	
	private GraphicsDevice getMainGraphicsDevice() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	  GraphicsDevice[] gs = ge.getScreenDevices();
	  return getFirstWidestScreen(gs);
	}
	
	private Point getLocationOfScreen(GraphicsDevice gs) {
		GraphicsConfiguration[] gc = gs.getConfigurations();
		Rectangle bounds = gc[0].getBounds();
		return bounds.getLocation();
	}

	private Dimension getSizeOfScreen(GraphicsDevice gs) {
		int height = gs.getDisplayMode().getHeight();
		int width = gs.getDisplayMode().getWidth();
		return new Dimension(width, height);
	}

	public Point calculateTopLeft(Point centre, Dimension size) {
		int x = centre.x - size.width / 2;
		int y = centre.y - size.height / 2;
		return new Point (x, y);
	}

	public Dimension cropToMainScreenSize(Dimension dimension) {
	  GraphicsDevice gsMain = getMainGraphicsDevice();
	  Dimension size = getSizeOfScreen(gsMain);
	  int x = Math.min(dimension.width, size.width);
	  int y = Math.min(dimension.height, size.height);
	  return new Dimension(x, y);
	}
	
	public Rectangle getSafeScreenBounds(Point pos) {
    Rectangle bounds = getScreenBoundsAt(pos);
    Insets insets = getScreenInsetsAt(pos);
    bounds.x += insets.left;
    bounds.y += insets.top;
    bounds.width -= (insets.left + insets.right);
    bounds.height -= (insets.top + insets.bottom);
    return bounds;
	}	

	private Insets getScreenInsetsAt(Point pos) {
    GraphicsDevice gd = getGraphicsDeviceAt(pos);
    Insets insets;
    if (gd != null) {
      insets = Toolkit.getDefaultToolkit().getScreenInsets(gd.getDefaultConfiguration());
    } else {
    	insets = new Insets(20, 20, 20, 20);
    }
    return insets;
	}

	private Rectangle getScreenBoundsAt(Point pos) {
    GraphicsDevice gd = getGraphicsDeviceAt(pos);
    Rectangle bounds;
    if (gd != null) {
      bounds = gd.getDefaultConfiguration().getBounds();
    } else {
    	bounds = new Rectangle(0, 0, 640, 480);
    }
    return bounds;
	}

	private GraphicsDevice getGraphicsDeviceAt(Point pos) {
    GraphicsDevice device = null;
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] lstGDs = ge.getScreenDevices();
    ArrayList<GraphicsDevice> lstDevices = new ArrayList<>(lstGDs.length);
    for (GraphicsDevice gd : lstGDs) {
    	GraphicsConfiguration gc = gd.getDefaultConfiguration();
    	Rectangle screenBounds = gc.getBounds();
    	if (screenBounds.contains(pos)) {
    		lstDevices.add(gd);
    	}
    }
    if (!lstDevices.isEmpty()) {
    	device = lstDevices.get(0);
    } else {
    	device = ge.getDefaultScreenDevice();
    }
    return device;
	}
	
	public Point getPositionForTrayPopupMenu(Point point, JComponent trayPopupMenu) {
		Rectangle bounds = getSafeScreenBounds(point);
		int x = point.x;
		int y = point.y;
		if (y < bounds.y) {
			y = bounds.y;
		} else if (y > bounds.y + bounds.height) {
			y = bounds.y + bounds.height;
		}
		if (x < bounds.x) {
			x = bounds.x;
		} else if (x > bounds.x + bounds.width) {
			x = bounds.x + bounds.width;
		}
		if (x + trayPopupMenu.getPreferredSize().width > bounds.x + bounds.width) {
			x = (bounds.x + bounds.width) - trayPopupMenu.getPreferredSize().width;
		}
		if (y + trayPopupMenu.getPreferredSize().height > bounds.y + bounds.height) {
			y = (bounds.y + bounds.height) - trayPopupMenu.getPreferredSize().height;
		}
		return new Point(x, y);
	}

	public void placeInCentreOfScreen(JDialog dialogue) {
	  Point centre = getCentreOfMainScreen();
	  Rectangle bounds = getSafeScreenBounds(centre);
	  Dimension frameSize = dialogue.getSize();
	  int x = Math.max(bounds.x, centre.x - (frameSize.width / 2));
	  int y = Math.max(bounds.y, centre.y - (frameSize.height / 2));
	  dialogue.setLocation(x, y);
	}

}
