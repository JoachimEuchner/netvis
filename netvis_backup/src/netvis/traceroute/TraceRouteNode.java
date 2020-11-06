package netvis.traceroute;

import java.net.Inet4Address;

public class TraceRouteNode extends netvis.model.Node
{
   private final Inet4Address mSrcAddr;
   public Inet4Address getSrc() { return mSrcAddr; }
   private final Inet4Address mOrigDstAddr;
   public Inet4Address getDst() { return mOrigDstAddr; }
   private final Inet4Address mReplyingAddr;
   public Inet4Address getReplyingAddr() { return mReplyingAddr; }
  
   private final int mDepth;
   public int getDepth() { return mDepth; }
   
   private int          mObservedTimes;
   public int getObservedTimes() { return mObservedTimes; }
   public void incObservedTimes() { mObservedTimes++; }
   
   public TraceRouteNode( Inet4Address srcAddr, Inet4Address origDstAddr, Inet4Address replyingAddr, int depth )
   {
      super( replyingAddr );
      mSrcAddr = srcAddr;
      mOrigDstAddr = origDstAddr;
      mReplyingAddr =  replyingAddr;
      mDepth = depth;      
      mObservedTimes = 1;
      
      super.mType = TYPE_ROUTEPOINT;
      
      super.timeOfLastSeenPacket = System.currentTimeMillis();
      
      mDisplayName = mDisplayName+"@"+depth;
   }
  
}
