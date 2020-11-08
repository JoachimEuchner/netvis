package org.netvis.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import org.netvis.NetVisMain;
import org.netvis.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NetVisGraphComponent extends JComponent implements
  MouseListener, MouseWheelListener, MouseMotionListener, KeyListener,
  ActionListener
{
  private static final Logger logger = LoggerFactory.getLogger(NetVisGraphComponent.class);

  /**
   * 
   */
  private static final long serialVersionUID = -5194156817975062007L;
  
  private final NetVisMain mMain;
  
  private transient Image                     offscreen;
  private transient Toolkit                   mToolkit;
  private int                                 paintCounter;
  private Font                                myPlain11Font;
  private int                                 mWidth;
  private int                                 mHeight;

  private final ArrayList<NetVisGraphNode> mAllGraphNodes;
  public List<NetVisGraphNode> getAllGraphNodes() { return mAllGraphNodes; }
  
  private final ArrayList<NetVisGraphConnection> mAllGraphConnections;
  public List<NetVisGraphConnection> getAllGraphConnections() { return mAllGraphConnections; }
  
  /**
   * <ctor>
   * @param main
   */
  public NetVisGraphComponent( NetVisMain main ) {
    logger.debug("NetVisComponent<ctor> called.");
    mMain = main;
    
    addMouseListener(this);
    addKeyListener(this);
    addMouseWheelListener(this);
    addMouseMotionListener(this);
    
    this.mAllGraphNodes = new ArrayList<>(1000);
    this.mAllGraphConnections =  new ArrayList<>(1000);
    
    this.myPlain11Font = new Font("Courier", Font.PLAIN, 11);
  }
  
  
  public void addGraphNode( NetVisGraphNode nvgn ) {
    mAllGraphNodes.add(nvgn);
  }
  
  public void addGraphConnection( NetVisGraphConnection nvgc ) {
    mAllGraphConnections.add(nvgc);
  }
  
  
  @Override
  public void invalidate() {
    super.invalidate();
    this.offscreen = null;
  }

  @Override
  public void update( Graphics g) {
    paint(g);
  }

  @Override
  public void paint( Graphics g) {
    paintCounter++;
    Dimension currentSize = getSize();
    if ((this.mWidth != (int)currentSize.getWidth())
        || (this.mHeight != (int)currentSize.getHeight())) {
      this.mWidth = (int) currentSize.getWidth();
      this.mHeight = (int) currentSize.getHeight();
      this.offscreen = createImage(this.mWidth, this.mHeight);
    }

    if (this.offscreen == null) {
      this.offscreen = createImage(this.mWidth, this.mHeight);
    }

    Graphics og = this.offscreen.getGraphics();
    og.setClip(0, 0, this.mWidth, this.mHeight);
    paintOS(og);

    g.drawImage(this.offscreen, 0, 0, null);
    og.dispose();
  }

  public void paintOS(Graphics g) {
    long paintstart = System.currentTimeMillis();
    Graphics2D g2 = null ;
    if( g instanceof Graphics2D ) {
      g2 = (Graphics2D) g;
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING,
          RenderingHints.VALUE_TEXT_ANTIALIAS_ON); 
    }
    else {
      return;
    }

    g2.setColor(Color.BLACK);
    g2.fillRect(0, 0, this.mWidth, this.mHeight);

    paintAllNodes( g2 );
    paintAllConnections ( g2 );
    
    long paintend = System.currentTimeMillis();
    g2.setColor(java.awt.Color.GREEN.darker());
    g2.setFont(this.myPlain11Font);
    String s = "g2.paint(): " + (paintend - paintstart) + " ms, "+paintCounter+"paints "+mMain.getModel().getAllNodes().size()+"nodes"; //$NON-NLS-1$ //$NON-NLS-2$
    g2.drawString(s, this.mWidth - 250, this.mHeight - 8);
  }
  
  
  /**
   * paintAllNodes
   * @param g2
   */
  private void paintAllNodes( Graphics2D g2 ) {
    synchronized( mMain.getModel().getAllNodes() ) {
      
    }
  }
  
  
  /**
   * paintAllConnections
   * @param g2
   */
  private void paintAllConnections( Graphics2D g2 ) {
    synchronized( mMain.getModel().getAllConnections() ) {
      
    }
  }
  
  
  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void keyPressed(KeyEvent arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void keyReleased(KeyEvent arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void keyTyped(KeyEvent arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mouseDragged(MouseEvent arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mouseMoved(MouseEvent arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent arg0) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mouseExited(MouseEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mousePressed(MouseEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    // TODO Auto-generated method stub
    
  }


}
