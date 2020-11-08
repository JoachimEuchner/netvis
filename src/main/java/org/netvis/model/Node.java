package org.netvis.model;

import java.net.Inet4Address;
import org.netvis.ui.NetVisGraphNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Node {
  private static final Logger logger = LoggerFactory.getLogger(Node.class);

  private Inet4Address mAddr;
  public Inet4Address getAddr() { return ( mAddr ); }
  
  private final byte[] addressBytes;
  public byte[] getAddressBytes() { return addressBytes; }
  
  private int receivedPackets;
  public void incReceivedPackets() { receivedPackets++; }
  public int getReceivedPackets() { return receivedPackets; }

  private int sentPackets;
  public void incSentPackets() { sentPackets++; }
  public int getSentPackets() { return sentPackets; }
 
  private final NetVisGraphNode mGraphNode;
  public NetVisGraphNode getGraphNode() { return mGraphNode; }
  
  public Node( Inet4Address addr ) {
    setInet4Address( addr );
    addressBytes = mAddr.getAddress(); 
    receivedPackets = 0;
    sentPackets = 0;
    mGraphNode = new NetVisGraphNode(this);
  }
  
  public void setInet4Address( Inet4Address addr ) {
     mAddr = addr;
  }
}
