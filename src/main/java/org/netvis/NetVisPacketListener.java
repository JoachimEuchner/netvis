package org.netvis;

import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapPacket;
import org.pcap4j.packet.ArpPacket;
import org.pcap4j.packet.IcmpV4EchoPacket;
import org.pcap4j.packet.IcmpV4TimeExceededPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetVisPacketListener implements PacketListener {

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
    
    if( mNVL.getPcapHandle() != null ) {
       logger.trace("got Packet @{}ms", timeOfLastPackage );
    }
    
    IpV4Packet ipv4p = packet.get(IpV4Packet.class);
    if( ipv4p != null ) {
      logger.trace("received: [nr.: {}]----> IPV4: {} --> {}, len={}"
          , counter
          , ipv4p.getHeader().getSrcAddr()
          , ipv4p.getHeader().getDstAddr()
          , ipv4p.length());
      
      if (packet.contains(IcmpV4TimeExceededPacket.class)) 
      {  
        // IcmpV4TimeExceededPacket icmpTEp = packet.get(IcmpV4TimeExceededPacket.class);
        logger.trace("received [nr.:{}] ---> got a IcmpV4TimeExceededPacket from {}", 
            counter, ipv4p.getHeader().getSrcAddr());
      }
      
      synchronized( mNVL.getMain().getModel() ){
        mNVL.getMain().getModel().addIPv4Packet( timeOfLastPackage, ipv4p  ); 
      }
    } else {
      ArpPacket arpp = packet.get(ArpPacket.class);
      if( arpp != null  ) {
        logger.trace("received: [nr.: {}]----> ARP: {} --> {}, len={}"
            , counter
            , arpp.getHeader().getSrcProtocolAddr()
            , arpp.getHeader().getDstProtocolAddr()
            , arpp.length());
        // TODO: handle ARP-Packet
      } else  { 
        IpV6Packet ipv6p = packet.get(IpV6Packet.class);
        if( ipv6p != null ) {
          logger.trace("received: [nr.: {}]----> IPV6: {} --> {}, len={}"
              , counter
              , ipv6p.getHeader().getSrcAddr()
              , ipv6p.getHeader().getDstAddr()
              , ipv6p.length());
          // TODO: handle IPV6-Packet
        }
        else {
          logger.trace("received: [nr.: {}] {}", counter, packet);
        }
      }
    }
    
    mNVL.getMain().getNetVisFrame().getNetVisGraphComponent().repaint();
  }

}
