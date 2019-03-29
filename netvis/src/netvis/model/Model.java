package netvis.model;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.pcap4j.packet.IpV4Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netvis.NetVisMain;
import netvis.traceroute.TraceRouteNode;

public class Model
{
   private static final Logger logger = LoggerFactory.getLogger(Model.class);

   private NetVisMain mMain;
   
   private final ArrayList<netvis.model.Link> mAllLinks;
   private final ArrayList<netvis.model.Node> mAllNodes;
   private final ArrayList<netvis.model.Packet> mAllPackets;   
   
   private final ArrayList<netvis.model.Route> mRoutes;
   private final ArrayList<netvis.traceroute.TraceRouteNode> mTraceRouteNodes;
   
   private CondoCleaner myCondoCleaner;
   
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
   
   public static boolean equalsAddr( Inet4Address addr1, Inet4Address addr2 )
   {
      boolean retVal = false;

      if(( addr1 == null ) || ( addr2 == null))
      {
         return false;
      }
      
      if( ( addr1.getAddress()[0] == addr2.getAddress()[0])
          && ( addr1.getAddress()[1] == addr2.getAddress()[1])     
          && ( addr1.getAddress()[2] == addr2.getAddress()[2]) 
          && ( addr1.getAddress()[3] == addr2.getAddress()[3]) )
      {
         retVal = true;
      }
      return ( retVal );
   }
   
   public Node findNode( Inet4Address addr )
   {
      Node retVal = null;
      synchronized( mAllNodes )
      {
         for ( Node n : mAllNodes )
         {
            if ( equalsAddr ( n.getAddr(), addr ) )
            {
               retVal = n;
               break;
            }
         }
      }
      
      return ( retVal );
   }
   
   public Node findNodeAndAdd( Inet4Address addr )
   {
      Node retVal = findNode( addr );
      
      if( retVal == null )
      {
         synchronized( mAllNodes )
         {
            Node n = new Node( addr );
            mAllNodes.add( n );
         }
      }
      
      return ( retVal );
   }
   
   
   public Link findLink( Inet4Address src, Inet4Address dst )
   {
      Link retVal = null;
      synchronized( mAllLinks )
      {
         for ( Link l : mAllLinks )
         {
            if( ( l!= null) && ( l.getSrc() != null ) && ( l.getDst() != null ))
            {
               if( equalsAddr ( l.getSrc().getAddr(), src ) 
                        && equalsAddr ( l.getDst().getAddr(), dst ) )
               {
                  retVal = l;
                  break;
               }
            }
         }
      }
      return ( retVal );
   }
   
   
   public Link findLink( Node src, Node dst )
   {
      Link retVal = null;
      synchronized( mAllLinks )
      {
         for ( Link l : mAllLinks )
         {
            if( ( l!= null) && ( l.getSrc() != null ) && ( l.getDst() != null ))
            {
               if( equalsAddr ( l.getSrc().getAddr(), src.getAddr() ) 
                        && equalsAddr ( l.getDst().getAddr(), dst.getAddr() ) )
               {
                  retVal = l;
                  break;
               }
            }
         }
      }
      return ( retVal );
   }
   
   
   public Route findRoute( Node src, Node dst )
   {
      Route retVal = null;
            
      byte[] srcAddressBytes = src.getAddressBytes();
      byte[] dstAddressBytes = dst.getAddressBytes();
      
      if( (srcAddressBytes[0] == -64) && (srcAddressBytes[1] == -88) && (srcAddressBytes[2] == 2) )
      {
         // src=192.168.2.0/8, dst=X
         // --> search for 192.168.1.44 <--> X
         Node routeSrc = findNode( mMain.getTracerouter().getSrcAddress() );
         synchronized( mRoutes )
         {
            for ( Route r : mRoutes )
            {
               if( ( r!= null) && ( r.mSrcAddr != null ) && ( r.mDstAddr != null ))
               {
                  if( equalsAddr ( r.mSrcAddr, routeSrc.getAddr() ) 
                           && equalsAddr ( r.mDstAddr, dst.getAddr() ) )
                  {
                     retVal = r;
                     break;
                  }
               }
            }
         }
      }
      else  if ( (dstAddressBytes[0] == -64) && (srcAddressBytes[1] == -88) && (srcAddressBytes[2] == 2) )
      {
         Node routeDst = findNode( mMain.getTracerouter().getSrcAddress() );
         // dst=192.168.2.0/8, src=X
         // --> search for 192.168.1.44 <--> X
         synchronized( mRoutes )
         {
            for ( Route r : mRoutes )
            {
               if( ( r!= null) && ( r.mSrcAddr != null ) && ( r.mDstAddr != null ))
               {
                  if( equalsAddr ( r.mSrcAddr, src.getAddr() ) 
                           && equalsAddr ( r.mDstAddr, routeDst.getAddr() ) )
                  {
                     retVal = r;
                     break;
                  }
               }
            }
         }
      }
      return ( retVal );
   }  
   
   public Model( NetVisMain m )
   {
      logger.info("Model<ctor> called.");
      
      mMain = m;
      
      this.mAllLinks = new ArrayList<>(100);
      this.mAllNodes = new ArrayList<>(1000);
      this.mAllPackets = new ArrayList<>(1000);
      
      this.mRoutes = new ArrayList<>(100);
      this.mTraceRouteNodes = new ArrayList<>(1000);
            
      myCondoCleaner = new CondoCleaner();
      Thread cleanerThread = new Thread( myCondoCleaner );
      cleanerThread.start();
      
   }
   
   public List<Node> getAllNodes()
   {
      return mAllNodes;
   }
   
   public List<Link> getAllLinks()
   {
      return mAllLinks;
   }
   
   public List<netvis.model.Packet> getAllPackets()
   {
      return mAllPackets;
   }
   
   
   public void addIPv4Packet( IpV4Packet ipv4p )
   {
      Inet4Address src = ipv4p.getHeader().getSrcAddr();
      Inet4Address dst = ipv4p.getHeader().getDstAddr();  
      
      long now = System.currentTimeMillis();
      
      Node srcNode = findNode( src );
      if( srcNode == null )
      {
         srcNode = new Node( src );
         synchronized( mAllNodes )
         {
            mAllNodes.add(srcNode);
         }
      }
      else
      {
         srcNode.incSentPackets();        
      }
      srcNode.setLastSeenIpv4Packet( ipv4p );
      srcNode.timeOfLastSeenPacket = now;

      Node dstNode = findNode( dst );
      if( dstNode == null )
      {
         dstNode = new Node( dst );
         synchronized( mAllNodes )
         {
            mAllNodes.add(dstNode);
         }
      }
      else
      {
         dstNode.incReceivedPackets();
      }
      dstNode.setLastSeenIpv4Packet( ipv4p );
      dstNode.timeOfLastSeenPacket = now;
      
      Packet p = new Packet ( now, srcNode, dstNode, ipv4p.length()); 
      mAllPackets.add(p);
      
      Link link = findLink( srcNode, dstNode );
      Route route = findRoute( srcNode, dstNode );
      if( route != null )
      {
         route.add( p );
      }
      else if( link == null )
      {
         link = new Link( srcNode, dstNode );
         synchronized( mAllLinks )
         {
            mAllLinks.add( link );
         }
      }
      link.incPacketNr();
   }
   
   
   public void addNode( Node n )
   {
      synchronized( mAllNodes )
      {
         mAllNodes.add(n);
      }
   }
   
   
   public void addLink( Link l )
   {
      synchronized( mAllLinks )
      {
         if(( l.getSrc() != null ) && ( l.getDst() != null ))
         {
            mAllLinks.add(l);
         }
         else
         {
            logger.warn("Model.addLink(): tried to add l with {} -> {} ", l.getSrc(), l.getDst());
         }
      }
   }
   
   public void removeLink(  Inet4Address src , Inet4Address dst )
   {
      synchronized( mAllLinks )
      {
         for( Link l: new ReverseIterator<Link>(mAllLinks) )
         {
            if( ( l!= null) && ( l.getSrc() != null ) && ( l.getDst() != null ))
            {
               if( ( Model.equalsAddr( l.getSrc().getAddr(), src) )
                        && Model.equalsAddr( l.getDst().getAddr(), dst ) ) 
               {
                  mAllLinks.remove( l );
               }
            }
         }
      }
   }
      
   public void addTraceRouteNode( Inet4Address srcAddr, Inet4Address origDstAddr, Inet4Address replyingAddr, int depth )
   {
      TraceRouteNode trn = null;
      synchronized( mTraceRouteNodes )
      {
         for ( TraceRouteNode n : mTraceRouteNodes )
         {
            if ( equalsAddr ( n.getAddr(), replyingAddr ) )
            {
               trn = n;
               break;
            }
         }
      }
      if( trn == null )
      {
         trn = new TraceRouteNode( srcAddr, origDstAddr, replyingAddr, depth);
         this.mTraceRouteNodes.add( trn );
      }
      
      Route r = findRouteForTraceRouteNode( srcAddr, origDstAddr );
      if( r != null )
      {
         r.addTraceRouteNode(trn);
      }
      else
      {
         r = new Route( this, srcAddr,  origDstAddr );
         mRoutes.add(r);
         r.addTraceRouteNode(trn);
      }
   }
   
   private Route findRouteForTraceRouteNode( Inet4Address src, Inet4Address dst )
   {
      Route retVal = null;
      synchronized( mRoutes )
      {
         for ( Route r : mRoutes )
         {
            logger.trace("findRouteForTraceRouteNode:<{}->{}> ?= <{}->{}>", src, dst, r.mSrcAddr, r.mDstAddr);
            
            if( equalsAddr ( src, r.mSrcAddr ) 
                && equalsAddr ( dst, r.mDstAddr ) )
            {
               retVal = r;
               break;
            }
         }
      }
      
      logger.trace("trn:<{}->{}> found {} as route", src, dst, retVal);
      
      return ( retVal );
   }
   
   
   
   
   private class CondoCleaner implements Runnable
   {
   
      public void run()
      {
         logger.debug("CondoCleaner.run() called");
         boolean watching = true;
         
         while ( watching )
         {
            try
            {
               Thread.sleep(1000);
            }
            catch( InterruptedException ie )
            {
               //...
            }
            
            long now = System.currentTimeMillis();

            synchronized( mAllNodes ) 
            {
               for( Node n: new ReverseIterator<Node>(mAllNodes) )
               {
                  if ( n.timeOfLastSeenPacket < ( now - (300000000) ))
                  {                        
                     synchronized( mAllLinks )
                     {
                        for( Link l: new ReverseIterator<Link>(mAllLinks) )
                        {
                           if( l.getSrc() == n )
                           {
                              mAllLinks.remove( l );
                           }
                           if( l.getDst() == n )
                           {
                              mAllLinks.remove( l );
                           }
                        }
                     }
                     mAllNodes.remove(n);
                  }
               }
            }
         }
      }
   }
}
