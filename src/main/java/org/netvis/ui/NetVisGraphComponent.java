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
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
  
  private transient Image                offscreen;
  private int                            paintCounter;
  private Font                           myPlain10Font;
  private Font                           myPlain11Font;
  private int                            mWidth;
  private int                            mHeight;
  private static final BasicStroke       mStroke1 = new BasicStroke( 1 );
  private static final BasicStroke       mStroke2 = new BasicStroke( 2 );
  private static final BasicStroke       mStroke3 = new BasicStroke( 3 );
  private static final BasicStroke       mStroke4 = new BasicStroke( 4 );
  private static final BasicStroke       mStroke5 = new BasicStroke( 5 );
  private static final BasicStroke       mStroke6 = new BasicStroke( 6 );
  private static final BasicStroke       mStroke7 = new BasicStroke( 7 );
  private static final BasicStroke       mStroke8 = new BasicStroke( 8 );;
  private static final BasicStroke       mStroke9 = new BasicStroke( 9 );
  private static final BasicStroke[]     mStrokes = new BasicStroke[]{ mStroke1, mStroke2, mStroke3,
                                                      mStroke4, mStroke5, mStroke6,
                                                      mStroke7, mStroke8, mStroke9 };

  private boolean mAnonDisplay;
  public void toggleAnonDisplay() { mAnonDisplay = !mAnonDisplay; repaint(); }
  
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
  
  // layouting
  private double standardCharge;
  private double standardSpring;
  
 
  /**
   * <ctor>
   * @param main
   */
  public NetVisGraphComponent( NetVisMain main ) {
    logger.debug("NetVisComponent<ctor> called.");
    mMain = main;
    this.myPlain10Font = new Font("Courier", Font.PLAIN, 10);
    this.myPlain11Font = new Font("Courier", Font.PLAIN, 11);  
    
    mAnonDisplay = false;
    
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
    
    standardCharge = 15000.0;
    standardSpring = 0.05;
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
    
    paintAllConnections ( g2 );
   
    paintAllNodes( g2 );
    
    long paintend = System.currentTimeMillis();
    g2.setColor(java.awt.Color.GREEN.darker());
    g2.setFont(this.myPlain11Font);
    String s = "g2.paint(): " + (paintend - paintstart) + " ms, "+paintCounter+"paints "+mMain.getModel().getAllNodes().size()+"nodes"; //$NON-NLS-1$ //$NON-NLS-2$
    g2.drawString(s, this.mWidth - 250, this.mHeight - 8);
  }
  
  /**
   * initial layout, e.g. three circles
   */
  private void checkInitialLayoutNodes() {
    synchronized( mAllGraphNodes ) {
      if( mAllGraphNodes.size() > 0 ) {

        int nrOfLocalNodesToLayout = 0;
        int nrOfMulticastNodesToLayout = 0;
        int nrOfOtherNodesToLayout = 0;

        for (NetVisGraphNode nvgn : mAllGraphNodes ) {
          if( ( !nvgn.isInitiallyLayouted() ) 
              && ( !nvgn.isManuallyMoved() ) 
              && ( !nvgn.isAutomatiallyMoved()) )  {
            if( nvgn.getNode().getAddr().isMulticastAddress() ) {
              nrOfMulticastNodesToLayout++;
            } else if( nvgn.getNode().getAddr().isSiteLocalAddress() ) {
              nrOfLocalNodesToLayout++;
            } else {
              nrOfOtherNodesToLayout++;
            }
          }
        }

        int xCenter = mWidth / 2;
        int yCenter = mHeight / 2;

        double radiusMulticastNodesX = (double)mWidth * 0.1;
        double radiusMulticastNodesY= (double)mHeight * 0.1;
        double radiusLocalNodesX = (double)mWidth * 0.45;
        double radiusLocalNodesY = (double)mHeight * 0.45;
        double radiusOtherNodesX = (double)mWidth * 0.3;
        double radiusOtherNodesY = (double)mHeight * 0.3;

        double nrMulticastNode = 0.0;
        double nrLocalNode = 0.0;
        double nrOtherNode = 0.0;

        for (NetVisGraphNode nvgn : mAllGraphNodes ) {
          if( ( !nvgn.isInitiallyLayouted() ) 
              && ( !nvgn.isManuallyMoved() ) 
              && ( !nvgn.isAutomatiallyMoved()) ) {
            if( nvgn.getNode().getAddr().isMulticastAddress() ) {
              if( nrOfMulticastNodesToLayout > 0) {
                double angle = 2* Math.PI / nrOfMulticastNodesToLayout * nrMulticastNode;
                nvgn.setMx( (int)( xCenter + radiusMulticastNodesX * Math.sin ( angle )) );
                nvgn.setMy( (int)( yCenter + radiusMulticastNodesY * Math.cos ( angle )) );
                nrMulticastNode+=1.0;
                nvgn.setIsInitiallyLayouted( true );
              } 
            } else if( nvgn.getNode().getAddr().isSiteLocalAddress() ) {
              if( nrOfLocalNodesToLayout > 0) {
                double angle = 2* Math.PI / nrOfLocalNodesToLayout * nrLocalNode;
                nvgn.setMx( (int)( xCenter + radiusLocalNodesX * Math.sin ( angle )) );
                nvgn.setMy( (int)( yCenter + radiusLocalNodesY * Math.cos ( angle )) );
                nrLocalNode+=1.0;
                nvgn.setIsInitiallyLayouted( true );
              }
            } else {
              if( nrOfOtherNodesToLayout > 0) {
                double angle = 2* Math.PI / nrOfOtherNodesToLayout * nrOtherNode;
                nvgn.setMx( (int)( xCenter + radiusOtherNodesX * Math.sin ( angle )) );
                nvgn.setMy( (int)( yCenter + radiusOtherNodesY * Math.cos ( angle )) );
                nrOtherNode+=1.0;
                nvgn.setIsInitiallyLayouted( true );
              }
            } 
          } 
        }
      }
    }
  }
  
  
  /**
   * resets the isInitialLayouted for all nodes.
   * needed when resizing JFrame
   */
  private void revertAllLayoutNodes() {
    synchronized( mAllGraphNodes ) { 
      for (NetVisGraphNode nvgn : mAllGraphNodes ) {
        nvgn.setIsInitiallyLayouted( false );
      }
    }
  }

  
  /**
   * automatic layouting
   */
  public void incCharge() { standardCharge *= 1.6; }
  public void decCharge() { standardCharge /= 1.6; }
  public void incSpring() { standardSpring *= 1.6; }
  public void decSpring() { standardSpring /= 1.6; }
  Timer timer;
  int mLayoutCounter;
  public void startLayouting() {
    logger.info("startLayouting(): {} nodes, {} connections", mAllGraphNodes.size(), mAllGraphConnections.size());
    timer = new Timer("Timer");
    mLayoutCounter = 0;
    TimerTask task = new TimerTask() {
      public void run() {
        
        // logger.info("startLayouting::run() {} nodes, {} connections, count= {}", mAllGraphNodes.size(), mAllGraphConnections.size(), mLayoutCounter);
        long layoutstart = System.currentTimeMillis();
        
        synchronized ( mAllGraphNodes ) {
          synchronized ( mAllGraphConnections ) {
            
            for( NetVisGraphNode n : mAllGraphNodes ) {
              n.fx = 0.0;
              n.fy = 0.0;
            }
            
            // calculate electrostatic force for each node
            for( NetVisGraphNode n : mAllGraphNodes ) {
              // need to work through all other nodes:   
              for( NetVisGraphNode m : mAllGraphNodes ) {
                if( n != m ) {
                  double dx = n.getX() - m.getX();
                  double dy = n.getY() - m.getY();
                  double d2 = dx*dx + dy*dy;

                  if( d2 > 0 ) {
                    double d = Math.sqrt ( d2 );  // <-- the most expensive line 
                    double nx = dx / d;
                    double ny = dy / d;
                    
                    double localCharge = standardCharge;
                    if( ( n.getCategory() != 0 ) && ( m.getCategory() == n.getCategory() ) ) {
                      localCharge = standardCharge / 2;
                    }
                    
                    n.fx += localCharge * nx / d2;
                    n.fy += localCharge * ny / d2;
                  }
                }
              } // m
            } // n
            
            // calculate spring force for each node
            // need to work through all connections:
            for ( NetVisGraphConnection c : mAllGraphConnections ) {
              NetVisGraphNode src = c.getSrcGraphNode();
              NetVisGraphNode dst = c.getDstGraphNode();

              double dx = src.getX() - dst.getX();
              double dy = src.getY() - dst.getY();

              double mySpring = standardSpring * (1.0 + c.getConnection().getNrOfSeenPackets() / 1000.0 );
              if( mySpring > 50.0) {
                mySpring = 50.0;
              }
              src.fx -= mySpring * dx ;
              src.fy -= mySpring * dy ;
              dst.fx += mySpring * dx ;
              dst.fy += mySpring * dy ;
            } // c
            // all forces calculated.
            
            // apply all individual forces to all individual nodes
            double maxForce = 50.0; // avoid too big jumps.
            for( NetVisGraphNode n : mAllGraphNodes ) {
              if( n.canFlow() ) {
                if( n.fx > maxForce ) {
                  n.fx = maxForce;
                }
                if( n.fx < -maxForce ) {
                  n.fx = -maxForce;
                }
                if( n.fy > maxForce ) {
                  n.fy = maxForce;
                }
                if( n.fy < -maxForce ) {
                  n.fy = -maxForce;
                }

                // apply force to a given node.
                // for now, do not do the full numerical approach to the 
                // partial differential equation, "F=m*a" :
                // Simply move the node. (we do not need a full oscillation here)

                // x
                n.setX( n.getX() + n.fx ) ;
                if( n.getMx() > (mWidth - n.getWidth())) {
                  n.setMx( mWidth - n.getWidth() );
                }
                if( n.getMx() < 0 ) {
                  n.setMx( 0 );
                }

                // y
                n.setY( n.getY() + n.fy  ) ;
                if( n.getMy() > (mHeight - n.getHeight())) {
                  n.setMy( mHeight - n.getHeight() );
                }
                if( n.getMy() < 0 ) {
                  n.setMy( 0 );
                }

                n.setIsAutomaticallyMoved(true);
              }
            } // n
          } // sync allConnections
        } // sync all Nodes
        
        repaint();
        long layoutTime = System.currentTimeMillis() - layoutstart;
        if( layoutTime > 20 ) { // ms
          logger.info("MyLayouter.run() layout: {} ms, {} nodes, {} connections", layoutTime, mAllGraphNodes.size(), mAllGraphConnections.size());
        }
        
        mLayoutCounter++;
        if(( mLayoutCounter % 600 ) == 0 ) {
          logger.info("MyLayouter.run() layout: count:{}, charge: {}, spring {} ", mLayoutCounter, standardCharge, standardSpring);
        }
          
      }
    };
      
    long delay = 100L;
    timer.schedule(task, delay, delay);
    logger.info("startLayouting() done.");
  }
  
  public void stopLayouting() {
    timer.cancel();
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
    g2.setStroke(mStroke1);
    g2.setFont(this.myPlain11Font);
    synchronized( mAllGraphNodes ) {
      for (NetVisGraphNode nvgn : mAllGraphNodes ) {
        
        g2.setColor(Color.WHITE);
        
        if( nvgn.getNode().getAddr().isMulticastAddress() ) {
          g2.setColor(Color.BLUE);
        } 
        
        if( nvgn.getNode().getAddr().isSiteLocalAddress() ) {
          g2.setColor(Color.ORANGE);
        } 
        
        String s = nvgn.getDisplayString();
        if( mAnonDisplay ) {
          s = nvgn.getAnonDisplayString();
        }
        if( nvgn.getStringWidth() == -1) {
          int stringWidth = (int) g2.getFontMetrics().getStringBounds(s, g2).getWidth();
          nvgn.setStringWidth( stringWidth );
          nvgn.setWidth( stringWidth + 3 + 11);
        }

        int width = nvgn.getWidth();
        g2.drawRect(nvgn.getMx()+width-9, nvgn.getMy()+2, 6, 9);
        // draw pin
        if( nvgn.isManuallyMoved() ) {
          // pinned
          g2.drawLine(nvgn.getMx()+width-7, nvgn.getMy()+7, nvgn.getMx()+width-5, nvgn.getMy()+7); // h
          g2.drawLine(nvgn.getMx()+width-6, nvgn.getMy()+7, nvgn.getMx()+width-6, nvgn.getMy()+9); // v
        } else {
          // unpinned
          g2.drawLine(nvgn.getMx()+width-7, nvgn.getMy()+4, nvgn.getMx()+width-5, nvgn.getMy()+4); // h
          g2.drawLine(nvgn.getMx()+width-6, nvgn.getMy()+4, nvgn.getMx()+width-6, nvgn.getMy()+9); // v
        }
        
        if ( nvgn.getNode().getSentPackets() > 0 ) { 
          // draw sending circle
          Color c = g2.getColor();
          int radius =  nvgn.getNode().getSentPackets() / 20 + 1;
          if(  radius > 25 ) {
            radius = 25;
          } 
          g2.setColor(Color.GREEN);
          g2.drawOval(nvgn.getMx()-radius, nvgn.getMy()-radius, 2*radius, 2*radius);
          g2.setColor(c);
        }
        if ( nvgn.getNode().getReceivedPackets() > 0 ) {
          // draw receiving circle
          Color c = g2.getColor();
          int radius =  nvgn.getNode().getSentPackets() / 20 + 1;
          if(  radius > 25 ) {
            radius = 25;
          } 
          g2.setColor(Color.GREEN);
          g2.drawOval(nvgn.getMx() + nvgn.getWidth() -radius , nvgn.getMy()-radius, 2*radius, 2*radius);
          g2.setColor(c);
        }
        

        // paint the node:
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
        else if( nvgn.getLod() == 2 ) {
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
          g2.fillRect(nvgn.getMx(), nvgn.getMy() + 33, timeDiagramWidth, timeDiagramHeight);
          g2.setColor(Color.CYAN);
          g2.drawRect(nvgn.getMx(), nvgn.getMy() + 33, timeDiagramWidth, timeDiagramHeight);

          // paint mini-diagram:
          synchronized ( mMain.getModel().getAllPackets() ) {  
            g2.setStroke(mStroke1);
            g2.setColor(Color.CYAN);
            long diagramDuration = 900000;
            for (Packet p : mMain.getModel().getAllPackets() ) {
              if( ( p.getSrc() == nvgn.getNode() ) || ( p.getDst() == nvgn.getNode() )) {
                long now = System.currentTimeMillis();
                long age = now - p.getTs();

                if( age < diagramDuration ) {  
                  int x = (timeDiagramWidth - (int)(age * timeDiagramWidth / diagramDuration)) + nvgn.getMx();
                  int yTop = timeDiagramHeight - p.getSize()/5;
                  if( yTop <  0 ) {
                    yTop = 0;
                  }
                  yTop += nvgn.getMy() + 33 ;
                  if( yTop < nvgn.getMy()) {
                    yTop = nvgn.getMy();
                  }

                  int yBottom = timeDiagramHeight + nvgn.getMy()  + 33;
                  g2.drawLine( x, yTop, x, yBottom );  
                }
              }
            }
            g2.setColor(Color.CYAN);
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
    long now = System.currentTimeMillis();
    synchronized( mAllGraphConnections ) {
      g2.setColor(Color.GREEN);
      for (NetVisGraphConnection nvgc : mAllGraphConnections ) {
        NetVisGraphNode src = nvgc.getSrcGraphNode();
        NetVisGraphNode dst = nvgc.getDstGraphNode();

        if(( src!=null) && ( dst!=null )) {
          int packets = nvgc.getConnection().getNrOfSeenPackets();

          int size = 1;
          if( packets > 2 ) {
            if( packets > 5 ) {
              if( packets > 10 ) {
                if( packets > 50 ) {
                  if( packets > 100 ) {
                    if( packets > 500 ) {
                      if( packets > 1000 ) {
                        size = 8;
                      } else {
                       size = 7;
                      }
                    } else {
                      size = 6;
                    }
                  } else {
                    size = 5;
                  }
                } else {
                  size = 4;
                }
              } else {
                size = 3;
              }
            } else {
              size = 2;
            }
          } else {
            size = 1;
          }

          ShortestPair xp = getShortestPair(src.getMx(),  src.getMx() + src.getWidth(), dst.getMx(),  dst.getMx() + dst.getWidth()); 
          ShortestPair yp = getShortestPair(src.getMy(),  src.getMy() + src.getHeight(), dst.getMy(), dst.getMy() + dst.getHeight()); 

          Color c = getColorOfLinkAge( nvgc.getConnection().getTimeSinceLastSeenPacket(), true );
          g2.setColor(c);

          double offset = 0.0;
          if( nvgc.getConnection().getReverseConnection() != null ){
            offset = 10.0;
          }
          
          double travelFraction = ( now - nvgc.getConnection().getTimeOfLastSeenPacket() ) / 1000.0;
          if ( travelFraction > 1.0 ) {
            travelFraction = 1.0;
          }
          if ( travelFraction < 0.0 ) { // strange...
            travelFraction = 0.0;
          }
          
          drawParabel( g2, xp.s1, yp.s1, xp.s2, yp.s2, size, offset, travelFraction, nvgc.getConnection().getSizeOfLastSeenPacket() );
        }
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
          if( ( e.getX() >= ( n.getMx() + n.getWidth() - 10 ) ) && 
              ( e.getX() <= ( n.getMx() + n.getWidth() - 2 ) ) &&
              ( e.getY() >= ( n.getMy() + 3  )) &&
              ( e.getY() <= ( n.getMy() + 10 ))) {
            // pin-rectangle hit:
            n.setIsManuallyMoved( !n.isManuallyMoved() );
          } else {
            // pin-rectangle not hit
            n.setIsManuallyMoved( true );
          }
          repaint();
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
        
        mSelectedNode = n;
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
  
  public static void drawParabel( Graphics2D g2, int xStart, int yStart, int xEnd, int yEnd, int size, double shift, double travelFraction, int packetSize) {
    if ((xStart == xEnd) && (yStart == yEnd)) {
      return;
    }

    if( size < mStrokes.length ) {
      if( size > 0 ){
        g2.setStroke( mStrokes[ size-1 ] );
      } else {
        g2.setStroke( mStrokes[ 0 ] );
      }
    } else {
      g2.setStroke( mStrokes[ mStrokes.length-1 ] );
    }

    int boxSize = packetSize / 100 + 3;

    double arrowLen = size*5.0;
    double arrowWith = size*2.0;

    if( shift == 0.0 ) {
      drawArrow(g2, xStart, yStart, xEnd, yEnd, size, arrowLen, arrowWith);
      if( travelFraction < 1.0 ) {
        int xPacket = (int)(travelFraction * (double)( xEnd - xStart ) + xStart);
        int yPacket = (int)(travelFraction * (double)( yEnd - yStart ) + yStart);
        g2.setColor(Color.WHITE);
        g2.fillRect(xPacket-boxSize/2, yPacket-boxSize/2, boxSize, boxSize);
      }
    } else {
      double xNormale = (yEnd - yStart);
      double yNormale = -(xEnd - xStart);
      double len = Math.sqrt(xNormale * xNormale + yNormale * yNormale);
      if( len > 0 ) {
        xNormale /= len;
        yNormale /= len;
      }
      int x1 = xStart;
      int y1 = yStart;
      int x = 0;
      int y = 0;

      // drawParabel
      int steps = (int)(len/10.0 + 1.0);
      double dv = 1.0 / steps;
      for ( int step = 0; step <= steps; step++) {
        double v = (double)( step ) / (double)(steps);
        double u = 1.0 - 4.0 * (v - 0.5) * (v - 0.5);
        x = (int) ((double)(xEnd - xStart) * v + (double)xStart + u * shift * xNormale);
        y = (int) ((double)(yEnd - yStart) * v + (double)yStart + u * shift * yNormale);
        if( step < steps ) {
          // draw all segments but the last
          g2.drawLine(x1, y1, x, y);

          if((  v <= travelFraction ) && ( travelFraction < ( v+dv ))) {
            Color c = g2.getColor();
            int xPacket = (int)(travelFraction * (double)( x - x1 ) + x1);
            int yPacket = (int)(travelFraction * (double)( y - y1 ) + y1);
            g2.setColor(Color.WHITE);
            g2.fillRect(xPacket-boxSize/2, yPacket-boxSize/2, boxSize,  boxSize);
            g2.setColor(c);
          }

          x1 = x;
          y1 = y;
        }
      }
      
      // draw last segment as an arrow:
      drawArrow(g2, x1, y1, xEnd, yEnd, size, arrowLen, arrowWith);
      
      if((  (1.0-dv ) <= travelFraction ) && ( travelFraction < 1.0 )) {
        Color c = g2.getColor();
        int xPacket = (int)(travelFraction * (double)( xEnd - x1 ) + x1);
        int yPacket = (int)(travelFraction * (double)( yEnd - y1 ) + y1);
        g2.setColor(Color.WHITE);
        g2.fillRect(xPacket-boxSize/2, yPacket-boxSize/2, boxSize,  boxSize);
        g2.setColor(c);
      }
      
    }
  }
  
  private static void drawArrow( Graphics2D g2, int xStart, int yStart, int xEnd, int yEnd, 
                                    int size,   double arrowLen, double arrowWith) {
    double xNormale = (yEnd - yStart);
    double yNormale = -(xEnd - xStart);
    double len = Math.sqrt(xNormale * xNormale + yNormale * yNormale);
    if( len > 0 ) {
      xNormale /= len;
      yNormale /= len;
    }
    
    if( len > arrowLen ) {
      int xShortEnd = (int)(xEnd + yNormale * arrowLen );
      int yShortEnd = (int)(yEnd - xNormale * arrowLen );
      g2.drawLine(xStart, yStart, xShortEnd, yShortEnd);
    }
        
    int[] x = new int[3];
    int[] y = new int[3];
    
    // drawArrowTip:
    x[0] = (int)(xEnd + yNormale * arrowLen + xNormale * arrowWith );
    y[0] = (int)(yEnd - xNormale * arrowLen + yNormale * arrowWith );
    x[1] = xEnd ;
    y[1] = yEnd ;
    x[2] = (int)(xEnd + yNormale * arrowLen - xNormale * arrowWith );
    y[2] = (int)(yEnd - xNormale * arrowLen - yNormale * arrowWith );
    g2.fillPolygon(x, y, 3);
  }
  
  
  
  private class ShortestPair {
    private int s1;
    private int s2;
    ShortestPair(int tmps1, int tmps2) {
      s1 = tmps1;
      s2 = tmps2;
    }
  }
  
  private  ShortestPair getShortestPair( int p1, int p2, int q1, int q2) {
    int r1;
    int r2;

    int d1 = Math.abs(p1 - q1);
    r1 = p1;
    r2 = q1;
    int d2 = Math.abs(p1 - q2);
    if( d2 < d1 ) {
      // r1 is already p1
      r2 = q2;
    }
    int d3 = Math.abs(p2 - q1);
    if(( d3 < d2 ) && ( d3 < d1 )) {
      r1 = p2;
      r2 = q1;
    }
    int d4 = Math.abs(p2 - q2);
    if( ( d4 < d3 ) && ( d4 < d2 ) && ( d4 < d1 )) {
      r1 = p2;
      r2 = q2;
    }

    ShortestPair sp = new ShortestPair( r1, r2 );
    return ( sp );
  }


  private Color getColorOfLinkAge( long millis, boolean isActive ) { 
    if( isActive ) {

      if ( !mMain.isOnline() ) {
        millis = 1000;
      }
      // float hue;
      double fsFract =  Math.exp( -(double)millis / 100000.0 ); // 1.0 ... 0.0
      // hue = (float) ( 0.6666 - fsFract * 2.0 / 3.0);
      double bright  = fsFract * 0.5 + 0.5;  // 1.0 ... 0.5

      // Color bgc = Color.getHSBColor(hue, (float)1.0, (float)b);
      Color bgc = Color.WHITE;
      if( fsFract > 0.5 ) {
        double hue = 0.0; // constant red
        double sat = - 1.0 + 2.0 * fsFract ; // 1.0 ... 0.0
        bgc = Color.getHSBColor((float)hue, (float)sat, (float)bright);
      } else {
        double hue = 0.66666;  // constant blue
        double sat = 1.0 - 2.0 * fsFract; // 0.0 ... 1.0
        bgc = Color.getHSBColor((float)hue, (float)sat, (float)bright);
      }

      return ( bgc );
    } else {
      return ( Color.DARK_GRAY.darker().darker() );
    }
  }
}
