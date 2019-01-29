package net.mohc.utils;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;

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
	  GraphicsDevice gsMain = getFirstWidestScreen(gs);
	  return gsMain;
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
	  Dimension revised = new Dimension(x, y);
		return revised;
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
    Insets insets = null;
    if (gd != null) {
      insets = Toolkit.getDefaultToolkit().getScreenInsets(gd.getDefaultConfiguration());
    }
    return insets;
	}

	private Rectangle getScreenBoundsAt(Point pos) {
    GraphicsDevice gd = getGraphicsDeviceAt(pos);
    Rectangle bounds = null;
    if (gd != null) {
      bounds = gd.getDefaultConfiguration().getBounds();
    }
    return bounds;
	}

	private GraphicsDevice getGraphicsDeviceAt(Point pos) {
    GraphicsDevice device = null;
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice lstGDs[] = ge.getScreenDevices();
    ArrayList<GraphicsDevice> lstDevices = new ArrayList<GraphicsDevice>(lstGDs.length);
    for (GraphicsDevice gd : lstGDs) {
    	GraphicsConfiguration gc = gd.getDefaultConfiguration();
    	Rectangle screenBounds = gc.getBounds();
    	if (screenBounds.contains(pos)) {
    		lstDevices.add(gd);
    	}
    }
    if (lstDevices.size() > 0) {
    	device = lstDevices.get(0);
    } else {
    	device = ge.getDefaultScreenDevice();
    }
    return device;
	}

}
