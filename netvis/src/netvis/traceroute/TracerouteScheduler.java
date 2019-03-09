package netvis.traceroute;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netvis.NetVisMain;
import netvis.model.Model;

public class TracerouteScheduler
{
   private static final Logger logger = LoggerFactory.getLogger(TracerouteScheduler.class);
   
   
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
            logger.error("TraceRouteTimerReceiver: cought: "+e);
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
      
      public boolean equals( Object o )
      { 
         if(o == null || !getClass().equals(o.getClass()))  
            return false;
         if(o == this)  
            return true;

         TracerouteTargetHost tth = (TracerouteTargetHost) o;
         // return this.key.compareTo(tth.key) == 0 ? true : false;
         return ( Model.equalsAddr(targetAddress, tth.targetAddress));
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
         
         if (!mTargetHosts.contains( tth ))
         {   
            mTargetHosts.add(tth);
         }
      }
   }
   
   public void addTargetName( String targetName )
   {
      Inet4Address targetAddress = null;
      
      logger.info("trs.addTargetName(\"" + targetName +"\") called.");
      
      if(!targetName.equalsIgnoreCase("localhost"))
      {

         try 
         {
            targetAddress = (Inet4Address) InetAddress.getByName(targetName);
         } 
         catch (UnknownHostException e1) 
         {
            logger.warn("TraceRouteMsg<ctor>: " + targetName + " got "+e1);
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
            timer.schedule(mTimerReceiver, (long)20000);
         }
         catch ( java.lang.IllegalStateException ise )
         {
            logger.warn("getNextTargetAddress() cought: "+ ise);
         }
         
      }
      
      System.out.println("trs.getNextTargetAddress(): "+
               nextAddress+", "+ mTargetHosts.size() +" targets");
      
      
      return ( nextAddress );
   }
   
   public void traceNextTarget()
   {
      Inet4Address nextAddress = getNextTargetAddress();
      
      logger.trace("trs.traceNextTarget(): "+nextAddress+" from "+mTargetHosts.size() +" targets" );
      
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
