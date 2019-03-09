package netvis;

import java.net.Inet4Address;

import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.packet.ArpPacket;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IcmpV4CommonPacket;
import org.pcap4j.packet.IcmpV4EchoPacket;
import org.pcap4j.packet.IcmpV4TimeExceededPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.LinuxSllPacket;
import org.pcap4j.packet.Packet;

import netvis.model.Model;
import netvis.traceroute.TraceRouteMsg;
import netvis.traceroute.TraceRouteNode;

public class NetVisPackageListener implements PacketListener
{ 
   
   private NetVisMain mMain;
   private Model mModel;
   
   public NetVisPackageListener( NetVisMain main, PcapHandle pch, Model m ) 
   {
      // pcapHandle = pch;
      mMain = main;
      mModel = m;
      timeOfLastPackage = System.currentTimeMillis();
   };
   
   int counter = 0;
   
   public long timeOfLastPackage;
   
   public void gotPacket(Packet packet) 
   {
      counter++;
      timeOfLastPackage = System.currentTimeMillis();
      System.out.print(".");
      
      // System.out.println("got Packet @"+mPcapHandle.getTimestamp());

      // String s = packet.toString();
      // System.out.println("received [nr.: "+counter+"]");

      if( true )
      {
         IpV4Packet ipv4p = packet.get(IpV4Packet.class);
         if( ipv4p != null )
         {
            // Inet4Address destAddr = ipv4p.getHeader().getDstAddr();

//            System.out.println("received: [nr.: " +counter +"]----> IPV4: "+ 
//                     ipv4p.getHeader().getSrcAddr() + " --> " +
//                     ipv4p.getHeader().getDstAddr() + ": len " +
//                     ipv4p.length());

            
         
            if (packet.contains(IcmpV4TimeExceededPacket.class)) 
            {  
               // tarceroute reply.
               
               IcmpV4TimeExceededPacket icmpTEp = packet.get(IcmpV4TimeExceededPacket.class);
               // System.out.println("received [nr.: " +counter +"] : "+packet);
               // System.out.println("received [nr.: " +counter +"] TTLEx: "+icmpTEp);
               // int depth = icmpTEp.get(IpV4Packet.class).getHeader().getTtl();
               // int depth  = icmpTEp.get(IpV4Packet.class).getPayload().getRawData()[0];
               Inet4Address origSrcAddr = (icmpTEp.get(IpV4Packet.class)).getHeader().getSrcAddr();
               Inet4Address origDstAddr = (icmpTEp.get(IpV4Packet.class)).getHeader().getDstAddr();
               int depth  = -1;
               try
               {
                  depth = icmpTEp.get(IcmpV4EchoPacket.class).getPayload().getRawData()[0];
               }
               catch (Exception e )
               {
                  // System.out.println("received [nr.: " +counter +"] : cought: "+e);
                  System.out.println("received [nr.: " +counter +"]: no data[] in IcmpV4EchoPacket from "+ipv4p.getHeader().getSrcAddr());
               }
                              
               if( depth == -1 )
               {
                  depth = mMain.mTraceRouter.getLastSentDepth();
               }
               
               System.out.println("received: [nr.: " +counter +"] src="+origSrcAddr +" reply from " + 
                        ipv4p.getHeader().getSrcAddr()  +", at depth: " + depth +", sent to "+origDstAddr);
               
               TraceRouteMsg trm = new TraceRouteMsg(mMain.mTraceRouter, ipv4p.getHeader().getSrcAddr(), depth+1);
               mMain.sendMsg ( trm );
                             
               TraceRouteNode trn = new TraceRouteNode(origSrcAddr, origDstAddr, ipv4p.getHeader().getSrcAddr(), depth);
               mModel.addTraceRouteNode(trn);
            }
            else if ( ( packet.contains(IcmpV4CommonPacket.class) && 
                      ( Model.equalsAddr( ipv4p.getHeader().getSrcAddr(), mMain.mTraceRouter.getTargetAddress()))  ) )
       
            {
               TraceRouteMsg trm = new TraceRouteMsg(mMain.mTraceRouter, 
                                                     ipv4p.getHeader().getSrcAddr(), 
                                                     mMain.mTraceRouter.getLastSentDepth()+1 );
               mMain.sendMsg ( trm );
               
               System.out.println("received: [nr.: " +counter +"] IcmpV4CommonPacket reply from " + 
                        ipv4p.getHeader().getSrcAddr());
               
            }
            else
            {
               boolean accept = true;

               byte[] srcAddressBytes = ipv4p.getHeader().getSrcAddr().getAddress();
               byte[] dstAddressBytes = ipv4p.getHeader().getDstAddr().getAddress();
               if( (srcAddressBytes[0] == -64) && (srcAddressBytes[1] == -88) && (srcAddressBytes[2] == 1) )
               {
                  accept = false;
               }

               if( (dstAddressBytes[0] == -64) && (dstAddressBytes[1] == -88) && (dstAddressBytes[2] == 1) )
               {
                  accept = false;
               }

               if( (dstAddressBytes[0] == -64) && (dstAddressBytes[1] == -88) && (dstAddressBytes[2] == 1) && (dstAddressBytes[3] == 44))
               {
                  // System.out.println("received [nr.: " +counter +"] IPv4: "+packet);
               }
               
               if( accept )
               {
                  synchronized( mModel  )
                  {
                     mModel.addIPv4Packet( /*pcapHandle.getTimestamp(),*/ ipv4p  );
                  }
               }
            }
         }
         else
         {
            ArpPacket arpp = packet.get(ArpPacket.class);
            if( arpp != null )
            {                               
               // dunno
//               System.out.println("[nr.:" +counter +"]----> ARP: "+ 
//                        arpp.getHeader().getSrcProtocolAddr() + " --> " +
//                        arpp.getHeader().getDstProtocolAddr() + ": len" + 
//                        arpp.length());
            }
            else
            {
               LinuxSllPacket lSllp =  packet.get(LinuxSllPacket.class);
               if( lSllp != null )
               {
                  // dunno
               }
               else
               {
                  EthernetPacket ep = packet.get(EthernetPacket.class);
                  if ( ep != null )
                  {
                     // still dunno
                  }
                  else
                  {
                     System.out.println("[ "+counter +"] unsorted package:");
                     String s = packet.toString();
                     System.out.println(s);
                  }
               }
            }
         }
      }

      mModel.mMain.mNetVisFrame.getNetVisComponent().repaint();
      // System.out.println("gotPacket() done, packets="+counter);
   }
   
   
}
