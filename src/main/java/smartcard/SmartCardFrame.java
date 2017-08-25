package smartcard;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SmartCardFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	protected static final int DEFAULT_WIDTH = 700;
	protected static final int DEFAULT_HEIGHT = 600;
	private static JFrame frame = null;

	public static void launch() {
		if (null != frame) {
			frame.setVisible(true);
			return;
		}
		Runnable r = new Runnable() {
      public void run() {
      	frame = new SmartCardFrame();
      	Dimension size = GraphicsToolkit.cropToMainScreenSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
      	frame.setSize(size);
        Point centre = GraphicsToolkit.getCentreOfMainScreen();
        Point topLeft = GraphicsToolkit.calculateTopLeft(centre, size);
        frame.validate();
        frame.setLocation(topLeft);
        frame.setVisible(true);
        frame.addWindowListener(new WindowListener() {
					public void windowOpened(WindowEvent e) {}
					public void windowIconified(WindowEvent e) {}
					public void windowDeiconified(WindowEvent e) {}
					public void windowDeactivated(WindowEvent e) {}
					public void windowClosing(WindowEvent e) {
						System.exit(0);
					}
					public void windowActivated(WindowEvent e) {}
					public void windowClosed(WindowEvent e) {
						System.exit(0);
					}
				});
      }
      
		};
		try {
      javax.swing.SwingUtilities.invokeAndWait(r);
    } catch (InterruptedException e) {
    } catch (InvocationTargetException e) {
    }
		
	}
	private JPanel jpMain;
	public SmartCardFrame () {
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			this.setTitle("Smart card test");
			initFrame();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void initFrame() {
		jpMain = new JPanel(new RiverLayout());
		this.getContentPane().add(jpMain);
		jpMain.add("vtop", new JLabel("This application is used to test the ability of using a smart card to sign a document fragment"));
		jpMain.add("br",new SmartCardInfoPanel());
	}
}
