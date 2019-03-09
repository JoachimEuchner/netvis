package netvis.traceroute;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import netvis.NetVisMsg;
import netvis.NetVisMsgReceiver;

public class TraceRouteMsg
   extends NetVisMsg
{
   private Inet4Address ipV4Addr;
   private int receivedDepth;
   
   public TraceRouteMsg( NetVisMsgReceiver _rec, Inet4Address addr, int depth )
   {
      super( _rec );
      ipV4Addr = addr;
      receivedDepth = depth;
   }
   
   public TraceRouteMsg( NetVisMsgReceiver _rec, String targetName )
   {
      super( _rec );
      
      try 
      {
         ipV4Addr = (Inet4Address) InetAddress.getByName(targetName);
      } 
      catch (UnknownHostException e1) 
      {
         System.out.println("TraceRouteMsg<ctor>: " + targetName + " got "+e1);
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
