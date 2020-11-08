package org.netvis.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import org.netvis.NetVisMain;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetVisMenuBar extends JMenuBar implements ActionListener {
  
  private static final Logger logger = LoggerFactory.getLogger(NetVisMenuBar.class);
  
  /**
   * 
   */
  private static final long  serialVersionUID = 1L;
  
  NetVisMain mMain;
  
  JMenu mFileMenu;
  JMenuItem mFileMenuOpenItem;
  JMenuItem mFileMenuExitItem;
  
  JMenu mCaptureMenu;
  JMenu mCaptureMenuInterfacesItem;
  JMenuItem mCaptureMenuStartItem;
  JMenuItem mCaptureMenuStopItem;
  JMenuItem mCaptureMenuClearItem;
  
  public NetVisMenuBar( NetVisMain m )  {
    mMain = m;
    
    
    // -------------- File -----------------
    mFileMenu = new JMenu("File");
    mFileMenuOpenItem = new JMenuItem("open");
    mFileMenuOpenItem.setEnabled(false);  // <--- set to true when file-handling is enabled
    mFileMenuOpenItem.addActionListener( new ActionListener() {
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
    mFileMenu.add( mFileMenuOpenItem );
    
    mFileMenu.addSeparator();
    
    mFileMenuExitItem = new JMenuItem("exit");
    mFileMenuExitItem.addActionListener( new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        System.exit(0);
      }
    });
    mFileMenu.add( mFileMenuExitItem );
    add(mFileMenu);
    
    
    // -------------- Capture -----------------
    mCaptureMenu = new JMenu("Capture");
  
    // Interfaces
    mCaptureMenuInterfacesItem = new JMenu("Interfaces");
    ButtonGroup groupInterfaces = new ButtonGroup();
    List<PcapNetworkInterface> allDevs = null;
    try {
      allDevs = Pcaps.findAllDevs();
    } 
    catch (PcapNativeException e) {
      e.printStackTrace();
    }
    int currentNifIdx = 2;
    if(  mMain.getNetVisListener() != null ) {
      currentNifIdx = mMain.getNetVisListener().getCurrentNifIdx();
    }
    if( allDevs != null ) {
      int i = 0;
      for (PcapNetworkInterface nif : allDevs) {
        JRadioButtonMenuItem radioMenuItemForNif = new
            JRadioButtonMenuItem(Integer.toString(i)+": "+nif.getName(),
                ( i == currentNifIdx ));
        radioMenuItemForNif.addActionListener( this );
        mCaptureMenuInterfacesItem.add(radioMenuItemForNif);
        groupInterfaces.add(radioMenuItemForNif);
        i++;
      }
    }
    mCaptureMenu.add( mCaptureMenuInterfacesItem );
    
    // Start
    mCaptureMenuStartItem = new JMenuItem("start");
    mCaptureMenuStartItem.addActionListener(this);
    mCaptureMenu.add(mCaptureMenuStartItem);
    // Stop
    mCaptureMenuStopItem = new JMenuItem("stop");
    mCaptureMenuStopItem.addActionListener(this);
    mCaptureMenu.add(mCaptureMenuStopItem);
    // Clear
    mCaptureMenuClearItem = new JMenuItem("clear");
    mCaptureMenuClearItem.addActionListener(this);
    mCaptureMenu.add(mCaptureMenuClearItem);
    
    add( mCaptureMenu );
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    logger.debug("NetVisMenuBar.actionPerformed( {} ) called", e);
    String cmd = e.getActionCommand();
    if( cmd.contains(": ")) {
      try {
        String id = cmd.substring(0, cmd.indexOf(": "));
        int idx = Integer.parseInt(id);
        mMain.getNetVisListener().selectNif(idx);
      }
      catch( StringIndexOutOfBoundsException sioobe ) {
        sioobe.printStackTrace();
      }
      catch( NumberFormatException nfe ){
        nfe.printStackTrace();
      }
    }
    else if( cmd.equals("start"))
    {
      mMain.getNetVisListener().start();
    }
    else if( cmd.equals("stop"))
    {
      mMain.getNetVisListener().stop();
    }
    else if( cmd.equals("clear"))
    {
      mMain.getNetVisListener().clear();
    }
  }
 
}
