package netvis.traceroute;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.AbstractPacket.AbstractBuilder;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IcmpV4CommonPacket;
import org.pcap4j.packet.IcmpV4EchoPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Rfc791Tos;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.Packet.Builder;
import org.pcap4j.packet.UnknownPacket;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.packet.namednumber.IcmpV4Code;
import org.pcap4j.packet.namednumber.IcmpV4Type;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.IpVersion;
import org.pcap4j.util.MacAddress;

import netvis.NetVisMain;

public class Traceroute
{
   private static final String READ_TIMEOUT_KEY =
            Traceroute.class.getName() + ".readTimeout";
   private static final int READ_TIMEOUT = Integer.getInteger(READ_TIMEOUT_KEY, 10); // [ms]
   
  
   private static final String SNAPLEN_KEY = Traceroute.class.getName() + ".snaplen";
   private static final int SNAPLEN = Integer.getInteger(SNAPLEN_KEY, 65536); // [bytes]

  
   private static final String TU_KEY = Traceroute.class.getName() + ".tu";
   private static final int TU = Integer.getInteger(TU_KEY, 40); // [bytes] 
 
   
   private NetVisMain main;
   private PcapHandle sendHandle;

   public Traceroute( NetVisMain m )
   {
      main = m;     
   }


   public void doTraceRoute(String target)
   {
      while ( main.getNif() == null )
      {
         try {
            Thread.sleep(1000);
         } catch (InterruptedException e) {
            break;
         }
      }
      
      try
      {
         sendHandle = main.getNif().openLive(SNAPLEN, PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);
      } 
      catch (PcapNativeException e)
      {
         e.printStackTrace();
      }
       
      try 
      {
         final Inet4Address targetAddress;
         try 
         {
            targetAddress = (Inet4Address) InetAddress.getByName(target);
         } 
         catch (UnknownHostException e1) 
         {
            throw new IllegalArgumentException("args[0]: " + target);
         }

         final Inet4Address srcAddress = (Inet4Address) InetAddress.getLocalHost();
         
         Packet.Builder tmp;
         final IpV4Packet.Builder ipv4b = new IpV4Packet.Builder();

         byte[] echoData = new byte[TU - 28];
         for (int i = 0; i < echoData.length; i++) {
            echoData[i] = (byte) i;
         }

         IcmpV4EchoPacket.Builder echoBuilder = new IcmpV4EchoPacket.Builder();
         echoBuilder
            .identifier((short) 1)
            .payloadBuilder(new UnknownPacket.Builder().rawData(echoData));

         IcmpV4CommonPacket.Builder icmpV4CommonBuilder = new IcmpV4CommonPacket.Builder();
         icmpV4CommonBuilder
            .type(IcmpV4Type.ECHO)
            .code(IcmpV4Code.NO_CODE)
            .payloadBuilder(echoBuilder)
            .correctChecksumAtBuild(true);

         IpV4Packet.Builder ipV4Builder = new IpV4Packet.Builder();
         
         for ( int ttl = 1; ttl < 20; ttl++)
         {
            
            
            ipV4Builder
               .version(IpVersion.IPV4)
               .tos(IpV4Rfc791Tos.newInstance((byte) 0))
               .ttl((byte) ttl)                           // <---------------------!!
               .protocol(IpNumber.ICMPV4)
               .srcAddr( srcAddress )
               .dstAddr( targetAddress )
               .payloadBuilder(icmpV4CommonBuilder)
               .correctChecksumAtBuild(true)
               .correctLengthAtBuild(true);
            
            MacAddress srcMac = MacAddress.getByName("aa:aa:aa:00:7e:24");
            
            EthernetPacket.Builder etherBuilder = new EthernetPacket.Builder();
            etherBuilder.dstAddr(MacAddress.ETHER_BROADCAST_ADDRESS)
                    .srcAddr(srcMac)
                    .type(EtherType.IPV4)
                    .payloadBuilder(ipV4Builder)
                    .paddingAtBuild(true);

            Packet p = etherBuilder.build();
            
            System.out.println("Traceroute, ttl="+ttl+": sending "+p);
            
            main.getPcapHandle().sendPacket(p);

            try {
               Thread.sleep(1000);
            } catch (InterruptedException e) {
               break;
            }
         }

      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         if (sendHandle != null && sendHandle.isOpen()) {
            sendHandle.close();
         }
      }
   }


}
