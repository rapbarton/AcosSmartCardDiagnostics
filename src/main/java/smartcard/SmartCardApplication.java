package smartcard;

import java.awt.AWTException;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import org.apache.log4j.Logger;


public class SmartCardApplication {
	private Logger logger;
	private SystemTray systemTray;
	private SmartCardController controller;
	TrayIcon trayIcon;
	
	public static void main(String[] args) {
		try {
			new SmartCardApplication();
		} catch (Exception e) {
			Logger.getLogger(SmartCardApplication.class).fatal("A fatal error has occured, this application will close. The cause:" + e.getMessage());
		}
	}
	
	private SmartCardApplication () {
		logger = Logger.getLogger(this.getClass());
		controller = SmartCardController.getInstance();
		initialiseTray();
		controller.getStatus().addStatusChangeListener(new StatusChangeListener() {
			public void actionPerformed(ActionEvent e) {
				updateIcon(e);
			}
		});
		controller.initialise();
	}
	
	private void initialiseTray () {
    if(!SystemTray.isSupported()){
      throw new SmartCardApplicationException("System tray is not supported !!! ");
    }
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception ex) {
    }
    systemTray = SystemTray.getSystemTray();
    final JPopupMenu trayPopupMenu = new JPopupMenu();
    ImageHelper imageHelper = new ImageHelper();
    JMenuItem tools = new JMenuItem("Tools", imageHelper.getIcon("Tools16"));
    tools.addActionListener(new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		trayPopupMenu.setVisible(false);
    		JOptionPane.showMessageDialog(null, new ToolsPanel());         
    	}
    });     
    trayPopupMenu.add(tools);

    JMenuItem about = new JMenuItem("About", imageHelper.getIcon("About16"));
    about.addActionListener(new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		trayPopupMenu.setVisible(false);
    		JOptionPane.showMessageDialog(null, new AboutPanel());         
    	}
    });     
    trayPopupMenu.add(about);

    JMenuItem close = new JMenuItem("Close", imageHelper.getIcon("Exit16"));
    close.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
    		trayPopupMenu.setVisible(false);
    		controller.shutdown();
      }
    });
    trayPopupMenu.add(close);

    trayIcon = new TrayIcon(controller.getStatus().getCurrentStatusImage(), "Prescribing Card");
    trayIcon.setImageAutoSize(true);
    trayIcon.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
      	if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
      		if (trayPopupMenu.isVisible()) {
      			trayPopupMenu.setVisible(false);
      		} else {
      			showPopup(e);
      		}      		
        } else {
        	if (trayPopupMenu.isVisible()) {
      			trayPopupMenu.setVisible(false);
      		}
        }
      }

			private void showPopup(MouseEvent e) {
    		Rectangle bounds = getSafeScreenBounds(e.getPoint());
    		Point point = e.getPoint();
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
    		trayPopupMenu.setLocation(x, y);
    		trayPopupMenu.setVisible(true);
			}
    });
    
    try{
    	systemTray.add(trayIcon);
    }catch(AWTException awtException){
      throw new SmartCardApplicationException("Not able to add SmartCard to system tray");
    }
	}

	protected void updateIcon(ActionEvent e) {
		Image image = controller.getStatus().getCurrentStatusImage();
		trayIcon.setImage(image);
	}


	public static Rectangle getSafeScreenBounds(Point pos) {

    Rectangle bounds = getScreenBoundsAt(pos);
    Insets insets = getScreenInsetsAt(pos);

    bounds.x += insets.left;
    bounds.y += insets.top;
    bounds.width -= (insets.left + insets.right);
    bounds.height -= (insets.top + insets.bottom);

    return bounds;

	}	

	public static Insets getScreenInsetsAt(Point pos) {
    GraphicsDevice gd = getGraphicsDeviceAt(pos);
    Insets insets = null;
    if (gd != null) {
        insets = Toolkit.getDefaultToolkit().getScreenInsets(gd.getDefaultConfiguration());
    }
    return insets;
	}

	public static Rectangle getScreenBoundsAt(Point pos) {
    GraphicsDevice gd = getGraphicsDeviceAt(pos);
    Rectangle bounds = null;
    if (gd != null) {
        bounds = gd.getDefaultConfiguration().getBounds();
    }
    return bounds;
	}

	public static GraphicsDevice getGraphicsDeviceAt(Point pos) {

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
