package netvis.traceroute;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

import netvis.NetVisMain;

public class TracerouteScheduler
{

   private final NetVisMain main;
   private final Traceroute traceroute;

   private class TracerouteTargetHost
   {
      Inet4Address targetAddress;
      public TracerouteTargetHost( Inet4Address ta )
      {
         targetAddress = ta;
      }
   }
   
   private Vector<TracerouteTargetHost> mTargetHosts;
   
   public TracerouteScheduler( NetVisMain m, Traceroute tr )
   {
      main = m;
      traceroute = tr;
      mTargetHosts = new Vector<TracerouteTargetHost>(100,100);
   }
   
   public void addTargetAddress( Inet4Address targetAddress )
   {
      if( targetAddress != null )
      {
         TracerouteTargetHost tth = new TracerouteTargetHost( targetAddress );
         mTargetHosts.add(tth);
      }
   }
   
   public void addTargetName( String targetName )
   {
      Inet4Address targetAddress = null;
      try 
      {
         targetAddress = (Inet4Address) InetAddress.getByName(targetName);
      } 
      catch (UnknownHostException e1) 
      {
         System.out.println("TraceRouteMsg<ctor>: " + targetName + " got "+e1);
      }
      
      addTargetAddress( targetAddress );
   }
   
   
   public Inet4Address getNextTargetAddress()
   {
      Inet4Address nextAddress = null;
      if( !mTargetHosts.isEmpty() )
      {
         TracerouteTargetHost tth = mTargetHosts.firstElement();
         nextAddress = tth.targetAddress;
         mTargetHosts.remove(0);
      }
      
      return ( nextAddress );
   }
   
   public void traceNextTarget()
   {
      Inet4Address nextAddress = getNextTargetAddress();
      
      if( nextAddress != null )
      {
         TraceRouteMsg trm = new TraceRouteMsg(traceroute, nextAddress);
         main.sendMsg ( trm );
      }
   }
}
