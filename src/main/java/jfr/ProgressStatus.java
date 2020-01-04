package jfr;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

public class ProgressStatus {
	JFrame j;
	JProgressBar jb;
	public ProgressStatus() {
		j = new JFrame();
		jb = new JProgressBar(0, 100);
		jb.setBounds(40, 40, 160, 30);
		jb.setValue(0);
		jb.setStringPainted(true);
		j.add(jb);
		j.setSize(250, 150);
		j.setLayout(null);
		j.setVisible(true);
	}
	
	public void setProgressValue(int x) {
		jb.setValue(x);
	}
}
