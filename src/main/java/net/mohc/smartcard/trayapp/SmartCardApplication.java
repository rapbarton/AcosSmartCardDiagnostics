package net.mohc.smartcard.trayapp;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.UIManager;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import net.mohc.smartcard.comms.CommsController;
import net.mohc.smartcard.comms.CommsException;
import net.mohc.smartcard.utils.GraphicsToolkit;
import net.mohc.smartcard.utils.ImageHelper;


public class SmartCardApplication {
	public static final String APPLICATION_STARTED_MESSAGE = "Smart Card Application started";
	private static final Color MENU_HIGHLIGHT_COLOUR = new Color(0x91C9F7);
	private Logger logger;
	private GraphicsToolkit graphicesToolkit;
	private SmartCardController controller;
	private ImageHelper imageHelper;
	private TrayIcon trayIcon;
	private JPopupMenu trayPopupMenu;
	private MouseListener highlighter;
	boolean viewInitialised = false;

	/**	
	 * Entry point for Tray Application
	 */
	public static void main(String[] args) {
		configureLog4j();
		boolean quietMode = args.length >= 1 && "-q".equals(args[0]);
		if (System.getProperty("disable.error.popup", "false").equalsIgnoreCase("true")) {
			quietMode = true;
		}
		Logger.getLogger(SmartCardApplication.class).info("disable.error.popup = " + quietMode);
		checkSecurityDebug();		
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
		imageHelper = new ImageHelper();
		initialiseTray();
		registerAsListener();
		controller.initialise();
		System.out.println(APPLICATION_STARTED_MESSAGE);//This line is needed so that the launcher can detect application has started
		logger.info(APPLICATION_STARTED_MESSAGE);
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
    trayPopupMenu = new JPopupMenu();
    highlighter = createMenuItemPainter();    
    
    addPopupItem(new ToolsPanel());
    addPopupItem(new AboutPanel());
    trayPopupMenu.addSeparator();
    addShutdownItem();
    
    createTrayIcon(trayPopupMenu);
    try{
    	systemTray.add(trayIcon);
    }catch(AWTException awtException){
      throw new SmartCardApplicationException("Not able to add SmartCard to system tray");
    }
	}
	
	private void addPopupItem(final PopupMenuItemPanel panel) {
		JMenuItem menuItem = new JMenuItem(panel.getMenuName(), imageHelper.getIcon(panel.getIconName()));
    menuItem.setBackground(MENU_HIGHLIGHT_COLOUR);
    menuItem.addActionListener(new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		trayPopupMenu.setVisible(false);
    		popUpFrame(panel.getTitle(), panel);
    	}
    });  
    menuItem.addMouseListener(highlighter);
    trayPopupMenu.add(menuItem);
	}
	
	private void addShutdownItem() {
    JMenuItem close = new JMenuItem("Close", imageHelper.getIcon("Exit16"));
    close.setBackground(MENU_HIGHLIGHT_COLOUR);
    close.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
    		trayPopupMenu.setVisible(false);
    		controller.shutdown();
      }
    });
    close.addMouseListener(highlighter);
    trayPopupMenu.add(close);
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
				Point popupLocation = graphicesToolkit.getPositionForTrayPopupMenu(eventLocation, trayPopupMenu);
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

	/**
	 * Suggestion to deal with occasional PKCS11 keystore instantiation - looks like switching debug on ensures exception is not thrown.<br/>
	 * See <a href=https://stackoverflow.com/questions/25306191/pkcs11-instantiation-problems>StackOverflow</a><br/>
	 * Needs: -Djava.security.debug=sunpkcs11 (Note: Can't be set at runtime)<br/>	 
	 * See <a href=https://bugs.openjdk.java.net/browse/JDK-8039912>OpenJDK bug</a>. This issue is solved in OpenJDK, but maybe it is still unresolved in Oracle JDK.
	 */
	public static void checkSecurityDebug() {
		String value = System.getProperty("java.security.debug","");
		if (value.trim().isEmpty()) {
			Logger.getLogger(SmartCardApplication.class).warn("Recommend JVM parameter should be set: java.security.debug=sunpkcs11");
		}
	}

	private static void configureLog4j() {
		//setting value for "logs.dir" to overwrite the ${logs.dir} in the corresponding log4j.xml
		String sHome = ensureTrailingSeparator(System.getProperty("user.dir", "."+separator()+"opms"));
		String logFilePath = sHome + "logs";
		System.setProperty("logs.dir", logFilePath);

		String value = System.getProperty("location.log4j","");
		if (!value.trim().isEmpty()) {
			String fullPath = value;
			if (!value.endsWith(".properties")) {
				fullPath = ensureTrailingSeparator(value) + "log4j-smartcard.properties";
			}
			System.out.println("Log4j config = " + fullPath);
			PropertyConfigurator.configure(fullPath);
		}

		System.out.println("Log output path = " + logFilePath);
	}

	private static String ensureTrailingSeparator(String path) {
		if (path.isEmpty()) return path;
		String sep = separator();
		if (path.endsWith(sep)) return path;
		return path + sep;
	}
	
	private static String separator() {
		return System.getProperty("file.separator");
	}



}
