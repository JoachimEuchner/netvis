package org.netvis.model;

import java.net.Inet4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Node {
  private static final Logger logger = LoggerFactory.getLogger(Node.class);

  private Inet4Address mAddr;
  public Inet4Address getAddr() { return ( mAddr ); }
  
  private final byte[] addressBytes;
  public byte[] getAddressBytes() { return addressBytes; }
  
  public Node( Inet4Address addr )
  {
    setInet4Address( addr );
    addressBytes = mAddr.getAddress();  
  }
  
  public void setInet4Address( Inet4Address addr )
  {
     mAddr = addr;
  }
}
