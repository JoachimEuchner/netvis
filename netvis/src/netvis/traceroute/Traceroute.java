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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netvis.NetVisMain;
import netvis.NetVisMsg;
import netvis.NetVisMsgReceiver;
import netvis.model.Model;


public class Traceroute
   implements NetVisMsgReceiver
{   
   private static final Logger logger = LoggerFactory.getLogger(Traceroute.class);

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
            logger.warn("TraceRouteTimerReceiver.run(): cought: {}", e);
         }
      }
   }
   TraceRouteTimerReceiver mTimerReceiver;
  
   public static final int TRACEROUTE_STATE_IDLE = 0;
   public static final int TRACEROUTE_STATE_TRACING_ACTIVE = 1;
   private int mState = TRACEROUTE_STATE_IDLE;
   public int getState() {return mState;};
   
   public static final int TRACEROUTE_MAXDEPTH = 32;
   
   private static final String READ_TIMEOUT_KEY =
            Traceroute.class.getName() + ".readTimeout";
   private static final int READ_TIMEOUT = Integer.getInteger(READ_TIMEOUT_KEY, 10); // [ms]
   
   private static final String SNAPLEN_KEY = Traceroute.class.getName() + ".snaplen";
   private static final int SNAPLEN = Integer.getInteger(SNAPLEN_KEY, 65536); // [bytes]

   private static final String TU_KEY = Traceroute.class.getName() + ".tu";
   private static final int TU = Integer.getInteger(TU_KEY, 40); // [bytes] 
 
   
   private Inet4Address srcAddress;
   public Inet4Address getSrcAddress() { return srcAddress; };
   private Inet4Address mTargetAddress;
   private MacAddress srcMac;
   private MacAddress dstMac;
   
   private Timer timer;
   private final NetVisMain main;
   private PcapHandle sendHandle;
 
   private Object mLastSentDepthObject;
   private int mLastSentDepth; 
    
   
   public int getLastSentDepth()
   {
      int depth;
      synchronized ( mLastSentDepthObject )
      {
         depth = mLastSentDepth;
      }
      return depth;
   }
   private void setLastSentDepth( int depth )
   {
      synchronized ( mLastSentDepthObject )
      {
         mLastSentDepth = depth ;
      }
   }
   
   boolean targetAddressLocked = false;
   private boolean acceptTarget( Inet4Address t )
   {
      boolean accept = true;
      byte[] srcAddressBytes = t.getAddress();
      
      if( (srcAddressBytes[0] == 127) && (srcAddressBytes[1] == 0) && (srcAddressBytes[2] == 0) ) 
      { 
         // localhost
         accept = false;
      }
      else if( (srcAddressBytes[0] == -64) && (srcAddressBytes[1] == -88) && (srcAddressBytes[2] == 1) )
      {
         // 192.168.1.0/24
         accept = false;
      }
      else  if ( (srcAddressBytes[0] == -64) && (srcAddressBytes[1] == -88) && (srcAddressBytes[2] == 2) ) 
      {
         // 192.168.2.0/24
         accept = false;
      }
      return accept;
   }
   
   public void setTargetAddess( Inet4Address t )
   {
      logger.trace("tr.setTarget: {} called, locked: {}", t, targetAddressLocked);
      if( !targetAddressLocked )
      {
         if( acceptTarget( t ) )
         {
            if ( !Model.equalsAddr(t, mTargetAddress))
            {
               logger.debug("tr.setTarget: {}", t);
            }
            mTargetAddress = t;
         }
      }
      logger.trace("tr.setTarget({}), now: {}", t, mTargetAddress);
   }
   
   public Inet4Address getTargetAddress()
   {
      return mTargetAddress;
   }
   

   public Traceroute( NetVisMain m )
   {
      main = m;     
          
      mState = TRACEROUTE_STATE_IDLE;
      
      mLastSentDepthObject = new Object();
      mLastSentDepth = -1;
      
      mTimerReceiver = new TraceRouteTimerReceiver();
      timer = new Timer("TraceRouteTimer", false);
      // Schedule task to start immediately and re-fire every second...
      // timer.scheduleAtFixedRate(mTimerReceiver, (long)0, (long)1000);
     
   }
   
   public void initialize(Inet4Address tmpSrcAddr,
                          MacAddress tmpSrcMac, 
                          MacAddress tmpDstMac )
   {
      srcAddress = tmpSrcAddr;
      srcMac = tmpSrcMac;
      dstMac = tmpDstMac;
      
      List<PcapNetworkInterface> allDevs = null;
      try 
      {
         allDevs = Pcaps.findAllDevs();
      } 
      catch (PcapNativeException e) 
      {
         logger.error("cought native {}", e );
      }

      if( allDevs != null )
      {
         int nifIdx = 0;
         PcapNetworkInterface nif = allDevs.get(nifIdx);

         if( nif != null )
         {
            try
            {
               sendHandle = nif.openLive(SNAPLEN, PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);
               sendHandle.setBlockingMode(BlockingMode.NONBLOCKING);
            } 
            catch (PcapNativeException e1)
            {
               logger.error("cought native {}", e1 );
            }
            catch (NotOpenException noe )
            {
               logger.error("cought not open {}", noe );
            }
            
            logger.info("tr.initialize() done, got: nifIdx: {} and nif {}", nifIdx, nif.getName());
            mState = TRACEROUTE_STATE_IDLE;
         }
         else
         {
            logger.error("tr.initialize() got: null from  allDevs.get({})", nifIdx);
         }

        
         
      }
      else
      {
         logger.error("tr.initialize() got: allDevs = null.");
      }
   }
   
   
   private void sendICMPPackage( Inet4Address dst, int ttl )
   {
      logger.debug("tr.sendICMPPackage({}, {}) called.", dst, ttl);
                   
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
               .code(IcmpV4Code.NO_CODE)
               .payloadBuilder(echoBuilder)
               .correctChecksumAtBuild(true);

      IpV4Packet.Builder ipV4Builder = new IpV4Packet.Builder();
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

      Packet p = etherBuilder.build();  // --> INFO org.pcap4j.util.PropertiesLoader - [org/pcap4j/packet/packet.properties] Got"true" which means true by org.pcap4j.packet.ipV4.calcChecksumAtBuild
     
      try
      {
         sendHandle.sendPacket(p);
         logger.trace("sendICMPPackage({}, {}) sent icmp-package.", dst, ttl);
         
         setLastSentDepth( ttl );
         
         try
         {
            mTimerReceiver.cancel();
            mTimerReceiver = new TraceRouteTimerReceiver();
         }
         catch ( java.lang.IllegalStateException ise )
         {
           logger.warn("sendICMPPackage() 1. cought: {} ", ise);
         }
         
         try
         {
            long timeForOneProbe = 3500;
            timer.schedule(mTimerReceiver, timeForOneProbe);
            logger.trace("sendICMPPackage({}, {}) started timer with {} ms", dst, ttl, timeForOneProbe);
         }
         catch ( java.lang.IllegalStateException ise )
         {
            logger.warn("sendICMPPackage() 2. cought: {}", ise);
         }            
      } 
      catch (PcapNativeException e1)
      { 
         logger.error( "sendPacket: cought native: {}", e1 );
      } 
      catch (NotOpenException e2)
      {
         logger.error( "sendPacket: cought not open: {}", e2 );
      }
         
      logger.trace("sendICMPPackage({}, {}) done.", dst, ttl);
   }
   
  
   public void msgReceived( NetVisMsg msg )
   {
      TraceRouteMsg trm = (TraceRouteMsg) msg ;      
           
      if( ( trm.getDepth() == -1 ) && ( trm.getAddr() != null ) )
      {
         // start new traceroute.
         logger.debug("##### tr.msgReceived() got TraceRouteMsg(): start new traceroute to {}", trm.getAddr());
         setTargetAddess( trm.getAddr() );
         mState = TRACEROUTE_STATE_TRACING_ACTIVE;
         sendICMPPackage( mTargetAddress, 1 );
      }
      else
      {
         if( mTargetAddress != null ) 
         {
            if( !Model.equalsAddr(mTargetAddress, trm.getAddr()) )
            {
               mState = TRACEROUTE_STATE_TRACING_ACTIVE;
               sendICMPPackage( mTargetAddress, trm.getDepth() );
            }
            else
            {
               logger.debug("tr.msgReceived() found targetHost, this traceroute completed.");
               mState = TRACEROUTE_STATE_IDLE;
               main.getTRScheduler().traceNextTarget();
            }
         }
      }
   }
   
   
   public void timeoutOccured(int id)
   {
      logger.info("tr.timeoutOccured({}) called. mTargetAddress={}, lastDepth:{}, mState={}", id, mTargetAddress, getLastSentDepth(), mState);
      
      if( mTargetAddress != null )
      {
         if( ( getLastSentDepth() < TRACEROUTE_MAXDEPTH ) && ( TRACEROUTE_STATE_TRACING_ACTIVE == mState ) )
         {
            TraceRouteMsg trm = new TraceRouteMsg(this, null, getLastSentDepth()+1);
            main.sendMsg ( trm );
         }
         else
         {
            // giving up on mTargetHost.
            if( TRACEROUTE_STATE_TRACING_ACTIVE == mState )
            {
               logger.debug("tr.timeoutOccured() giving up on: {}", mTargetAddress);
               mState = TRACEROUTE_STATE_IDLE;
               main.getTRScheduler().traceNextTarget();
            }
         }
      }
   }
   
 
}
