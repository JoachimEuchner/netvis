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
import java.util.Vector;

import javax.swing.JComponent;

import netvis.NetVisMain;
import netvis.model.Model;
import netvis.model.Node;
import netvis.traceroute.TraceRouteNode;

public class NetVisComponent extends JComponent implements
   MouseListener, MouseWheelListener, MouseMotionListener, KeyListener,
   ActionListener
{
   
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

   transient Image           offscreen;
   transient Toolkit         mToolkit;
   
   private final NetVisMain  mMain;
   private final Model       mModel;
   
   int                       mWidth            = 300;                   // 1500;
   int                       mHeight           = 200;                    // 1000;

   public Font                         myPlain10Font;
   public int                          mLineHeight;
  
   public transient BasicStroke                  mStroke8;          
   public transient BasicStroke                  mStroke7;
   public transient BasicStroke                  mStroke6;
   public transient BasicStroke                  mStroke5;
   public transient BasicStroke                  mStroke4;
   public transient BasicStroke                  mStroke3;
   public transient BasicStroke                  mStroke2;
   public transient BasicStroke                  mStroke1;
   
   
   private Node mSelectedNode = null;
   private Node mDraggingNode = null;
   private int mDraggingNodeDx = 0;
   private int mDraggingNodeDy = 0;
   
   
   public boolean doLayouting;
   public int paintCounter;
   
   double standardCharge;
   double standardSpring;
   
   public NetVisComponent( NetVisMain main, Model m )
   {
      System.out.println("NetVisComponent<ctor> called.");
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
      // g2.setTransform(mAffineTransform);
      // g2d.drawString("aString", x, y);


      g2.setColor(Color.BLACK);
      // g2.setColor(Color.GREEN.darker().darker().darker());
      g2.fillRect(0, 0, this.mWidth, this.mHeight);
      
      
      layoutNodes();
      
      paintAllLinks( g2 );
      paintAllNodes( g2 );
      
      
      long paintend = System.currentTimeMillis();
      g2.setColor(java.awt.Color.GREEN.darker());
      g2.setFont(this.myPlain10Font);
      String s = "g2.paint(): " + (paintend - paintstart) + " ms/"+paintCounter; //$NON-NLS-1$ //$NON-NLS-2$
      g2.drawString(s, this.mWidth - 200, this.mHeight - 8);
      // System.out.println("paint() done.");
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
         System.out.println("MyLayouter.run() called.");
         
         try
         {
            Thread.sleep(2000);
         }
         catch( InterruptedException ie )
         {
            //...
         }
         System.out.println("MyLayouter.run() start layouting, "+mModel);
       
         
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
                  // System.out.println("MyLayouter.run() start layouting "+mModel.getAllNodes().size()+" nodes");
                                    
                  synchronized( mModel.getAllNodes() )
                  {

                     Vector<Node> mAllNodes = mModel.getAllNodes();

                     // System.out.println("...MyLayouter.run() got Mutex,  layouting "+mAllNodes.size()+" nodes");

                     // new ReverseIterator<String>(mAllNodes)
                     // for (Node n : mAllNodes )
                     for( Node n: new ReverseIterator<Node>(mAllNodes) )
                     {
                        n.fx = 0.0;
                        n.fy = 0.0;

                        if ( n.mbCanFlow )
                        {

                           double charge = standardCharge;
                           for (Node m : mAllNodes )
                           {
                              if( n != m )
                              {
                                 if( ( n.isActive ) && ( m.isActive ) )
                                 {
                                    double d2 = (n.x - m.x)*(n.x - m.x) + (n.y - m.y)*(n.y - m.y);

                                    double localCharge = charge;
                                    if ( n.addressBytes[0] == m.addressBytes[0] ) 
                                    { 
                                       localCharge = standardCharge / 2.0;
                                       if  ( n.addressBytes[1] == m.addressBytes[1] ) 
                                       {
                                          localCharge = standardCharge / 10.0;
                                          if  ( n.addressBytes[2] == m.addressBytes[2] ) 
                                          {
                                             localCharge = standardCharge / 50.0;
                                          }
                                       }
                                    }


                                    if( d2 > 0 )
                                    {
                                       double d = Math.sqrt ( d2 );

                                       double nx = (n.x - m.x) / d;
                                       double ny = (n.y - m.y) / d;
                                       n.fx += localCharge * nx / d2;
                                       n.fy += localCharge * ny / d2;
                                    }
                                 }
                              }
                           }

                           // System.out.println("fx="+n.fx+" fy="+n.fy);


                           double spring = standardSpring;
                           synchronized( mModel.getAllLinks() )
                           {
                              Vector<netvis.model.Link> mAllLinks = mModel.getAllLinks();

                              for (netvis.model.Link l : mAllLinks )
                              {
                                 Node s = l.src;
                                 Node d = l.dst;

                                 if( ( s!= null ) && (d!= null ))
                                 {
                                    if( ( s.isActive ) && ( d.isActive ) )
                                    {
                                       double mySpring = spring * (1.0 + l.getNrOfSeenPackets() / 1000.0 );
                                       if( mySpring > 0.5)
                                       {
                                          mySpring = 0.5;
                                       }
                                       if( n == s ) 
                                       {
                                          n.fx -=  mySpring * (n.x - d.x) ;
                                          n.fy -=  mySpring * (n.y - d.y) ;
                                       }


                                       if ( n == d )
                                       {
                                          n.fx -= mySpring * (n.x - s.x) ;
                                          n.fy -= mySpring * (n.y - s.y) ;
                                       }
                                    }
                                 }
                              }
                           }

                           // n.vx += n.fx;
                           // n.vy += n.fy;
                           if( n.fx > 50.0 )
                           {
                              n.fx = 50.0;
                           }
                           if( n.fx < -50.0 )
                           {
                              n.fx = -50.0;
                           }

                           n.x += n.fx ;
                           n.mx = (int)( n.x + 0.5);
                           if( n.mx < 0 )
                           {
                              n.mx = 0;
                              n.x = 0;
                           }
                           if( n.mx > (mWidth - 100)  )
                           {
                              n.mx = (mWidth - 100);
                              n.x = (mWidth - 100);
                           }

                           if( n.fy > 50.0 )
                           {
                              n.fy = 50.0;
                           }
                           if( n.fy < -50.0 )
                           {
                              n.fy = -50.0;
                           }

                           n.y += n.fy;
                           n.my = (int)( n.y +0.5);
                           if( n.my < 0 )
                           {
                              n.my = 0;
                              n.y = 0;
                           }
                           if( n.my > (mHeight - 10)  )
                           {
                              n.my = (mHeight - 10);
                              n.y = (mHeight - 10);
                           }

                           // System.out.println(" -----> fx="+n.fx+" fy="+n.fy+", vx="+n.vx+" "+"vy="+n.vy+", x="+n.x+" "+n.y);
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
         Vector<Node> mAllNodes = mModel.getAllNodes();
         
         int nrOfNodesToLayout = mAllNodes.size();
         
         int xCenter = mWidth / 2;
         int yCenter = mHeight / 2;
                 
         double radius = mHeight * 0.4;
                          
         double i = 0.0;
         if( nrOfNodesToLayout > 0 )
         {
            for (Node n : mAllNodes )
            {
               if( !n.mbIsInitialLayouted ) 
               {
                  double angle = 2* Math.PI / nrOfNodesToLayout * i;

                  n.mx = (int)( xCenter + radius * Math.sin ( angle ));
                  n.my = (int)( yCenter + radius * Math.cos ( angle ));

                  n.x = n.mx;
                  n.y = n.my;
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
         
         Vector<netvis.model.Node> allNodes = mModel.getAllNodes();
         
         
         int timeDiagramWidth = 300;
         int timeDiagramHeight = 100;
         
         
         g2.setColor(Color.WHITE);
         for ( netvis.model.Node n : allNodes )
         {
            String s = n.mDisplayName; 
           
            if( true )//  !n.mbIsUserMoved  )
            {
               int width = g2.getFontMetrics().stringWidth(s);

               if( n.mLoD == 0 )
               {
                  n.mWidth = 5;
                  n.mHeight = 5;
               }
               else if( n.mLoD == 1 )
               {
                  n.mWidth = width;
                  n.mHeight = 1 * this.mLineHeight;
               } 
               else if( n.mLoD == 2 )
               {
                  n.mWidth = width;
                  n.mHeight = 3 * this.mLineHeight;
               }
               else if( n.mLoD == 3 )
               {
                  n.mHeight = timeDiagramHeight + 3 * this.mLineHeight ;//35 * this.mLineHeight;
                  n.mWidth = timeDiagramWidth;
               }

               n.mDisplayNameXoffset = width/2;

               // n.mx -= n.mDisplayNameXoffset;
            }
            
            Color mainColor = Color.BLACK;
            Color textColor = Color.LIGHT_GRAY;
            Color borderColor = Color.GRAY.darker();
         
            
            if( n.type == Node.TYPE_ENDPOINT )
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
            g2.fillRect(n.mx,n.my,n.mWidth,n.mHeight);

       
            if( n.mLoD == 0 )
            {
               if( n.type == Node.TYPE_ROUTEPOINT) 
               {
                  try 
                  {
                     if( n instanceof TraceRouteNode)
                     {
                        int depth = ((TraceRouteNode)n).mDepth;
                        g2.setColor(textColor);
                        g2.drawString( Integer.toString(depth), n.mx+5, n.my+ this.mLineHeight - 1);
                     }
                  }
                  catch (ClassCastException cce )
                  {
                     
                  }
               }
            }
            else if( n.mLoD > 0)
            {
               g2.setColor(textColor);
               g2.drawString(s, n.mx, n.my+ this.mLineHeight - 1);
            }
            
            if( n.mLoD > 1 )
            {
               String s2 = n.getAddr().toString();
               if( s2.startsWith("/"))
               {
                  s2 = s2.substring( 1, s2.length());
               }
               g2.drawString(s2, n.mx, n.my+2*this.mLineHeight - 1);
               
               String s3 = "s"+n.sentPackets + ",r" + n.receivedPackets;
               g2.drawString(s3, n.mx, n.my+3*this.mLineHeight - 1);
               
               if( n.mLoD > 2 )
               {

                  g2.setColor(Color.CYAN.darker().darker().darker());
                  g2.fillRect(n.mx, n.my + 3 * this.mLineHeight, timeDiagramWidth, timeDiagramHeight);
                                    
                  synchronized ( mModel.getAllPackets() )
                  {  
                     g2.setStroke(mStroke1);
                     g2.setColor(Color.CYAN);
                     long diagramDuration = 300000;
                     for (netvis.model.Packet p : mModel.getAllPackets() )
                     {
                        if( ( p.src == n ) || ( p.dst == n ))
                        {
                           long now = System.currentTimeMillis();
                           long age = now - p.ts;

                           if( age < diagramDuration )
                           {  
                              int x = (timeDiagramWidth - (int)(age * timeDiagramWidth / diagramDuration)) + n.mx;
                              int yTop = timeDiagramHeight - p.size/5;
                              if( yTop <  0 )
                              {
                                 yTop = 0;
                              }
                              yTop += n.my + 3*this.mLineHeight ;
                              if( yTop < n.my)
                              {
                                 yTop = n.my;
                              }

                              int yBottom = timeDiagramHeight + n.my  + 3*this.mLineHeight;

                              g2.drawLine( x, yTop, x, yBottom );  
                           }
                        }
                     }
                  }
                  
                  /*
                  g2.setColor(textColor);
                  if( n.lastSeenIpv4p != null )
                  {
                     String s4 = n.lastSeenIpv4p.toString();
                     
                     String[] lines = s4.split("\\r?\\n", -1);
                     int nr = 4;
                     for(String line : lines) {
                         // System.out.printf("\tLine %02d \"%s\"%n", nr++, line);
                        
                        g2.drawString(line, n.mx, n.my+ nr*this.mLineHeight - 1);
                        nr++;
                        if ( nr > 35 )
                        {
                           break;
                        }
                     }
                  }
                  */
               }
            }
            
            g2.setStroke(mStroke1);
            g2.setColor(borderColor);
            g2.drawRect(n.mx,n.my,n.mWidth,n.mHeight);
            if( n == mSelectedNode )
            {
               g2.setColor(borderColor);
               g2.drawRect(n.mx,n.my,n.mWidth,n.mHeight);
               g2.drawRect(n.mx-1,n.my-1,n.mWidth+2,n.mHeight+2);
               g2.drawRect(n.mx-2,n.my-2,n.mWidth+4,n.mHeight+4);
            }
            
            if ( !n.mbCanFlow )
            {
               g2.setColor(borderColor);
               g2.fillRect(n.mx,n.my,3,3);
            }
            
            if( !n.isActive )
            {
               g2.drawLine( n.mx, n.my + n.mHeight, n.mx+n.mWidth, n.my);
            }
            
         }
      }
   }
   
   
   private class ShortestPair
   {
      public int s1;
      public int s2;
      ShortestPair(int _s1, int _s2)
      {
         s1 = _s1;
         s2 = _s2;
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
         r1 = p1;
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
         if ( !mModel.mMain.isOnline )
         {
            millis = 1000;
         }

         float hue = (float) 0.0;
         double fsFract =  Math.exp( -(double)millis / 10000.0 );
         hue = (float) ( 0.6666 - fsFract * 2.0 / 3.0);   // 0.000(red) - 0.33333(green);
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
         Vector<netvis.model.Link> mAllLinks = mModel.getAllLinks();
         
         g2.setColor(Color.BLACK);
                 
         for (netvis.model.Link l : mAllLinks )
         {
            Node s = l.src;
            Node d = l.dst;

            if(( s!=null) && ( d!=null ))
            {
               int packets = l.getNrOfSeenPackets();

               g2.setStroke(mStroke1);
               if( ( packets > 2 ) && ( s.isActive ) && ( d.isActive ) )
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


               ShortestPair xp = getShortestPair(s.mx,  s.mx + s.mWidth, d.mx,  d.mx + d.mWidth); 
               ShortestPair yp = getShortestPair(s.my,  s.my + s.mHeight, d.my, d.my + d.mHeight); 

               Color c = getColorOfLinkAge( l.getTimeSinceLastSeenPacket(), (s.isActive && d.isActive) );
               g2.setColor(c);

               // g2.drawLine( xp.s1, yp.s1, xp.s2, yp.s2 );
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
         Vector<Node> allNodes = mModel.getAllNodes();
         // new ReverseIterator<String>(mAllNodes)
         // for (Node n : mAllNodes )
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

      // double length = Math.sqrt((xStart - xEnd) * (xStart - xEnd) + (yStart -
      // yEnd) * (yStart - yEnd));
      // double nSteps = length / 10.0; // 10Pixel long lines.

      // if( length > 10.0 )
      // {
      // vStep = 1.0 / nSteps;
      // }

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
      
      
      System.out.println("Charge: "+standardCharge + ", Spring: "+standardSpring);

   }

   @Override
   public void keyPressed(KeyEvent e)
   {
      int code = e.getKeyCode();
   
      switch (code)
      {
         case KeyEvent.VK_SPACE:
         {
            // start/stop layouting
         }
         break;
         
         case KeyEvent.VK_A:
         {
            if ( mSelectedNode != null )
            {
               mSelectedNode.isActive = !mSelectedNode.isActive;
            }
         }
         
         case KeyEvent.VK_T:
         {
            if ( mSelectedNode != null )
            {
               // mSelectedNode.isActive = !mSelectedNode.isActive;
               mMain.mTracerouteScheduler.addTargetAddress(mSelectedNode.getAddr());
               // mMain.mTracerouteScheduler.traceNextTarget();
            }
         }
      
      }

   }

   @Override
   public void keyReleased(KeyEvent e)
   {
   

   }

   @Override
   public void keyTyped(KeyEvent e)
   {
   

   }

   @Override
   public void mouseDragged(MouseEvent arg0)
   {
      if ( this.mDraggingNode != null )
      {
         int x = arg0.getX();
         int y = arg0.getY();
         
         // System.out.println("dragged to "+x+", "+y+" node="+mDraggingNode.mDisplayName);
         this.mDraggingNode.mx = x - mDraggingNodeDx;
         this.mDraggingNode.my = y - mDraggingNodeDy;
         
         this.mDraggingNode.x = this.mDraggingNode.mx;
         this.mDraggingNode.y = this.mDraggingNode.my;
      }
      repaint();
   }

   @Override
   public void mouseMoved(MouseEvent arg0)
   {
    
   }

   @Override
   public void mouseWheelMoved(MouseWheelEvent arg0)
   {
     

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
               n.mLoD++; 
               if ( n.mLoD > 3 )
               {
                  n.mLoD = 0;
               }
               
               if( n.mLoD == 1 )
               {
                  n.mHeight = 1 * this.mLineHeight;
               }
               else if ( n.mLoD == 2 )
               {
                  n.mHeight = 3 * this.mLineHeight;
               }
               else if ( n.mLoD == 3 )
               {
                  n.mHeight = 200; //35 * this.mLineHeight;
                  n.mWidth = 300;
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
               n.mbCanFlow = !n.mbCanFlow;
            }
         }
      }

   }

   @Override
   public void mouseEntered(MouseEvent arg0)
   {
    

   }

   @Override
   public void mouseExited(MouseEvent arg0)
   {
   

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
            mDraggingNodeDx = ev.getX() - n.mx;
            mDraggingNodeDy = ev.getY() - n.my;   // 0...11
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
