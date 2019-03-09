package netvis.traceroute;

import java.net.Inet4Address;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.core.PcapHandle.BlockingMode;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IcmpV4CommonPacket;
import org.pcap4j.packet.IcmpV4EchoPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Rfc791Tos;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UnknownPacket;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.packet.namednumber.IcmpV4Code;
import org.pcap4j.packet.namednumber.IcmpV4Type;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.IpVersion;
import org.pcap4j.util.MacAddress;

import netvis.NetVisMain;
import netvis.NetVisMsg;
import netvis.NetVisMsgReceiver;
import netvis.model.Model;


public class Traceroute
   implements NetVisMsgReceiver
{
   private class TraceRouteTimerReceiver extends TimerTask
   {
      public void run()
      {
         try
         {
            timeoutOccured ( 1 );
         }
         catch ( Exception e )
         {
            System.out.println("TraceRouteTimerReceiver.run(): cought: "+e);
         }
      }
   }
   TraceRouteTimerReceiver mTimerReceiver;
  
   
   private static final String READ_TIMEOUT_KEY =
            Traceroute.class.getName() + ".readTimeout";
   private static final int READ_TIMEOUT = Integer.getInteger(READ_TIMEOUT_KEY, 10); // [ms]
   
   private static final String SNAPLEN_KEY = Traceroute.class.getName() + ".snaplen";
   private static final int SNAPLEN = Integer.getInteger(SNAPLEN_KEY, 65536); // [bytes]

   private static final String TU_KEY = Traceroute.class.getName() + ".tu";
   private static final int TU = Integer.getInteger(TU_KEY, 40); // [bytes] 
 
   
   private Inet4Address srcAddress;
   private Inet4Address mTargetAddress;
   private MacAddress srcMac;
   private MacAddress dstMac;
   
   private Timer timer;
   private final NetVisMain main;
   private PcapHandle sendHandle;
 
   private Integer mLastSentDepth = -1;
    
   public int getLastSentDepth()
   {
      int depth;
      synchronized ( mLastSentDepth )
      {
         depth = mLastSentDepth.intValue();
      }
      return depth;
   }
   private void setLastSentDepth( int depth )
   {
      synchronized ( mLastSentDepth )
      {
         mLastSentDepth = depth ;
      }
   }
   
   
  
   boolean targetAddressLocked = false;
   public void setTargetAddess( Inet4Address t )
   {
      // System.out.println("tr.setTarget: "+t+" called, locked: "+targetAddressLocked);
      if( !targetAddressLocked )
      {
         byte[] srcAddressBytes = t.getAddress();
         
         if(!( (srcAddressBytes[0] == 127) && (srcAddressBytes[1] == 0) && (srcAddressBytes[2] == 0) ) )
         {
            if(!( (srcAddressBytes[0] == -64) && (srcAddressBytes[1] == -88) && (srcAddressBytes[2] == 1) ) )
            {
               if(!( (srcAddressBytes[0] == -64) && (srcAddressBytes[1] == -88) && (srcAddressBytes[2] == 2) ) )
               {
                  if ( !Model.equalsAddr(t, mTargetAddress))
                  {
                     System.out.println("tr.setTarget: "+t);
                  }
                  mTargetAddress = t;
               }
            }
         }
      }
      // System.out.println("tr.setTarget: "+t+", now: "+mTargetAddress);
   }
   public Inet4Address getTargetAddress()
   {
      return mTargetAddress;
   }
   

   public Traceroute( NetVisMain m )
   {
      main = m;     
          
      mTimerReceiver = new TraceRouteTimerReceiver();
      timer = new Timer("TraceRouteTimer", false);
      // Schedule task to start immediately and re-fire every second...
      // timer.scheduleAtFixedRate(mTimerReceiver, (long)0, (long)1000);
     
   }
   
   public void initialize(Inet4Address _srcAddr,
                          MacAddress _srcMac, 
                          MacAddress _dstMac )
   {
      srcAddress = _srcAddr;
      srcMac = _srcMac;
      dstMac = _dstMac;
      
      List<PcapNetworkInterface> allDevs = null;
      try 
      {
         allDevs = Pcaps.findAllDevs();
      } 
      catch (PcapNativeException e) 
      {
         e.printStackTrace();
      }

      int nifIdx = 0;
      PcapNetworkInterface nif = allDevs.get(nifIdx);
   
      try
      {
         sendHandle = nif.openLive(SNAPLEN, PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);
         sendHandle.setBlockingMode(BlockingMode.NONBLOCKING);
      } 
      catch (PcapNativeException e1)
      {
         e1.printStackTrace();
      }
      catch (NotOpenException noe )
      {
         noe.printStackTrace();
      } 
      
      System.out.println("tr.initialize() done, got: nifIdx:"+nifIdx+" and nif "+nif.getName());
   }
   
   
   private void sendICMPPackage( Inet4Address dst, int ttl )
   {
      // System.out.println("sendICMPPackage("+dst+", "+ttl+") called.");
      
      IpV4Packet.Builder ipV4Builder = new IpV4Packet.Builder();
      byte[] echoData = new byte[TU - 28];
      for (int i = 0; i < echoData.length; i++) {
         echoData[i] = (byte) i;
      }
      echoData[0] = (byte)ttl;

      IcmpV4EchoPacket.Builder echoBuilder = new IcmpV4EchoPacket.Builder();
      echoBuilder.identifier((short) 1)
      .payloadBuilder(new UnknownPacket.Builder().rawData(echoData));

      IcmpV4CommonPacket.Builder icmpV4CommonBuilder = new IcmpV4CommonPacket.Builder();
      icmpV4CommonBuilder.type(IcmpV4Type.ECHO)
               .code(IcmpV4Code.NO_CODE).payloadBuilder(echoBuilder)
               .correctChecksumAtBuild(true);

      ipV4Builder.version(IpVersion.IPV4).tos(IpV4Rfc791Tos.newInstance((byte) 0))
               .ttl((byte) ttl) 
               .protocol(IpNumber.ICMPV4)
               .srcAddr(srcAddress)
               .dstAddr(mTargetAddress)
               .payloadBuilder(icmpV4CommonBuilder)
               .correctChecksumAtBuild(true)
               .correctLengthAtBuild(true);

      EthernetPacket.Builder etherBuilder = new EthernetPacket.Builder();
      etherBuilder.dstAddr(MacAddress.ETHER_BROADCAST_ADDRESS)
               .srcAddr(srcMac)
               .dstAddr(dstMac)
               .type(EtherType.IPV4)
               .payloadBuilder(ipV4Builder)
               .paddingAtBuild(true);

      Packet p = etherBuilder.build();
     
      try
      {
         sendHandle.sendPacket(p);
         setLastSentDepth( ttl );
         
         try
         {
            mTimerReceiver.cancel();
            mTimerReceiver = new TraceRouteTimerReceiver();
         }
         catch ( java.lang.IllegalStateException ise )
         {
            System.out.println("sendICMPPackage() 1. cought: "+ ise);
         }
         
         try
         {
            timer.schedule(mTimerReceiver, (long)2000);
         }
         catch ( java.lang.IllegalStateException ise )
         {
            System.out.println("sendICMPPackage() 2. cought: "+ ise);
         }
         
            
      } 
      catch (PcapNativeException e)
      { 
         e.printStackTrace();
      } 
      catch (NotOpenException e)
      {
         e.printStackTrace();
      }
         
      // System.out.println("sendICMPPackage("+dst+", "+ttl+") done.");
   }
   
  
   public void msgReceived( NetVisMsg msg )
   {
      TraceRouteMsg trm = (TraceRouteMsg) msg ;      
      System.out.println("tr.msgReceived() got TraceRouteMsg("+trm.getAddr()+", "+trm.getDepth()+")");
      
      if( ( trm.getDepth() == -1 ) && ( trm.getAddr() != null ) )
      {
         // start new traceroute.
         setTargetAddess( trm.getAddr() );
         sendICMPPackage( mTargetAddress, 2 );
      }
      else
      {
         if( ( trm.getDepth() < 15 ) && ( mTargetAddress != null ) )
         {
            if( !Model.equalsAddr(mTargetAddress, trm.getAddr()) )
            {
               sendICMPPackage( mTargetAddress, trm.getDepth()+1 );
            }
            else
            {
               System.out.println("tr.msgReceived() found targetHost, this traceroute completed.");
               main.mTracerouteScheduler.traceNextTarget();
            }
         }
      }
   }
   
   
   public void timeoutOccured(int id)
   {
      System.out.println("tr.timeoutOccured("+id+") called.");
      
      if( mTargetAddress != null )
      {
         if( getLastSentDepth() < 15 )
         {
            TraceRouteMsg trm = new TraceRouteMsg(this, null, getLastSentDepth()+1);
            main.sendMsg ( trm );
         }
         else
         {
            // giving up on mTargetHost.
            System.out.println("tr.timeoutOccured() giving up on: "+ mTargetAddress);
            main.mTracerouteScheduler.traceNextTarget();
         }
      }
   }
   
 
}
