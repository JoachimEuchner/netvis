package netvis;

import java.io.EOFException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapHandle.BlockingMode;
import org.pcap4j.core.PcapHandle.TimestampPrecision;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netvis.model.Model;
import netvis.traceroute.Traceroute;
import netvis.traceroute.TracerouteScheduler;
import netvis.ui.NetVisFrame;

public class NetVisMain
{
   private static final  Logger logger = LoggerFactory.getLogger(NetVisMain.class);
   
   PcapNetworkInterface nif;
   
   static NetVisMain handle;
   private NetVisPackageListener nvpl;
   
   private Model mNetVisModel;
   public Model getNetVisModel() { return mNetVisModel; }
   private NetVisFrame mNetVisFrame;
   public NetVisFrame getNetVisFrame() { return mNetVisFrame; }
   
   private Traceroute mTraceRouter;
   public Traceroute getTracerouter() { return mTraceRouter; }
   private TracerouteScheduler mTracerouteScheduler;
   public TracerouteScheduler getTRScheduler() { return mTracerouteScheduler; }
   
   
   private static final String COUNT_KEY = NetVisMain.class.getName() + ".count";
   private static final int COUNT = Integer.getInteger(COUNT_KEY, 1000);

   private static final String READ_TIMEOUT_KEY = NetVisMain.class.getName() + ".readTimeout";
   private static final int READ_TIMEOUT = Integer.getInteger(READ_TIMEOUT_KEY, 100); // [ms]

   private static final String SNAPLEN_KEY = NetVisMain.class.getName() + ".snaplen";
   private static final int SNAPLEN = Integer.getInteger(SNAPLEN_KEY, 65536); // [bytes]
  
   private PcapHandle pcapHandle; 
  
   public PcapHandle getPcapHandle()
   {
      return pcapHandle;
   }
   
   public PcapNetworkInterface getNif()
   {
      return nif;
   }
   
   private boolean mIsOnline;
   public boolean isOnline() {return mIsOnline;}
   
   MyListeningStarter mLs;
  
   Thread mListeningStartThread; 
   Thread mFileReaderThread;
   
   private final BlockingQueue<NetVisMsg> mQueue;
   public BlockingQueue<NetVisMsg> getQueue()
   {
      return mQueue;
   }
   private class MainCallable implements Callable<NetVisMsg>
   {
      @Override
      public NetVisMsg call()
      {
         NetVisMsg msg = null;
         try 
         {
            boolean receive = true;          
            while ( receive ) 
            { 
               msg = mQueue.take();
               NetVisMsgReceiver receiver = msg.getMsgReceiver();
               try
               {
                  receiver.msgReceived(msg);
               }
               catch( Exception e)
               {
                  receive=false;
                  logger.error("MainCallable: cought: {}.",e);
               }
            }
         } 
         catch (InterruptedException e) 
         {
            logger.error("MainCallable: exiting call().");
            // Allow our thread to be interrupted
            Thread.currentThread().interrupt();
            return ( null ); // this will never run, but the compiler needs it
         }
         return ( msg );
      }
   }
   public void sendMsg( NetVisMsg msg )
   {
      BlockingQueue<NetVisMsg> theQueue = this.getQueue();
      try
      {
         theQueue.put(msg);
      } 
      catch (InterruptedException e)
      {
         logger.error("sendMsg() cought {}.", e);
      }
   }
   
  
 
   private NetVisMain(String[] args) 
   {
      logger.info("info:NetVisMain.<ctor>() called.");
      
      mNetVisModel = new Model( this );
      mNetVisFrame = new NetVisFrame( this );
      

      this.mQueue = new ArrayBlockingQueue<>(100);
      ExecutorService threadPool = Executors.newFixedThreadPool(1); 
      /*Future<NetVisMsg> sum =*/ threadPool.submit(new MainCallable()); 
      
      mTraceRouter = new Traceroute( this );
      mTracerouteScheduler = new TracerouteScheduler( this, mTraceRouter );
      
      if( args.length == 0 )
      {
         mIsOnline = true;

         nvpl = new NetVisPackageListener( this, pcapHandle, mNetVisModel );
         mLs = new MyListeningStarter();

         mListeningStartThread = new Thread(mLs );
         mListeningStartThread.start();
         
         try
         {
            Inet4Address srcAddress = (Inet4Address) InetAddress.getByName("192.168.1.44");
            MacAddress srcMac = MacAddress.getByName("00:e0:4c:69:13:c7");
            MacAddress dstMac = MacAddress.getByName("34:31:c4:33:ce:ee");
            mTraceRouter.initialize(srcAddress, srcMac, dstMac );
         } 
         catch (UnknownHostException e)
         {
            logger.error( "cought: {}.", e);
         }
       
         
         Watchdog wd = new Watchdog();
         Thread watchdogThread = new Thread( wd );
         watchdogThread.start();
      }
      else
      {
         FileReader fr = new FileReader();
         
         nvpl = new NetVisPackageListener( this, pcapHandle, mNetVisModel );
         
         mIsOnline = false;
         mFileReaderThread = new Thread( fr );
         
         mFileReaderThread.start();
      }
      
      mNetVisFrame.setSize(1000, 500);
      mNetVisFrame.setVisible( true );
   }     
     
   
   private class FileReader implements Runnable
   {      
      public void run()
      {
         logger.debug("FileReader.run() called");
         
         try 
         {
            String PCAP_FILE_KEY = NetVisMain.class.getName() + ".pcapFile";
            String PCAP_FILE = System.getProperty(PCAP_FILE_KEY, "smallCapture2.pcapng");
            try 
            {
               pcapHandle = Pcaps.openOffline(PCAP_FILE, TimestampPrecision.NANO);
            } 
            catch (PcapNativeException e) 
            {
               pcapHandle = Pcaps.openOffline(PCAP_FILE);
            }

            for (int i = 0; i < 10000000; i++) 
            {
               try 
               {
                  Packet packet = pcapHandle.getNextPacketEx();
                  nvpl.gotPacket( packet );
               } 
               catch (TimeoutException e) 
               {
                  // ...
               } 
               catch (EOFException e) 
               {
                  logger.debug("EOF");
                  break;
               } 
               catch (NotOpenException e)
               {
                  logger.error("cought {}.", e);
               }
            }
         } catch (PcapNativeException e) {
            logger.error("cought {}.", e);
         }
         
      }
   }
   
   
   private class Watchdog implements Runnable
   {
      private boolean watching;
      
      public void run()
      {
         logger.debug("Watchdog.run() called");
         watching = true;
         
         while ( watching )
         {
            try
            {
               Thread.sleep(10000);
               
               long now = System.currentTimeMillis();
               if ( ( nvpl.timeOfLastPackage + 5000 ) < now )
               {
                  logger.trace("Watchdog.run() last package[{}] received {} ms ago..", nvpl.counter, (now - nvpl.timeOfLastPackage));
                     
                  StackTraceElement[] stackTrace = mListeningStartThread.getStackTrace();
                  logger.trace("getStackTrace()");
                  for (int i = 1; i < stackTrace.length; i++)
                      logger.trace("\tat {}",stackTrace[i]);
               
                  mListeningStartThread.interrupt();
               }
            }
            catch( InterruptedException ie )
            {
               logger.error("cought {}.", ie);
            }
         }
      }
      
   }
   
   
   
   private class MyListeningStarter implements Runnable
   {
   
      public void run()
      {
         logger.debug("MyListeningStarter.run() called");
         
         boolean keepReentring = true;
         while( keepReentring )
         {
            logger.debug("startListening to {} packages.", COUNT);

            List<PcapNetworkInterface> allDevs = null;
            try 
            {
               allDevs = Pcaps.findAllDevs();
            } 
            catch (PcapNativeException e) 
            {
               e.printStackTrace();
            }

            int nifIdx = 2;
            if( allDevs != null )
            {
               nif = allDevs.get(nifIdx);
               if ( nif != null )
               {
                  logger.debug("MyListeningStarter.run(): nifIdx:{}, got nif {}", nifIdx, nif.getName());

                  try
                  {
                     pcapHandle = nif.openLive(SNAPLEN, PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);
                     pcapHandle.setBlockingMode(BlockingMode.NONBLOCKING);
                  } 
                  catch (PcapNativeException e1)
                  {
                     e1.printStackTrace();
                  }
                  catch (NotOpenException noe )
                  {
                     noe.printStackTrace();
                  }

                  if( pcapHandle.isOpen() )
                  {
                     logger.debug("MyListeningStarter.run(): nifIdx:{}, start pcapHandle.loop()", nifIdx);

                     try
                     {
                        pcapHandle.loop(COUNT, nvpl);
                     } 
                     catch (PcapNativeException e)
                     {
                        e.printStackTrace();
                        keepReentring = false;
                     } 
                     catch (NotOpenException e)
                     {
                        e.printStackTrace();
                        keepReentring = false;
                     } 
                     catch (InterruptedException e) 
                     {
                        e.printStackTrace();
                        keepReentring = true;
                     } 
                  }
                  else
                  {
                     logger.debug("MyListeningStarter.run(): unable to open nifIdx:{}", nifIdx);
                  }

                  pcapHandle.close();
               }
            }
         }
         
         logger.debug("startListening() done.");
      }
   }
   
   
   public static void main(String[] args)
   {
      handle = new NetVisMain( args );
   }
}
