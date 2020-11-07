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
  
  private final ArrayList<Node> mAllNodes;
  private final ArrayList<Packet> mAllPackets;   
  
  public Model( NetVisMain m )
  {
    logger.info("Model<ctor> called.");
    mMain = m;
    this.mAllNodes = new ArrayList<>(1000);
    this.mAllPackets = new ArrayList<>(1000);
  }
  
  public void addIPv4Packet( IpV4Packet ipv4p )
  {
    Inet4Address src = ipv4p.getHeader().getSrcAddr();
    Inet4Address dst = ipv4p.getHeader().getDstAddr();  

    long now = System.currentTimeMillis();
    
    // for now:
    Node srcNode = new Node( src );
    Node dstNode = new Node( dst );
    Packet p = new Packet ( now, srcNode, dstNode, ipv4p.length()); 
    mAllPackets.add(p);
    
    logger.trace("addIPv4Packet, got {} packets", mAllPackets.size());
  }
}
