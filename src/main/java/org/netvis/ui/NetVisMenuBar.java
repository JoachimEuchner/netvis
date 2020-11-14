package org.netvis.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
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
  ButtonGroup mCaptureInterfacesButtonGroup;
  JMenuItem mCaptureMenuStartItem;
  JMenuItem mCaptureMenuPauseItem;
  JMenuItem mCaptureMenuStopItem;
  JMenuItem mCaptureMenuClearItem;
  
  JMenu mLayoutMenu;
  JMenuItem mStartLayoutingItem;
  JMenuItem mStopLayoutingItem;
  JMenuItem mIncCharge;
  JMenuItem mDecCharge;
  JMenuItem mIncSpring;
  JMenuItem mDecSpring;
  
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
    mCaptureInterfacesButtonGroup = new ButtonGroup();
    List<PcapNetworkInterface> allDevs = null;
    try {
      allDevs = Pcaps.findAllDevs();
    } 
    catch (PcapNativeException e) {
      e.printStackTrace();
    }
    int currentNifIdx = 1;
    if(  mMain.getNetVisListener() != null ) {
      currentNifIdx = mMain.getNetVisListener().getCurrentNifIdx();
    }
    if( allDevs != null ) {
      int i = 0;
      for (PcapNetworkInterface nif : allDevs) {
        JRadioButtonMenuItem radioMenuItemForNif = new
            JRadioButtonMenuItem(Integer.toString(i)+": "+nif.getName(),
                ( i == currentNifIdx ));
        radioMenuItemForNif.setActionCommand(Integer.toString(i)+": "+nif.getName());
        radioMenuItemForNif.addActionListener( this );
        mCaptureMenuInterfacesItem.add(radioMenuItemForNif);
        mCaptureInterfacesButtonGroup.add(radioMenuItemForNif);
        if( currentNifIdx == i ) {
          mCaptureMenuInterfacesItem.setSelected(true);
        } else {
          mCaptureMenuInterfacesItem.setSelected(false);
        }
          
        i++;
      }
    }
    mCaptureMenu.add( mCaptureMenuInterfacesItem );
    
    // Start
    mCaptureMenuStartItem = new JMenuItem("start");
    mCaptureMenuStartItem.addActionListener(this);
    mCaptureMenu.add(mCaptureMenuStartItem);
    // pause
    mCaptureMenuPauseItem = new JMenuItem("pause");
    mCaptureMenuPauseItem.addActionListener(this);
    mCaptureMenu.add(mCaptureMenuPauseItem);
    // Stop
    mCaptureMenuStopItem = new JMenuItem("stop");
    mCaptureMenuStopItem.addActionListener(this);
    mCaptureMenu.add(mCaptureMenuStopItem);
    // Clear
    mCaptureMenuClearItem = new JMenuItem("clear");
    mCaptureMenuClearItem.addActionListener(this);
    mCaptureMenu.add(mCaptureMenuClearItem);
    
    add( mCaptureMenu );
    
    
    // -------------- Layout  -----------------
    mLayoutMenu = new JMenu("Layout");
    // start
    mStartLayoutingItem = new JMenuItem("Start Layouting");
    mStartLayoutingItem.addActionListener(this);
    mLayoutMenu.add(mStartLayoutingItem);
    // stop
    mStopLayoutingItem = new JMenuItem("Stop Layouting");
    mStopLayoutingItem.addActionListener(this);
    mLayoutMenu.add(mStopLayoutingItem);
    
    mIncCharge = new JMenuItem("inc. charge");
    mIncCharge.addActionListener(this);
    mLayoutMenu.add(mIncCharge);
    mDecCharge = new JMenuItem("dec. charge");
    mDecCharge.addActionListener(this);
    mLayoutMenu.add(mDecCharge);
    mIncSpring = new JMenuItem("inc. spring");
    mIncSpring.addActionListener(this);
    mLayoutMenu.add(mIncSpring);
    mDecSpring = new JMenuItem("dec. spring");
    mDecSpring.addActionListener(this);
    mLayoutMenu.add(mDecSpring);
    
    add( mLayoutMenu );
  }

  
  private static int getNifIdxFromActionCmd( String cmd ) {
    int idx = -1;
    if( cmd.contains(": ")) {
      try {
        String id = cmd.substring(0, cmd.indexOf(": "));
        idx = Integer.parseInt(id);
       
      }
      catch( StringIndexOutOfBoundsException sioobe ) {
        sioobe.printStackTrace();
      }
      catch( NumberFormatException nfe ){
        nfe.printStackTrace();
      }
    }
    return idx;
  }
  
  
  @Override
  public void actionPerformed(ActionEvent e) {
    logger.debug("NetVisMenuBar.actionPerformed( {} ) called", e);
    String cmd = e.getActionCommand();
    if( cmd.contains(": ")) {
      int idx = getNifIdxFromActionCmd (cmd );
      if( idx != -1 ) {
        mMain.getNetVisListener().selectNif(idx);
      }
    }
    else if( cmd.equals("start")) {
      ButtonModel bm = mCaptureInterfacesButtonGroup.getSelection();
      String ac = bm.getActionCommand();
      if( ac.contains(": ")) {
        int idx = getNifIdxFromActionCmd ( ac );
        if( idx != -1 ) {
          mMain.getNetVisListener().selectNif( idx );
        }
      }
    }
    else if( cmd.equals("pause")) {
      mMain.getNetVisListener().stop();
    }
    else if( cmd.equals("stop")) {
      mMain.getNetVisListener().stop();
      mMain.getNetVisListener().clear();
    }
    else if( cmd.equals("clear")) {
      mMain.getNetVisListener().clear();
    }
    else if( cmd.equals("Start Layouting")) {
      mMain.getNetVisFrame().getNetVisGraphComponent().startLayouting();
    }
    else if( cmd.equals("Stop Layouting")) {
      mMain.getNetVisFrame().getNetVisGraphComponent().stopLayouting();
    }
    else if( cmd.equals("inc. charge")) {
      mMain.getNetVisFrame().getNetVisGraphComponent().incCharge();
    }
    else if( cmd.equals("dec. charge")) {
      mMain.getNetVisFrame().getNetVisGraphComponent().decCharge();
    }
    else if( cmd.equals("inc. spring")) {
      mMain.getNetVisFrame().getNetVisGraphComponent().incSpring();
    }
    else if( cmd.equals("dec. spring")) {
      mMain.getNetVisFrame().getNetVisGraphComponent().decSpring();
    }
  }
 
}
