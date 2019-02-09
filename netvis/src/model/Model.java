package netvis.model;

import java.net.Inet4Address;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.pcap4j.packet.IpV4Packet;

import netvis.NetVisMain;
import netvis.ui.NetVisComponent.ReverseIterator;

public class Model
{
   
   public NetVisMain mMain;
   
   private final Vector<netvis.model.Link> mAllLinks;
   private final Vector<netvis.model.Node> mAllNodes;
   private final Vector<netvis.model.Packet> mAllPackets;   
   
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
   
   
   boolean equalsAddr( Inet4Address addr1, Inet4Address addr2 )
   {
      boolean retVal = false;

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
   
   public Link findLink( Node src, Node dst )
   {
      Link retVal = null;
      synchronized( mAllLinks )
      {
         for ( Link l : mAllLinks )
         {
            if( equalsAddr ( l.src.getAddr(), src.getAddr() ) 
                     && equalsAddr ( l.dst.getAddr(), dst.getAddr() ) )
            {
               retVal = l;
               break;
            }
         }
      }
      return ( retVal );
   }
   
   public Model( NetVisMain m)
   {
      mMain = m;
      
      this.mAllLinks = new Vector<Link>(100, 100);
      this.mAllNodes = new Vector<Node>(1000, 1000);
      this.mAllPackets = new Vector<Packet>(1000,1000);
      
      myCondoCleaner = new CondoCleaner();
      Thread cleanerThread = new Thread( myCondoCleaner );
      cleanerThread.start();
      
   }
   
   public Vector<Node> getAllNodes()
   {
      return mAllNodes;
   }
   
   public Vector<Link> getAllLinks()
   {
      return mAllLinks;
   }
   
   public Vector<netvis.model.Packet> getAllPackets()
   {
      return mAllPackets;
   }
   
   
   public void addIPv4Packet( /*Timestamp ts,*/ IpV4Packet ipv4p)
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
         srcNode.sentPackets++;        
      }
      srcNode.lastSeenIpv4p = ipv4p;
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
         dstNode.receivedPackets++;
      }
      dstNode.lastSeenIpv4p = ipv4p;
      dstNode.timeOfLastSeenPacket = now;
      
      Packet p = new Packet ( now, srcNode, dstNode, ipv4p.length()); 
      mAllPackets.add(p);
      
      Link link = findLink( srcNode, dstNode );
      if( link == null )
      {
         link = new Link( srcNode, dstNode );
         synchronized( mAllLinks )
         {
            mAllLinks.add( link );
         }
      }
      else
      {
         link.incPacketNr();
      }


      System.out.println("addIPv4Packet("+src+"->"+dst+"), got " +
               mAllNodes.size() + " nodes and " +
               mAllLinks.size() + " links, done.");
   }
   
   private class CondoCleaner implements Runnable
   {
   
      public void run()
      {
         System.out.println("CondoCleaner.run() called");
         boolean watching = true;
         
         while ( watching )
         {
            try
            {
               Thread.sleep(1000);
               
               long now = System.currentTimeMillis();
              
               synchronized( mAllNodes ) 
               {
                  for( Node n: new ReverseIterator<Node>(mAllNodes) )
                  {
                     if ( n.timeOfLastSeenPacket < ( now - (300000) ))
                     {                        
                        synchronized( mAllLinks )
                        {
                           for( Link l: new ReverseIterator<Link>(mAllLinks) )
                           {
                              if( l.src == n )
                              {
                                 mAllLinks.remove( l );
                              }
                              if( l.dst == n )
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
            catch( InterruptedException ie )
            {
               //...
            }
         }
      }
   }
}