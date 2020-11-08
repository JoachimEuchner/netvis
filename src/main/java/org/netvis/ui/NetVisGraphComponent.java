package org.netvis.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
import org.netvis.model.Packet;
import org.netvis.util.ReverseIterator;
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
  private int                                 paintCounter;
  private Font                                myPlain10Font;
  private Font                                myPlain11Font;
  private int                                 mWidth;
  private int                                 mHeight;
  private final BasicStroke                   mStroke1;
  private final BasicStroke                   mStroke2;
  private final BasicStroke                   mStroke3;
  private final BasicStroke                   mStroke4;
  private final BasicStroke                   mStroke5;
  private final BasicStroke                   mStroke6;
  private final BasicStroke                   mStroke7;
  private final BasicStroke                   mStroke8;

  private final ArrayList<NetVisGraphNode> mAllGraphNodes;
  public List<NetVisGraphNode> getAllGraphNodes() { return mAllGraphNodes; }
  
  private final ArrayList<NetVisGraphConnection> mAllGraphConnections;
  public List<NetVisGraphConnection> getAllGraphConnections() { return mAllGraphConnections; }
  
  // selecting
  private transient NetVisGraphNode mSelectedNode = null;
  
  // dragging
  private transient NetVisGraphNode mDraggingNode = null;
  private int mDraggingNodeDx = 0;
  private int mDraggingNodeDy = 0; 
  
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
    
    this.addComponentListener( new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        revertAllLayoutNodes();
      }
    });
    
    this.mAllGraphNodes = new ArrayList<>(1000);
    this.mAllGraphConnections =  new ArrayList<>(1000);
    
    mDraggingNode = null;
    mDraggingNodeDx = 0;
    mDraggingNodeDy = 0;
    
    this.myPlain10Font = new Font("Courier", Font.PLAIN, 10);
    this.myPlain11Font = new Font("Courier", Font.PLAIN, 11);
    this.mStroke1                            = new BasicStroke( 1 );
    this.mStroke2                            = new BasicStroke( 2 );
    this.mStroke3                            = new BasicStroke( 3 );
    this.mStroke4                            = new BasicStroke( 4 );
    this.mStroke5                            = new BasicStroke( 5 );
    this.mStroke6                            = new BasicStroke( 6 );
    this.mStroke7                            = new BasicStroke( 7 );
    this.mStroke8                            = new BasicStroke( 8 );
   
  }
  
  
  public void clear() {
    logger.debug("NetVisComponent.clear() called.");
    synchronized ( mAllGraphNodes ) {
      this.mAllGraphNodes.clear();
    }
    synchronized ( mAllGraphConnections ) {
      this.mAllGraphConnections.clear();
    }
    repaint();
  }
  
  
  public void addGraphNode( NetVisGraphNode nvgn ) {
    synchronized ( mAllGraphNodes ) {
      mAllGraphNodes.add(nvgn);
      revertAllLayoutNodes();
    }
  }
  
  
  public void addGraphConnection( NetVisGraphConnection nvgc ) {
    synchronized ( mAllGraphConnections ) {
      mAllGraphConnections.add(nvgc);
    }
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

    checkInitialLayoutNodes();
    
    paintAllNodes( g2 );
    paintAllConnections ( g2 );
    
    long paintend = System.currentTimeMillis();
    g2.setColor(java.awt.Color.GREEN.darker());
    g2.setFont(this.myPlain11Font);
    String s = "g2.paint(): " + (paintend - paintstart) + " ms, "+paintCounter+"paints "+mMain.getModel().getAllNodes().size()+"nodes"; //$NON-NLS-1$ //$NON-NLS-2$
    g2.drawString(s, this.mWidth - 250, this.mHeight - 8);
  }
  
  
  private void checkInitialLayoutNodes() {
    synchronized( mAllGraphNodes ) {
      int nrOfNodesToLayout = mAllGraphNodes.size();

      int xCenter = mWidth / 2;
      int yCenter = mHeight / 2;

      double radius = mHeight * 0.4;

      double i = 0.0;
      if( nrOfNodesToLayout > 0 ) {
        for (NetVisGraphNode nvgn : mAllGraphNodes ) {
          if( ( !nvgn.isInitiallyLayouted() ) 
              && ( !nvgn.isManuallyMoved() ) ) {
            double angle = 2* Math.PI / nrOfNodesToLayout * i;

            nvgn.setMx( (int)( xCenter + radius * Math.sin ( angle )) );
            nvgn.setMy( (int)( yCenter + radius * Math.cos ( angle )) );
          }
          i += 1.0;
          nvgn.setIsInitiallyLayouted( true );
        }
      }
    }
  }
  
  
  /**
   * resets the isLayouted for all nodes.
   */
  private void revertAllLayoutNodes() {
    synchronized( mAllGraphNodes ) { 
      for (NetVisGraphNode nvgn : mAllGraphNodes ) {
        nvgn.setIsInitiallyLayouted( false );
      }
    }
  }


  /**
   * get the top (z-sorted) NetVisGraphNode at x,y 
   * @param x
   * @param y
   * @return
   */
  public NetVisGraphNode getNode( int x, int y ) {
    NetVisGraphNode resultNode = null;
    synchronized( mAllGraphNodes ) {
      for (NetVisGraphNode nvgn : new ReverseIterator<NetVisGraphNode>(mAllGraphNodes )) {
        if ( nvgn.contains(x, y) ) {
          resultNode = nvgn;
          break;
        }
      }
    }   
    return ( resultNode );
  }
  
  
  /**
   * paintAllNodes
   * @param g2
   */
  private void paintAllNodes( Graphics2D g2 ) {
    g2.setColor(Color.WHITE);
    g2.setFont(this.myPlain11Font);
    synchronized( mAllGraphNodes ) {
      for (NetVisGraphNode nvgn : mAllGraphNodes ) {
        String s = nvgn.getDisplayString();
        if( nvgn.getStringWidth() == -1)
        {
          int width = (int) g2.getFontMetrics().getStringBounds(s, g2).getWidth();
          nvgn.setStringWidth( width );
          nvgn.setWidth( width + 3);
        }

        if( nvgn.getLod() == 0 ) {
          // simple plain hostname
          nvgn.setHeight(13);
          g2.drawRect(nvgn.getMx(),nvgn.getMy(),nvgn.getWidth(),nvgn.getHeight());
          g2.drawString(s, nvgn.getMx()+2,nvgn.getMy()+11);   // hostname
        }
        else if( nvgn.getLod() == 1 ) {
          nvgn.setHeight(13+10);
          String s2 =  nvgn.getNode().getAddr().toString();
          g2.drawRect(nvgn.getMx(),nvgn.getMy(),nvgn.getWidth(),nvgn.getHeight());
          g2.drawString(s, nvgn.getMx()+2,nvgn.getMy()+11); // plain hostname
          g2.drawString(s2, nvgn.getMx()+2,nvgn.getMy()+21); // Inet4Address.toString
        }
        else if( nvgn.getLod() == 2 )
        {
          // plain hostname
          // Inet4Address.toString
          // small diagram
          int timeDiagramWidth = 300;
          int timeDiagramHeight = 100;

          nvgn.setHeight(13+20);

          g2.drawString(s, nvgn.getMx()+2,nvgn.getMy()+11);
          String s2 =  nvgn.getNode().getAddr().toString();  // hostname
          g2.drawString(s2, nvgn.getMx()+2,nvgn.getMy()+21);
          String s3 = "tx:" + nvgn.getNode().getSentPackets() + " rx:" + nvgn.getNode().getReceivedPackets();
          g2.drawString(s3, nvgn.getMx()+2, nvgn.getMy()+31 );
          g2.drawRect(nvgn.getMx(),nvgn.getMy(),nvgn.getWidth(),nvgn.getHeight());

          g2.setColor(Color.CYAN.darker().darker().darker());
          g2.fillRect(nvgn.getMx(), nvgn.getMy() + 31, timeDiagramWidth, timeDiagramHeight);
          g2.setColor(Color.CYAN);
          g2.drawRect(nvgn.getMx(), nvgn.getMy() + 31, timeDiagramWidth, timeDiagramHeight);

          synchronized ( mMain.getModel().getAllPackets() )
          {  
            g2.setStroke(mStroke1);
            g2.setColor(Color.CYAN);
            long diagramDuration = 900000;
            for (Packet p : mMain.getModel().getAllPackets() )
            {
              if( ( p.getSrc() == nvgn.getNode() ) || ( p.getDst() == nvgn.getNode() ))
              {
                long now = System.currentTimeMillis();
                long age = now - p.getTs();

                if( age < diagramDuration )
                {  
                  int x = (timeDiagramWidth - (int)(age * timeDiagramWidth / diagramDuration)) + nvgn.getMx();
                  int yTop = timeDiagramHeight - p.getSize()/5;
                  if( yTop <  0 )
                  {
                    yTop = 0;
                  }
                  yTop += nvgn.getMy() + 31 ;
                  if( yTop < nvgn.getMy())
                  {
                    yTop = nvgn.getMy();
                  }

                  int yBottom = timeDiagramHeight + nvgn.getMy()  + 31;

                  g2.drawLine( x, yTop, x, yBottom );  
                }
              }
            }
          }
          g2.setColor(Color.WHITE);
        }
      }
    }
  }
 
  
  
  /**
   * paintAllConnections
   * @param g2
   */
  private void paintAllConnections( Graphics2D g2 ) {
    synchronized( mAllGraphConnections ) {
      g2.setColor(Color.GREEN);
      for (NetVisGraphConnection nvgn : mAllGraphConnections ) {
        NetVisGraphNode src = nvgn.getSrcGraphNode();
        NetVisGraphNode dst = nvgn.getDstGraphNode();
        g2.drawLine(src.getMx(), src.getMy(), dst.getMx(), dst.getMy());
      }
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
    if ( this.mDraggingNode != null ) {
      this.mDraggingNode.setIsManuallyMoved( true );
      int x = arg0.getX();
      int y = arg0.getY();
      this.mDraggingNode.setMx( x - mDraggingNodeDx );
      this.mDraggingNode.setMy( y - mDraggingNodeDy );
    }
    repaint();
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

    if (e.getButton() == MouseEvent.BUTTON1) {
      if( e.getClickCount() == 1 ) {
        NetVisGraphNode n = getNode( e.getX(), e.getY() );
        if( n != null ) {
          mSelectedNode = n;
          n.setIsManuallyMoved(true);
        }
      }
    }
    else if (e.getButton() == MouseEvent.BUTTON3) {
      NetVisGraphNode n = getNode( e.getX(), e.getY() );
      if( n != null ) {
        if( e.getClickCount() == 1 ) {
          n.increaseLod();
        }
        else if(e.getClickCount() == 2 ) {
          n.resetLod();
        }
        repaint();
      }
    }
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    // tbd
  }

  @Override
  public void mouseExited(MouseEvent e) {
    // tbd
  }    

  
  @Override
  public void mousePressed(MouseEvent ev) {
    
    Dimension currentSize = getSize();
    this.mWidth = (int) currentSize.getWidth();
    this.mHeight = (int) currentSize.getHeight();

    requestFocusInWindow();
    
    this.mDraggingNode = null;
    this.mDraggingNodeDx = 0;
    this.mDraggingNodeDy = 0; 
    
    if (ev.getButton() == MouseEvent.BUTTON1) {
      NetVisGraphNode n = getNode( ev.getX(), ev.getY() );
      if( n != null ) {
        mDraggingNode = n;
        mDraggingNodeDx = ev.getX() - n.getMx();
        mDraggingNodeDy = ev.getY() - n.getMy();   // 0...mHeight
      }
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (e.getButton() == MouseEvent.BUTTON1) {
      this.mDraggingNodeDx = 0;
      this.mDraggingNodeDy = 0;
      this.mDraggingNode = null;
    }
  }


}
