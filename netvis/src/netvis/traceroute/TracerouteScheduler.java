package netvis.traceroute;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netvis.NetVisMain;
import netvis.model.Model;

public class TracerouteScheduler
{
   private static final Logger logger = LoggerFactory.getLogger(TracerouteScheduler.class);
   
   public static final int TRACEROUTESCHEDULER_STATE_READY = 0;
   public static final int TRACEROUTESCHEDULER_STATE_TRACING_ACTIVE = 1;
   public static final int TRACEROUTESCHEDULER_STATE_PAUSE = 2;
   private int mState;
   public int getState() {return mState;}
   
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
            logger.error("TraceRouteTimerReceiver: cought: {}", e);
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
         return ( Model.equalsAddr(targetAddress, tth.targetAddress));
      }
   }
   
   private ArrayList<TracerouteTargetHost> mTargetHosts;
   
   public TracerouteScheduler( NetVisMain m, Traceroute tr )
   {
      main = m;
      traceroute = tr;
      
      mState = TRACEROUTESCHEDULER_STATE_READY;
      
      mTargetHosts = new ArrayList<>(100);

      mTimerReceiver = new TracerouteSchedulerTimerReceiver();
      timer = new Timer("TraceRouteTimer", false);
   }
   
   public void addTargetAddress( Inet4Address targetAddress )
   {
      if( targetAddress != null )
      {
         byte[] addressBytes = targetAddress.getAddress();      
         
         
         if( (addressBytes[0] == -64) && (addressBytes[1] == -88) && (addressBytes[2] == 1) )
         {
            return;
         }
         
         if( (addressBytes[0] == -64) && (addressBytes[1] == -88) && (addressBytes[2] == 2) )
         {
            return;
         }
         
         if ( (addressBytes[0] == 127) && (addressBytes[1] == 0) && (addressBytes[2] == 0) )
         {
            return;
         }
         
         if ( (addressBytes[0] == 127) && (addressBytes[1] == 0) && (addressBytes[2] == 0) )
         {
            return;
         }
         
         if ( (addressBytes[0] == 0) && (addressBytes[1] == 0) && (addressBytes[2] == 0)  && (addressBytes[3] == 0))
         {
            return;
         }
         
         TracerouteTargetHost tth = new TracerouteTargetHost( targetAddress );
         
         if (!mTargetHosts.contains( tth ))
         {   
            mTargetHosts.add(tth);
         }
         
         if(( mTargetHosts.size() == 1) 
                  && ( traceroute.getState() == Traceroute.TRACEROUTE_STATE_IDLE)
                  && ( TRACEROUTESCHEDULER_STATE_READY == mState ) )
         {
            // first call:
            traceNextTarget();
         }
      }
   }
   
   public void addTargetName( String targetName )
   {
      Inet4Address targetAddress = null;
      
      logger.info("trs.addTargetName(\"{}\") called.", targetName);
      
      if(!targetName.equalsIgnoreCase("localhost"))
      {

         try 
         {
            targetAddress = (Inet4Address) InetAddress.getByName(targetName);
         } 
         catch (UnknownHostException e1) 
         {
            logger.warn("TraceRouteMsg<ctor>: {} cought {}", targetName, e1);
         }

         addTargetAddress( targetAddress );
      }
   }
   
   
   public Inet4Address getNextTargetAddress()
   {
      TracerouteTargetHost tth = null;
      Inet4Address nextAddress = null;
      if( !mTargetHosts.isEmpty() )
      {
         tth = mTargetHosts.get(0);
         nextAddress = tth.targetAddress;
         mTargetHosts.remove(0);
      }
      
      logger.debug("trs.getNextTargetAddress(): {}, left with {} targets",
                     nextAddress, mTargetHosts.size());
      
      mTargetHosts.add(tth);  // re-append to start loop
      return ( nextAddress );
   }
   
   
   public void traceNextTarget()
   {
      logger.debug("trs.traceNextTarget(): got {} targets, mState={}", mTargetHosts.size(), mState );
      
      if( TRACEROUTESCHEDULER_STATE_READY == mState  )
      {
         Inet4Address nextAddress = getNextTargetAddress();
         if( nextAddress != null )
         {
            logger.debug("trs.traceNextTarget(): start traceroute {} from {} targets, mState={}", nextAddress, mTargetHosts.size(), mState );
            TraceRouteMsg trm = new TraceRouteMsg(traceroute, nextAddress);
            main.sendMsg ( trm );
            mState = TRACEROUTESCHEDULER_STATE_TRACING_ACTIVE;
         }
      }
      else if( TRACEROUTESCHEDULER_STATE_TRACING_ACTIVE == mState )
      {
         try
         {
            mTimerReceiver.cancel();
            mTimerReceiver = new TracerouteSchedulerTimerReceiver();
            timer.schedule(mTimerReceiver, (long)20000);
         }
         catch ( java.lang.IllegalStateException ise )
         {
            logger.warn("getNextTargetAddress() cought: {}", ise);
         }
         mState = TRACEROUTESCHEDULER_STATE_PAUSE;
      }
   }
   
   public void timeoutOccured(int id)
   {
      logger.info("trs.timeoutOccured({}) called. mState={}", id, mState);
       
      if( TRACEROUTESCHEDULER_STATE_PAUSE == mState )
      {
         mState = TRACEROUTESCHEDULER_STATE_READY;
         traceNextTarget();
      }
   }
}
