package net.mohc.smartcard.trayapp;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.UIManager;

import org.apache.log4j.Logger;

import net.mohc.smartcard.comms.CommsController;
import net.mohc.smartcard.comms.CommsException;
import net.mohc.smartcard.utils.GraphicsToolkit;
import net.mohc.smartcard.utils.ImageHelper;


public class SmartCardApplication {
	private Logger logger;
	GraphicsToolkit graphicesToolkit;
	private SmartCardController controller;
	TrayIcon trayIcon;
	boolean viewInitialised = false;
	
	public static void main(String[] args) {
		boolean quietMode = args.length >= 1 && "-q".equals(args[0]);
		if (System.getProperty("disable.error.popup", "false").equalsIgnoreCase("true")) {
			quietMode = true;
		}
		Logger.getLogger(SmartCardApplication.class).info("disable.error.popup = " + quietMode);
		try {
			new SmartCardApplication();
		} catch (Exception e) {
			Logger.getLogger(SmartCardApplication.class).fatal("A fatal error has occured, this application will close. The cause:" + e.getMessage());
			if (!quietMode) {
				JOptionPane.showMessageDialog(null, "An error has occured, the cause is " + e.getMessage(), "Something Went Wrong...", JOptionPane.ERROR_MESSAGE);
			}
			System.exit(-1);
		}
	}
	
	private SmartCardApplication () throws CommsException {
		logger = Logger.getLogger(this.getClass());
		CommsController.getInstance().startListening(9311, new SmartCardCommandProcessor());
		graphicesToolkit = GraphicsToolkit.getInstance();
		controller = SmartCardController.getInstance();
		initialiseTray();
		registerAsListener();
		controller.initialise();
		logger.info("Smart Card Application started");
	}
	
	private void initialiseTray () {
    if(!SystemTray.isSupported()){
      throw new SmartCardApplicationException("System tray is not supported !!! ");
    }
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception ex) {
    	logger.warn("Could not set look and feel: " + ex.getMessage());
    }
		SystemTray systemTray = SystemTray.getSystemTray();
    final JPopupMenu trayPopupMenu = new JPopupMenu();
    ImageHelper imageHelper = new ImageHelper();
    Color menuHighlightColour = new Color(0x91C9F7);
    MouseListener highlighter = createMenuItemPainter();  
    
    JMenuItem tools = new JMenuItem("Tools", imageHelper.getIcon("Tools16"));
    tools.setBackground(menuHighlightColour);
    tools.addActionListener(new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		trayPopupMenu.setVisible(false);
    		popUpFrame("Smart Card View", new ToolsPanel());
    	}
    });  
    tools.addMouseListener(highlighter);
    trayPopupMenu.add(tools);

    JMenuItem about = new JMenuItem("About", imageHelper.getIcon("About16"));
    about.setBackground(menuHighlightColour);
    about.addActionListener(new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		trayPopupMenu.setVisible(false);
    		popUpFrame("About Smart Card Application", new AboutPanel());
    	}
    });     
    about.addMouseListener(highlighter);
    trayPopupMenu.add(about);

    trayPopupMenu.addSeparator();

    JMenuItem close = new JMenuItem("Close", imageHelper.getIcon("Exit16"));
    close.setBackground(menuHighlightColour);
    close.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
    		trayPopupMenu.setVisible(false);
    		controller.shutdown();
      }
    });
    close.addMouseListener(highlighter);
    trayPopupMenu.add(close);
    createTrayIcon(trayPopupMenu);
    try{
    	systemTray.add(trayIcon);
    }catch(AWTException awtException){
      throw new SmartCardApplicationException("Not able to add SmartCard to system tray");
    }
	}

	private void createTrayIcon (final JPopupMenu trayPopupMenu) {
		trayIcon = new TrayIcon(controller.getStatus().getCurrentStatusImage(), "Prescribing Card");
    trayIcon.setImageAutoSize(true);
    trayIcon.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
      	if (!viewInitialised) {
      		initialiseView(trayPopupMenu);
      	}
      	
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
				Point eventLocation = e.getPoint();
				Point popupLocation = getPositionForPopupMenu(eventLocation, trayPopupMenu);
    		trayPopupMenu.setLocation(popupLocation);
    		trayPopupMenu.setVisible(true);
			}
    });
	}
	
	protected void initialiseView(JPopupMenu trayPopupMenu) {
		MenuElement[] elements = trayPopupMenu.getSubElements();
		for (MenuElement menuElement : elements) {
			Component component = menuElement.getComponent();
			if (component instanceof JMenuItem) {
				JMenuItem item = (JMenuItem)component;
				item.setOpaque(false);
				item.repaint();
			}
		}
	}
	
	private MouseAdapter createMenuItemPainter () {
		return new MouseAdapter() {
    	@Override
			public void mouseExited(MouseEvent e) {
				JMenuItem item = (JMenuItem)e.getSource();
				item.setOpaque(false);
				item.repaint();
			}
    	@Override
			public void mouseEntered(MouseEvent e) {
				JMenuItem item = (JMenuItem)e.getSource();
				item.setOpaque(true);
				item.repaint();
			}
		}; 
	}

	private void popUpFrame(String title, JPanel contentPanel) {
		PopUpFrame.show(title, contentPanel);
		
	}

	protected Point getPositionForPopupMenu(Point point, JComponent trayPopupMenu) {
		Rectangle bounds = graphicesToolkit.getSafeScreenBounds(point);
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

	private void registerAsListener() {
		controller.getStatus().addStatusChangeListener(new StatusChangeListener() {
			public void actionPerformed(ActionEvent e) {
				updateTray(e);
			}
		});
	}

	protected void updateTray(ActionEvent e) {
		logger.debug("State change fired: " + e.getActionCommand());
		Image image = controller.getStatus().getCurrentStatusImage();
		trayIcon.setImage(image);
		String statusSummary = controller.getStatus().getStatusAsText();
		trayIcon.setToolTip(statusSummary);
	}	
}
