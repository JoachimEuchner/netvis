package netvis.ui;

import java.awt.BasicStroke;
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
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netvis.NetVisMain;
import netvis.model.Model;
import netvis.model.Node;
import netvis.traceroute.TraceRouteNode;

public class NetVisComponent extends JComponent implements
   MouseListener, MouseWheelListener, MouseMotionListener, KeyListener,
   ActionListener
{
   private static final Logger logger = LoggerFactory.getLogger(NetVisComponent.class);

   
   public class ReverseIterator<T> implements Iterator<T>, Iterable<T> 
   {
      private final List<T> list;
      private int position;

      public ReverseIterator(List<T> list) {
          this.list = list;
          this.position = list.size() - 1;
      }

      @Override
      public Iterator<T> iterator() {
          return this;
      }

      @Override
      public boolean hasNext() {
          return position >= 0;
      }

      @Override
      public T next() {
          return list.get(position--);
      }

      @Override
      public void remove() {
          throw new UnsupportedOperationException();
      }
   }
   
   /**
    * 
    */
   private static final long serialVersionUID = -5194156817975062007L;

   transient Image                     offscreen;
   transient Toolkit                   mToolkit;
   
   private final NetVisMain            mMain;
   private final Model                 mModel;
   
   int                                 mWidth;
   int                                 mHeight;

   private Font                        myPlain10Font;
   private int                         mLineHeight;
  
   private final transient BasicStroke mStroke8;          
   private final transient BasicStroke mStroke7;
   private final transient BasicStroke mStroke6;
   private final transient BasicStroke mStroke5;
   private final transient BasicStroke mStroke4;
   private final transient BasicStroke mStroke3;
   private final transient BasicStroke mStroke2;
   private final transient BasicStroke mStroke1;
   
   
   private transient Node mSelectedNode = null;
   private transient Node mDraggingNode = null;
   private int mDraggingNodeDx = 0;
   private int mDraggingNodeDy = 0;
   
   
   private boolean doLayouting;
   private int paintCounter;
   
   private double standardCharge;
   private double standardSpring;
   
   public NetVisComponent( NetVisMain main, Model m )
   {
      logger.debug("NetVisComponent<ctor> called.");
      mMain = main;
      mModel = m;
      addMouseListener(this);
      addKeyListener(this);
      addMouseWheelListener(this);
      addMouseMotionListener(this);
      
      this.mToolkit = Toolkit.getDefaultToolkit();
      
      doLayouting = true;
      MyLayouter myLayouter = new MyLayouter( this );
      runLayouter( myLayouter );
      
      this.myPlain10Font = new Font("Courier", Font.PLAIN, 11); //$NON-NLS-1$
      this.mLineHeight = 11;
      
      this.mStroke8                            = new BasicStroke( 8 );
      this.mStroke7                            = new BasicStroke( 7 );
      this.mStroke6                            = new BasicStroke( 6 );
      this.mStroke5                            = new BasicStroke( 5 );
      this.mStroke4                            = new BasicStroke( 4 );
      this.mStroke3                            = new BasicStroke( 3 );
      this.mStroke2                            = new BasicStroke( 2 );
      this.mStroke1                            = new BasicStroke( 1 );

      mDraggingNode = null;
      mDraggingNodeDx = 0;
      mDraggingNodeDy = 0;
      
      standardCharge = 4000.0;
      standardSpring = 0.01;
      
      paintCounter = 0;
   }
   
   private void runLayouter(Runnable runnable)
   {
      // RecordingExceptionHandler handler = new RecordingExceptionHandler();
      Thread thread = new Thread(runnable);
      // thread.setUncaughtExceptionHandler(handler);
      thread.start();
//      try {
//          thread.join();
//      } catch (Throwable t) {
//          fail("Unexpected failure in child thread:" + t.getMessage());
//      }
//      assertFalse(handler.getMessage(), handler.hadException());
  }
   
   
   public void invalidate()
   {
      super.invalidate();
      this.offscreen = null;
   }

   @Override
   public void update( Graphics g)
   {
      paint(g);
   }

   @Override
   public void paint( Graphics g)
   {
      paintCounter++;
      Dimension currentSize = getSize();
      if ((this.mWidth != (int)currentSize.getWidth())
            || (this.mHeight != (int)currentSize.getHeight()))
      {
         this.mWidth = (int) currentSize.getWidth();
         this.mHeight = (int) currentSize.getHeight();
         this.offscreen = createImage(this.mWidth, this.mHeight);
      }


      if (this.offscreen == null)
      {
         this.offscreen = createImage(this.mWidth, this.mHeight);
      }

      Graphics og = this.offscreen.getGraphics();
      og.setClip(0, 0, this.mWidth, this.mHeight);
      paintOS(og);

      g.drawImage(this.offscreen, 0, 0, null);
      og.dispose();

   }

   public void paintOS(Graphics g)
   {
      long paintstart = System.currentTimeMillis();
      Graphics2D g2 = null ;
      if( g instanceof Graphics2D )
      {
         g2 = (Graphics2D) g;
         g2.setRenderingHint(
               RenderingHints.KEY_TEXT_ANTIALIASING,
               RenderingHints.VALUE_TEXT_ANTIALIAS_ON); 
      }
      else
      {
         return;
      }

      g2.setColor(Color.BLACK);
      g2.fillRect(0, 0, this.mWidth, this.mHeight);
      
      
      layoutNodes();
      
      paintAllLinks( g2 );
      paintAllNodes( g2 );
      
      
      long paintend = System.currentTimeMillis();
      g2.setColor(java.awt.Color.GREEN.darker());
      g2.setFont(this.myPlain10Font);
      String s = "g2.paint(): " + (paintend - paintstart) + " ms/"+paintCounter; //$NON-NLS-1$ //$NON-NLS-2$
      g2.drawString(s, this.mWidth - 200, this.mHeight - 8);
   }
   
   private class MyLayouter implements Runnable
   {
      private NetVisComponent mHost;
      public MyLayouter( NetVisComponent lvc )
      {
         mHost = lvc;
      }
      
      
      public void run()
      {
         logger.debug("MyLayouter.run() called.");
         
         try
         {
            Thread.sleep(2000);
         }
         catch( InterruptedException ie )
         {
            //...
         }
         logger.debug("MyLayouter.run() start layouting.");
       
         
         while ( mHost.doLayouting )
         {
            try
            {
               Thread.sleep(200);
            }
            catch( InterruptedException ie )
            {
               //...
            }

            if( mModel != null )
            {
               if( mModel.getAllNodes() != null  )
               {
                  logger.trace("MyLayouter.run() start layouting {} nodes", mModel.getAllNodes().size());
                                    
                  synchronized( mModel.getAllNodes() )
                  {
                     List<Node> allNodes = mModel.getAllNodes();
                     
                     for( Node n: new ReverseIterator<Node>(allNodes) )
                     {
                        n.fx = 0.0;
                        n.fy = 0.0;

                        if ( n.canFlow() )
                        {

                           double charge = standardCharge;
                           for (Node m : allNodes )
                           {
                              if( n != m )
                              {
                                 if( ( n.isActive() ) && ( m.isActive() ) )
                                 {
                                    double d2 = (n.getX() - m.getX())*(n.getX() - m.getX()) + (n.getY() - m.getY())*(n.getY() - m.getY());

                                    double localCharge = charge;
                                    if ( n.getAddressBytes()[0] == m.getAddressBytes()[0] ) 
                                    { 
                                       localCharge = standardCharge / 2.0;
                                       if  ( n.getAddressBytes()[1] == m.getAddressBytes()[1] ) 
                                       {
                                          localCharge = standardCharge / 10.0;
                                          if  ( n.getAddressBytes()[2] == m.getAddressBytes()[2] ) 
                                          {
                                             localCharge = standardCharge / 50.0;
                                          }
                                       }
                                    }


                                    if( d2 > 0 )
                                    {
                                       double d = Math.sqrt ( d2 );

                                       double nx = (n.getX() - m.getX()) / d;
                                       double ny = (n.getY() - m.getY()) / d;
                                       n.fx += localCharge * nx / d2;
                                       n.fy += localCharge * ny / d2;
                                    }
                                 }
                              }
                           }


                           double spring = standardSpring;
                           synchronized( mModel.getAllLinks() )
                           {
                              List<netvis.model.Link> mAllLinks = mModel.getAllLinks();

                              for (netvis.model.Link l : mAllLinks )
                              {
                                 Node s = l.getSrc();
                                 Node d = l.getDst();

                                 if( ( s!= null ) && (d!= null ))
                                 {
                                    if( ( s.isActive() ) && ( d.isActive() ) )
                                    {
                                       double mySpring = spring * (1.0 + l.getNrOfSeenPackets() / 1000.0 );
                                       if( mySpring > 0.5)
                                       {
                                          mySpring = 0.5;
                                       }
                                       if( n == s ) 
                                       {
                                          n.fx -=  mySpring * (n.getX() - d.getX()) ;
                                          n.fy -=  mySpring * (n.getY() - d.getY()) ;
                                       }


                                       if ( n == d )
                                       {
                                          n.fx -= mySpring * (n.getX() - s.getX()) ;
                                          n.fy -= mySpring * (n.getY() - s.getY()) ;
                                       }
                                    }
                                 }
                              }
                           }

                           if( n.fx > 50.0 )
                           {
                              n.fx = 50.0;
                           }
                           if( n.fx < -50.0 )
                           {
                              n.fx = -50.0;
                           }

                           n.setx( n.getX() + n.fx ) ;
                           n.setMx( (int)( n.getX() + 0.5) );
                           if( n.getMx() < 0 )
                           {
                              n.setMx( 0 );
                              n.setx( 0.0 );
                           }
                           if( n.getMx() > (mWidth - 100)  )
                           {
                              n.setMx( mWidth - 100) ;
                              n.setx( mWidth - 100.0 );
                           }

                           if( n.fy > 50.0 )
                           {
                              n.fy = 50.0;
                           }
                           if( n.fy < -50.0 )
                           {
                              n.fy = -50.0;
                           }

                           n.sety( n.getY() + n.fy );
                           n.setMy( (int)( n.getY() + 0.5) );
                           if( n.getMy() < 0 )
                           {
                              n.setMy( 0 );
                              n.sety( 0.0 );
                           }
                           if( n.getMy() > (mHeight - 10)  )
                           {
                              n.setMy( mHeight - 10);
                              n.sety( mHeight - 10.0 );
                           }
                        }
                     }
                  }
               }
            }

            mHost.repaint();
         }
      }
      
   }
   
   
   
   private void layoutNodes()
   {
      synchronized( mModel.getAllNodes() )
      {
         List<Node> allNodes = mModel.getAllNodes();
         
         int nrOfNodesToLayout = allNodes.size();
         
         int xCenter = mWidth / 2;
         int yCenter = mHeight / 2;
                 
         double radius = mHeight * 0.4;
                          
         double i = 0.0;
         if( nrOfNodesToLayout > 0 )
         {
            for (Node n : allNodes )
            {
               if( !n.mbIsInitialLayouted ) 
               {
                  double angle = 2* Math.PI / nrOfNodesToLayout * i;

                  n.setMx( (int)( xCenter + radius * Math.sin ( angle )) );
                  n.setMy( (int)( yCenter + radius * Math.cos ( angle )) );

                  n.setx( n.getMx() );
                  n.sety( n.getMy() );
               }
               i += 1.0;
               n.mbIsInitialLayouted = true;
            }
         }
      }
   }
   
   
   private void paintAllNodes( Graphics2D g2 )
   {
      synchronized( mModel.getAllNodes() )
      {
         g2.setFont(this.myPlain10Font);
         
         List<netvis.model.Node> allNodes = mModel.getAllNodes();
         
         
         int timeDiagramWidth = 300;
         int timeDiagramHeight = 100;
         
         
         g2.setColor(Color.WHITE);
         for ( netvis.model.Node n : allNodes )
         {
            String s = n.getDisplayName(); 
           
            if( true )//  !n.mbIsUserMoved  )
            {
               int width = g2.getFontMetrics().stringWidth(s);

               if( n.getLoD() == 0 )
               {
                  n.setWidth( 5 );
                  n.setHeight( 5 );
               }
               else if( n.getLoD() == 1 )
               {
                  n.setWidth ( width );
                  n.setHeight ( 1 * this.mLineHeight );
               } 
               else if( n.getLoD() == 2 )
               {
                  n.setWidth( width );
                  n.setHeight( 3 * this.mLineHeight );
               }
               else if( n.getLoD() == 3 )
               {
                  n.setHeight( timeDiagramHeight + 3 * this.mLineHeight);
                  n.setWidth( timeDiagramWidth );
               }
            }
            
            Color mainColor = Color.BLACK;
            Color textColor = Color.LIGHT_GRAY;
            Color borderColor = Color.GRAY.darker();
         
            
            if( n.getType() == Node.TYPE_ENDPOINT )
            {
               mainColor = Color.BLACK;
               borderColor = Color.YELLOW;
            }
            
            if( n.isLocal )
            {
               mainColor = Color.WHITE;
               textColor = Color.DARK_GRAY;
            }
            
            g2.setColor(mainColor);
            g2.fillRect(n.getMx(),n.getMy(),n.getWidth(),n.getHeight());

       
            if( n.getLoD() == 0 )
            {
               if( n.getType() == Node.TYPE_ROUTEPOINT) 
               {
                  try 
                  {
                     if( n instanceof TraceRouteNode)
                     {
                        int depth = ((TraceRouteNode)n).getDepth();
                        g2.setColor(textColor);
                        g2.drawString( Integer.toString(depth), n.getMx()+5, n.getMy()+ this.mLineHeight - 1);
                     }
                  }
                  catch (ClassCastException cce )
                  {
                     logger.error("cought {}", cce);
                  }
               }
            }
            else if( n.getLoD() > 0)
            {
               g2.setColor(textColor);
               g2.drawString(s, n.getMx(), n.getMy()+ this.mLineHeight - 1);
            }
            
            if( n.getLoD() > 1 )
            {
               String s2 = n.getAddr().toString();
               if( s2.startsWith("/"))
               {
                  s2 = s2.substring( 1, s2.length());
               }
               g2.drawString(s2, n.getMx(), n.getMy()+2*this.mLineHeight - 1);
               
               String s3 = "s" + n.getSentPackets() + ",r" + n.getReceivedPackets();
               g2.drawString(s3, n.getMx(), n.getMy()+3*this.mLineHeight - 1);
               
               if( n.getLoD() > 2 )
               {

                  g2.setColor(Color.CYAN.darker().darker().darker());
                  g2.fillRect(n.getMx(), n.getMy() + 3 * this.mLineHeight, timeDiagramWidth, timeDiagramHeight);
                                    
                  synchronized ( mModel.getAllPackets() )
                  {  
                     g2.setStroke(mStroke1);
                     g2.setColor(Color.CYAN);
                     long diagramDuration = 300000;
                     for (netvis.model.Packet p : mModel.getAllPackets() )
                     {
                        if( ( p.getSrc() == n ) || ( p.getDst() == n ))
                        {
                           long now = System.currentTimeMillis();
                           long age = now - p.getTs();

                           if( age < diagramDuration )
                           {  
                              int x = (timeDiagramWidth - (int)(age * timeDiagramWidth / diagramDuration)) + n.getMx();
                              int yTop = timeDiagramHeight - p.getSize()/5;
                              if( yTop <  0 )
                              {
                                 yTop = 0;
                              }
                              yTop += n.getMy() + 3*this.mLineHeight ;
                              if( yTop < n.getMy())
                              {
                                 yTop = n.getMy();
                              }

                              int yBottom = timeDiagramHeight + n.getMy()  + 3*this.mLineHeight;

                              g2.drawLine( x, yTop, x, yBottom );  
                           }
                        }
                     }
                  }
               }
            }
            
            g2.setStroke(mStroke1);
            g2.setColor(borderColor);
            g2.drawRect(n.getMx(),n.getMy(),n.getWidth(),n.getHeight());
            if( n == mSelectedNode )
            {
               g2.setColor(borderColor);
               g2.drawRect(n.getMx(),n.getMy(),n.getWidth(),n.getHeight());
               g2.drawRect(n.getMx()-1, n.getMy()-1, n.getWidth()+2, n.getHeight()+2);
               g2.drawRect(n.getMx()-2, n.getMy()-2, n.getWidth()+4, n.getHeight()+4);
            }
            
            if ( !n.canFlow() )
            {
               g2.setColor(borderColor);
               g2.fillRect(n.getMx(),n.getMy(),3,3);
            }
            
            if( !n.isActive() )
            {
               g2.drawLine( n.getMx(), n.getMy() + n.getHeight(), n.getMx()+n.getWidth(), n.getMy());
            }
            
         }
      }
   }
   
  
   private class ShortestPair
   {
      private int s1;
      private int s2;
      ShortestPair(int tmps1, int tmps2)
      {
         s1 = tmps1;
         s2 = tmps2;
      }
   }
   
   private ShortestPair getShortestPair( int p1, int p2, int q1, int q2)
   {
      int r1;
      int r2;
      
      int d1 = Math.abs(p1 - q1);
      r1 = p1;
      r2 = q1;
      int d2 = Math.abs(p1 - q2);
      if( d2 < d1 )
      {
         // r1 is already p1
         r2 = q2;
      }
      int d3 = Math.abs(p2 - q1);
      if(( d3 < d2 ) && ( d3 < d1 ))
      {
         r1 = p2;
         r2 = q1;
      }
      int d4 = Math.abs(p2 - q2);
      if( ( d4 < d3 ) && ( d4 < d2 ) && ( d4 < d1 ))
      {
         r1 = p2;
         r2 = q2;
      }
      
      ShortestPair sp = new ShortestPair( r1, r2 );
      return ( sp );
   }
   
   
   private Color getColorOfLinkAge( long millis, boolean isActive )
   { 
      if( isActive )
      {
         if ( !mMain.isOnline() )
         {
            millis = 1000;
         }

         float hue;
         double fsFract =  Math.exp( -(double)millis / 100000.0 );
         hue = (float) ( 0.6666 - fsFract * 2.0 / 3.0);
         double b  = fsFract * 0.5 +0.5;

         Color bgc = Color.getHSBColor(hue, (float)1.0, (float)b);

         return ( bgc );
      }
      else
      {
         return ( Color.DARK_GRAY.darker().darker() );
      }
   }
   
   
   
   private void paintAllLinks( Graphics2D g2 )
   {
      synchronized( mModel.getAllLinks() )
      {
         List<netvis.model.Link> allLinks = mModel.getAllLinks();
         
         g2.setColor(Color.BLACK);
                 
         for (netvis.model.Link l : allLinks )
         {
            Node s = l.getSrc();
            Node d = l.getDst();

            if(( s!=null) && ( d!=null ))
            {
               int packets = l.getNrOfSeenPackets();

               g2.setStroke(mStroke1);
               if( ( packets > 2 ) && ( s.isActive() ) && ( d.isActive() ) )
               {
                  g2.setStroke(mStroke2);
                  if( packets > 5 ) 
                  {
                     g2.setStroke(mStroke3);
                     if( packets > 10 ) 
                     {
                        g2.setStroke(mStroke4);
                        if( packets > 50 ) 
                        {
                           g2.setStroke(mStroke5);
                           if( packets > 100 ) 
                           {
                              g2.setStroke(mStroke6);
                              if( packets > 500 ) 
                              {
                                 g2.setStroke(mStroke7);
                                 if( packets > 1000 ) 
                                 {
                                    g2.setStroke(mStroke8);
                                 }
                              }
                           }
                        }
                     }
                  }
               }


               ShortestPair xp = getShortestPair(s.getMx(),  s.getMx() + s.getWidth(), d.getMx(),  d.getMx() + d.getWidth()); 
               ShortestPair yp = getShortestPair(s.getMy(),  s.getMy() + s.getHeight(), d.getMy(), d.getMy() + d.getHeight()); 

               Color c = getColorOfLinkAge( l.getTimeSinceLastSeenPacket(), (s.isActive() && d.isActive()) );
               g2.setColor(c);

               drawParabel( g2, xp.s1, yp.s1, xp.s2, yp.s2, 10 );
            }
         }
      }
   }
   
   
   public Node getNode( int x, int y )
   {
      Node resultNode = null;
      synchronized ( mModel.getAllNodes() )
      {
         List<Node> allNodes = mModel.getAllNodes();
         for( Node n: new ReverseIterator<Node>(allNodes) )
         {
            if ( n.contains(x, y) )
            {
               resultNode = n;
               break;
            }
         }
      }   
      return ( resultNode );
   }
   
   
   public static void drawParabel( Graphics2D g2, int xStart, int yStart, int xEnd, int yEnd, double shift)
   {
      if ((xStart == xEnd) && (yStart == yEnd))
      {
         return;
      }

      int x1 = xStart;
      int y1 = yStart;

      double xNormale = (yEnd - yStart);
      double yNormale = -(xEnd - xStart);
      double len = Math.sqrt(xNormale * xNormale + yNormale * yNormale);

      if( len > 0 )
      {
         double vStep = 5.0 / len ; 
         if( vStep > 0.5 )
         {
            vStep = 0.5;
         }

         xNormale /= len;
         yNormale /= len;

         for (double v = 0.0; v <= 1.001; v += vStep)
         {
            double u = 1.0 - 4.0 * (v - 0.5) * (v - 0.5);
            int x = (int) ((xEnd - xStart) * v + xStart) + (int) (u * shift * xNormale);
            int y = (int) ((yEnd - yStart) * v + yStart) + (int) (u * shift * yNormale);

            g2.drawLine(x1, y1, x, y);

            x1 = x;
            y1 = y;
         }
      }
   }
   
   


   
   @Override
   public void actionPerformed(ActionEvent arg0)
   {
      if (arg0.getActionCommand().equals("C++")) //$NON-NLS-1$
      {
         standardCharge = standardCharge*1.1;
         repaint();
      } 
      else if (arg0.getActionCommand().equals("C--")) //$NON-NLS-1$
      {
         standardCharge = standardCharge* (1.0/1.1);
         repaint();
      }
      else if (arg0.getActionCommand().equals("S++")) //$NON-NLS-1$
      {
         standardSpring = standardSpring*1.1;
         repaint();
      } 
      else if (arg0.getActionCommand().equals("S--")) //$NON-NLS-1$
      {
         standardSpring = standardSpring* (1.0/1.1);
         repaint();
      }
      
      
      logger.trace("Charge: {}, Spring: {}", standardCharge, standardSpring);

   }

   @Override
   public void keyPressed(KeyEvent e)
   {
      int code = e.getKeyCode();
   
      switch (code)
      {
         case KeyEvent.VK_SPACE:
            // start/stop layout
            break;
         
         case KeyEvent.VK_A:
            if ( mSelectedNode != null )
            {
               mSelectedNode.setActive( !mSelectedNode.isActive() );
            }
            break;
         
         case KeyEvent.VK_T:
            if ( mSelectedNode != null )
            {
               mMain.getTRScheduler().addTargetAddress(mSelectedNode.getAddr());
            }
            break;
         
         default:
            break;
      
      }

   }

   @Override
   public void keyReleased(KeyEvent e)
   {
      // tbd.
   }

   @Override
   public void keyTyped(KeyEvent e)
   {
      // tbd.
   }

   @Override
   public void mouseDragged(MouseEvent arg0)
   {
      if ( this.mDraggingNode != null )
      {
         int x = arg0.getX();
         int y = arg0.getY();
         
         this.mDraggingNode.setMx( x - mDraggingNodeDx );
         this.mDraggingNode.setMy( y - mDraggingNodeDy );
         
         this.mDraggingNode.setx( this.mDraggingNode.getMx() );
         this.mDraggingNode.sety( this.mDraggingNode.getMy() );
      }
      repaint();
   }

   @Override
   public void mouseMoved(MouseEvent arg0)
   {
      // tbd.
   }

   @Override
   public void mouseWheelMoved(MouseWheelEvent arg0)
   {
     // tbd.
   }

   @Override
   public void mouseClicked(MouseEvent ev)
   {
      if( ev.getClickCount() == 1 )
      {
         if (ev.getButton() == MouseEvent.BUTTON1)
         {
            Node n = getNode( ev.getX(), ev.getY() );

            if( n != null )
            {
               mSelectedNode = n;
            }
         }
         else if (ev.getButton() == MouseEvent.BUTTON3)
         {
            Node n = getNode( ev.getX(), ev.getY() );

            if( n != null )
            {
               mSelectedNode = n;
               n.setLoD( n.getLoD() +1 ); 
               if ( n.getLoD() > 3 )
               {
                  n.setLoD( 0 );
               }
               
               if( n.getLoD() == 1 )
               {
                  n.setHeight( 1 * this.mLineHeight );
               }
               else if ( n.getLoD() == 2 )
               {
                  n.setHeight( 3 * this.mLineHeight );
               }
               else if ( n.getLoD() == 3 )
               {
                  n.setHeight( 200 ); 
                  n.setWidth( 300 );
               }
               
            }
         }
      }
      else if( ev.getClickCount() == 2)
      {
         if (ev.getButton() == MouseEvent.BUTTON1)
         {
            Node n = getNode( ev.getX(), ev.getY() );
            
            if( n != null )
            {
               mSelectedNode = n;
               n.setCanFlow( !n.canFlow() );
            }
         }
      }

   }

   @Override
   public void mouseEntered(MouseEvent arg0)
   {
      // tbd.
   }

   @Override
   public void mouseExited(MouseEvent arg0)
   {
      // tbd.
   }

   @Override
   public void mousePressed(MouseEvent ev)
   {
      Dimension currentSize = getSize();
      this.mWidth = (int) currentSize.getWidth();
      this.mHeight = (int) currentSize.getHeight();

      requestFocusInWindow();
    
      this.mDraggingNode = null;
      this.mDraggingNodeDx = 0;
      this.mDraggingNodeDy = 0;
      
      if (ev.getButton() == MouseEvent.BUTTON1)
      {
         Node n = getNode( ev.getX(), ev.getY() );
         
         if( n != null )
         {
            mDraggingNode = n;
            mDraggingNodeDx = ev.getX() - n.getMx();
            mDraggingNodeDy = ev.getY() - n.getMy();   // 0...11
            n.mbIsInitialLayouted = true; 
         }
      }
   }

   @Override
   public void mouseReleased(MouseEvent arg0)
   {
      if (arg0.getButton() == MouseEvent.BUTTON1)
      {
         this.mDraggingNodeDx = 0;
         this.mDraggingNodeDy = 0;
         this.mDraggingNode = null;
      }
   }
}
