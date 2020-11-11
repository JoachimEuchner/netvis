package org.netvis.model;

import org.pcap4j.packet.namednumber.IpNumber;

public class Packet {
  private final long mTimestamp;
  public long getTs() { return mTimestamp; }
  private final IpNumber mProtocol;
  public IpNumber getProtocol() {return mProtocol; }
  private final Node mSrc;
  public Node getSrc() { return mSrc; }
  private final Node mDst;
  public Node getDst() { return mDst; }
  private final int mSize ;
  public int getSize() { return mSize; }
  
  public Packet( long ts, IpNumber protocol, Node src, Node dst, int size ) {
     mTimestamp = ts;
     mProtocol = protocol;
     mSrc = src;
     mDst = dst;
     mSize = size;
  }  
}
