package org.netvis.model;


public class Connection {
  private final Node mSrc;
  public Node getSrc() { return mSrc; }

  private final Node mDst;
  public Node getDst() { return mDst; }

  private int seenPackets;

  private long timeOfLastSeenPacket = 0;

  public Connection( Node src, Node dst ) {
    mSrc = src;
    mDst = dst;
    seenPackets = 0;
  }

  public void incPacketNr() {
    seenPackets++;
    timeOfLastSeenPacket = System.currentTimeMillis();
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
