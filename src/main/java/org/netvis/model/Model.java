package org.netvis.model;

import java.net.Inet4Address;
import java.util.ArrayList;
import org.netvis.NetVisMain;
import org.pcap4j.packet.IpV4Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Model {
  private static final Logger logger = LoggerFactory.getLogger(Model.class);
  private NetVisMain mMain;
  

  private final ArrayList<Packet> mAllPackets;   
  private final ArrayList<Node> mAllNodes;
  private final ArrayList<Connection> mAllConnections;
  
  public Model( NetVisMain m ) {
    logger.info("Model<ctor> called.");
    mMain = m;
    this.mAllNodes = new ArrayList<>(1000);
    this.mAllPackets = new ArrayList<>(1000);
    this.mAllConnections =  new ArrayList<>(1000);
  }
  
  public void addIPv4Packet( IpV4Packet ipv4p ) {
    Inet4Address src = ipv4p.getHeader().getSrcAddr();
    Inet4Address dst = ipv4p.getHeader().getDstAddr();  

    long now = System.currentTimeMillis();
    
    // for now:
    Node srcNode = findNodeAndAdd ( src );
    Node dstNode = findNodeAndAdd ( dst );
    Packet p = new Packet ( now, srcNode, dstNode, ipv4p.length()); 
    mAllPackets.add(p);
    
    Connection conn = findConnectionAndAdd( srcNode, dstNode );
    
    logger.trace("addIPv4Packet, got {} packets, {} nodes, {} connections", 
        mAllPackets.size(), mAllNodes.size(), mAllConnections.size());
  }

  
  public static boolean equalsAddr( Inet4Address addr1, Inet4Address addr2 ) {
    boolean retVal = false;

    if(( addr1 == null ) || ( addr2 == null)) {
      return false;
    }

    if( ( addr1.getAddress()[0] == addr2.getAddress()[0])
        && ( addr1.getAddress()[1] == addr2.getAddress()[1])     
        && ( addr1.getAddress()[2] == addr2.getAddress()[2]) 
        && ( addr1.getAddress()[3] == addr2.getAddress()[3]) ) {
      retVal = true;
    }
    return ( retVal );
  }

  private Node findNode( Inet4Address addr ) {
    Node retVal = null;
    synchronized( mAllNodes ) {
      for ( Node n : mAllNodes ) {
        if ( equalsAddr ( n.getAddr(), addr ) ) {
          retVal = n;
          break;
        }
      }
    }
    return ( retVal );
  }

  private Node findNodeAndAdd( Inet4Address addr ) {
    Node retVal = findNode( addr );
    if( retVal == null ) {
      synchronized( mAllNodes ) {
        Node n = new Node( addr );
        mAllNodes.add( n );
        retVal = n;
      }
    }
    return ( retVal );
  }


  private Connection findConnection( Node src, Node dst )
  {
    Connection retVal = null;
    synchronized( mAllConnections ) {
      for ( Connection l : mAllConnections ) {
        if( ( l!= null) && ( l.getSrc() != null ) && ( l.getDst() != null ))
        {
          if( equalsAddr ( l.getSrc().getAddr(), src.getAddr() ) 
              && equalsAddr ( l.getDst().getAddr(), dst.getAddr() ) ) {
            retVal = l;
            break;
          }
        }
      }
    }
    return ( retVal );
  }
  
  
  private Connection findConnectionAndAdd( Node src, Node dst )
  {
    Connection retVal = findConnection( src, dst );
    if( retVal == null ) {
      synchronized( mAllConnections ) {
        Connection conn = new Connection( src, dst );
        mAllConnections.add( conn );
        retVal = conn;
      }
    }
    return ( retVal );
  }

}
