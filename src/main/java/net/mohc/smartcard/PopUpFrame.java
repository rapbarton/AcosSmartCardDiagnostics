package net.mohc.smartcard;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import net.mohc.utils.ImageHelper;

public class PopUpFrame extends JDialog {
	private static final long serialVersionUID = 1319372817456128280L;

	public PopUpFrame(String title, JPanel contentPanel) {
		super();
		setTitle(title);
		setModal(true);
		setIconImage(new ImageHelper().getImage("smart-card-16"));
		initialise(contentPanel);
	}

	private void initialise(JPanel contentPanel) {
		//setDefaultCloseOperation(WindowConstants.);
		JPanel toolbar = createDoneToolbar();
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		getContentPane().add(toolbar,BorderLayout.SOUTH);
    pack();
	  Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	  Dimension frameSize = getSize();
	  if (frameSize.height > screenSize.height) {
	    frameSize.height = screenSize.height;
	  }
	  if (frameSize.width > screenSize.width) {
	    frameSize.width = screenSize.width;
	  }
	  setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
	}

	private JPanel createDoneToolbar() {
		JPanel toolbar = new JPanel(new FlowLayout());
		JButton done = new JButton("Done");
		toolbar.add(done);
		done.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispatchEvent(new WindowEvent(PopUpFrame.this, WindowEvent.WINDOW_CLOSING));
			}
		});
		return toolbar;
	}

	public static void show(String title, JPanel contentPanel) {
		PopUpFrame frame = new PopUpFrame(title, contentPanel);
	  frame.setVisible(true);
//	  frame.dispose();
	  
	}

}
