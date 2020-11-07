package org.netvis;

import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapPacket;
import org.pcap4j.packet.IpV4Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetVisPacketListener implements PacketListener{

  private static final Logger logger = LoggerFactory.getLogger(NetVisPacketListener.class);

  int counter = 0;
  private long timeOfLastPackage;
  public long getTimeOfLastPackage() { return timeOfLastPackage; }

  private NetVisListener mNVL;
  
  public NetVisPacketListener( NetVisListener nvl ) {
    mNVL = nvl;
    timeOfLastPackage = 0;
  }
  
  @Override
  public void gotPacket(PcapPacket packet) {
    counter++;
    timeOfLastPackage = System.currentTimeMillis();
    
    if( mNVL.getPcapHandle() != null )
    {
       logger.trace("got Packet @{}", timeOfLastPackage );
    }
    
    IpV4Packet ipv4p = packet.get(IpV4Packet.class);
    if( ipv4p != null )
    {
      logger.trace("received: [nr.: {}]----> IPV4: {} --> {}, len={}"
          , counter
          , ipv4p.getHeader().getSrcAddr()
          , ipv4p.getHeader().getDstAddr()
          , ipv4p.length());
      
      synchronized( mNVL.getMain().getModel()  )
      {
        mNVL.getMain().getModel().addIPv4Packet( ipv4p  );
      }
    }
  }

}
