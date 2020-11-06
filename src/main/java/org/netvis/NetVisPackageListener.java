package org.netvis;

import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetVisPackageListener implements PacketListener{

  private static final Logger logger = LoggerFactory.getLogger(NetVisPackageListener.class);

  int counter = 0;
  private long timeOfLastPackage;
  public long getTimeOfLastPackage() { return timeOfLastPackage; }

  private NetVisListener mNVL;
  
  public NetVisPackageListener( NetVisListener nvl ) {
    mNVL = nvl;
    timeOfLastPackage = 0;
  }
  
  @Override
  public void gotPacket(PcapPacket packet) {
    counter++;
    timeOfLastPackage = System.currentTimeMillis();
    
    if( mNVL.getPcapHandle() != null )
    {
       logger.trace("got Packet @{}", mNVL.getPcapHandle().getTimestampPrecision() );
    }
  }

}
