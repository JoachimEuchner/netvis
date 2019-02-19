package netvis.traceroute;

import java.net.Inet4Address;

public class TraceRouteNode
{
   Inet4Address mSrcAddr;
   Inet4Address mOrigDstAddr;
   Inet4Address mReplyingAddr;
  
   int          mDepth;
   
   public TraceRouteNode( Inet4Address srcAddr, Inet4Address origDstAddr, Inet4Address replyingAddr, int depth )
   {
      mSrcAddr = srcAddr;
      mOrigDstAddr = origDstAddr;
      mReplyingAddr =  replyingAddr;
      mDepth = depth;      
   }
   
   
}
