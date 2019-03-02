package netvis.traceroute;

import java.net.Inet4Address;

public class TraceRouteNode extends netvis.model.Node
{
   public Inet4Address mSrcAddr;
   public Inet4Address mOrigDstAddr;
   public Inet4Address mReplyingAddr;
  
   public int          mDepth;
   
   public int          mObservedTimes;
   
   public TraceRouteNode( Inet4Address srcAddr, Inet4Address origDstAddr, Inet4Address replyingAddr, int depth )
   {
      super( replyingAddr );
      mSrcAddr = srcAddr;
      mOrigDstAddr = origDstAddr;
      mReplyingAddr =  replyingAddr;
      mDepth = depth;      
      mObservedTimes = 1;
      
      type = TYPE_ROUTEPOINT;
      
      timeOfLastSeenPacket = System.currentTimeMillis();
      
      mDisplayName = mDisplayName+"@"+depth;
   }
  
}
