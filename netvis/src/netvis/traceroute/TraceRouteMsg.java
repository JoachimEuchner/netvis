package netvis.traceroute;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netvis.NetVisMsg;
import netvis.NetVisMsgReceiver;

public class TraceRouteMsg
   extends NetVisMsg
{
   private static final Logger logger = LoggerFactory.getLogger(TraceRouteMsg.class);
   private Inet4Address ipV4Addr;
   private int receivedDepth;
   
   public TraceRouteMsg( NetVisMsgReceiver localRec, Inet4Address addr, int depth )
   {
      super( localRec );
      ipV4Addr = addr;
      receivedDepth = depth;
   }
   
   public TraceRouteMsg( NetVisMsgReceiver localRec, Inet4Address addr )
   {
      super( localRec );
      ipV4Addr = addr;
      receivedDepth = -1;
   }
   
   public TraceRouteMsg( NetVisMsgReceiver localRec, String targetName )
   {
      super( localRec );
      
      try 
      {
         ipV4Addr = (Inet4Address) InetAddress.getByName(targetName);
      } 
      catch (UnknownHostException e1) 
      {
         logger.debug("TraceRouteMsg<ctor>: {} cought {} ", targetName, e1);
      }
      
      receivedDepth = -1;
   }
   
   
   public Inet4Address getAddr()
   {
      return ipV4Addr;
   }
   
   
   public int getDepth()
   {
      return receivedDepth;
   }
}
