package netvis.traceroute;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import netvis.NetVisMain;

public class TracerouteScheduler
{
   private class TracerouteSchedulerTimerReceiver extends TimerTask
   {
      public void run()
      {
         try
         {
            timeoutOccured ( 1 );
         }
         catch ( Exception e )
         {
            System.out.println("TraceRouteTimerReceiver: cought: "+e);
         }
      }
   }
   TracerouteSchedulerTimerReceiver mTimerReceiver;
   
   
   private Timer timer;
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

      mTimerReceiver = new TracerouteSchedulerTimerReceiver();
      timer = new Timer("TraceRouteTimer", false);
   
   }
   
   public void addTargetAddress( Inet4Address targetAddress )
   {
      if( targetAddress != null )
      {
         byte[] addressBytes = targetAddress.getAddress();      
         
         if ( (addressBytes[0] == 127) && (addressBytes[1] == 0) && (addressBytes[2] == 0) )
         {
            return;
         }
         
         TracerouteTargetHost tth = new TracerouteTargetHost( targetAddress );
         mTargetHosts.add(tth);
      }
   }
   
   public void addTargetName( String targetName )
   {
      Inet4Address targetAddress = null;
      
      if(!targetName.equalsIgnoreCase("localhost"))
      {

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
      else
      {
         try
         {
            mTimerReceiver.cancel();
            mTimerReceiver = new TracerouteSchedulerTimerReceiver();
            timer.schedule(mTimerReceiver, (long)5000);
         }
         catch ( java.lang.IllegalStateException ise )
         {
            System.out.println("getNextTargetAddress() cought: "+ ise);
         }
         
      }
      
      System.out.println("trs.getNextTargetAddress(): "+
               nextAddress+", "+ mTargetHosts.size() +" targets");
      
      
      return ( nextAddress );
   }
   
   public void traceNextTarget()
   {
      Inet4Address nextAddress = getNextTargetAddress();
      
      System.out.println("trs.traceNextTarget(): "+nextAddress );
      
      if( nextAddress != null )
      {
         TraceRouteMsg trm = new TraceRouteMsg(traceroute, nextAddress);
         main.sendMsg ( trm );
      }
   }
   
   public void timeoutOccured(int id)
   {
      System.out.println("trs.timeoutOccured("+id+") called.");
      traceNextTarget();
   }
}
