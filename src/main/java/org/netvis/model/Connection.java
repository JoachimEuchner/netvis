package org.netvis.model;

import org.netvis.ui.NetVisGraphConnection;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.namednumber.IpNumber;

public class Connection {
  private final Node mSrc;
  public Node getSrc() { return mSrc; }

  private final Node mDst;
  public Node getDst() { return mDst; }

  private int seenPackets;
  
  private IpNumber mProtocol;
  

  private long timeOfLastSeenPacket = 0;

  private NetVisGraphConnection mGraphConnection;
  public NetVisGraphConnection getGraphConnection() { return mGraphConnection; };
  
  private Connection myReverseConnection;
  public Connection getReverseConnection() { return myReverseConnection; }
  public void setReverseConnection( Connection rev ) { myReverseConnection = rev; };
  
  public Connection( Node src, Node dst, Packet firstPacket ) {
    mSrc = src;
    mDst = dst;
    mProtocol = firstPacket.getProtocol();
    seenPackets = 0;
    mGraphConnection = new NetVisGraphConnection(this);
    myReverseConnection = null;
    timeOfLastSeenPacket = System.currentTimeMillis();
  }

  public void incPacketNr() {
    seenPackets++;
    timeOfLastSeenPacket = System.currentTimeMillis();
  }
  
  public void addPacket(long now, Packet p) {
    seenPackets++;
    mProtocol = p.getProtocol();
    timeOfLastSeenPacket = now;
  }

  public long getTimeSinceLastSeenPacket() {
    return ( System.currentTimeMillis() - timeOfLastSeenPacket);
  }

  public void setTimeSeenLastPacket(long t) {
    timeOfLastSeenPacket = t;
  }

  public int getNrOfSeenPackets() {
    return seenPackets;
  }
}
