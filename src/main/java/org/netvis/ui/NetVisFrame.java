package org.netvis.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.netvis.NetVisMain;

public class NetVisFrame extends  JFrame implements FocusListener, KeyListener {
  
  /**
   * 
   */
  private static final long  serialVersionUID = 1L;
  
  private final NetVisMain mMain;
  private final NetVisMenuBar mNVMB;
  
  private final Container mMainContainer;
  private NetVisGraphComponent mNVGC;
  public NetVisGraphComponent getNetVisGraphComponent() { return mNVGC; };
  
  public NetVisFrame( NetVisMain m ) {
    mMain = m;
    this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    
    mNVMB = new NetVisMenuBar( mMain );
    setJMenuBar(mNVMB);
    
    mNVGC = new NetVisGraphComponent( mMain );
    mMainContainer = createContentPane( mNVGC );
    setContentPane( mMainContainer );
    
    setSize( 300, 200 );
    setLocation( 100, 100 );
    
    // pack();
    setVisible(true);
  }
  
  private Container createContentPane(NetVisGraphComponent ngvc) {
    //Create the content-pane-to-be.
    JPanel contentPane = new JPanel(new BorderLayout());
    contentPane.setOpaque(true);

    contentPane.add(ngvc, BorderLayout.CENTER);

    return contentPane;
}
  
  @Override
  public void keyPressed(KeyEvent arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void keyReleased(KeyEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void keyTyped(KeyEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void focusGained(FocusEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void focusLost(FocusEvent e) {
    // TODO Auto-generated method stub
    
  }
  
}
