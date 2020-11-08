package org.netvis.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import org.netvis.NetVisMain;

public class NetVisMenuBar extends JMenuBar{
  
  /**
   * 
   */
  private static final long  serialVersionUID = 1L;
  
  NetVisMain mMain;
  
  JMenu mFileMenu;
  JMenuItem mFileMenuOpen;
  JMenuItem mFileMenuExit;
  
  public NetVisMenuBar( NetVisMain m )  {
    mMain = m;
    
    mFileMenu = new JMenu("File");
  
    
    mFileMenuOpen = new JMenuItem("open");
    mFileMenuOpen.setEnabled(false);  // <--- set to true when file-handling is enabled
    mFileMenuOpen.addActionListener( new ActionListener() {
      private Component frame;
      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setSelectedFile(new File(""));
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (chooser.showOpenDialog(frame) == JFileChooser.OPEN_DIALOG) {
            File file1 = chooser.getSelectedFile();
            // send this to main
        } else {

        }
      }
    });
    mFileMenu.add( mFileMenuOpen );
    
    mFileMenu.addSeparator();
    
    mFileMenuExit = new JMenuItem("exit");
    mFileMenuExit.addActionListener( new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        System.exit(0);
      }
    });
    mFileMenu.add( mFileMenuExit );
    
    add(mFileMenu);
  }
  
  
}
