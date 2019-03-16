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
import java.util.concurrent.Future;
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
   public NetVisPackageListener nvpl;
   
   public Model mNetVisModel;
   public NetVisFrame mNetVisFrame;
   
   public Traceroute mTraceRouter;
   public TracerouteScheduler mTracerouteScheduler;
   
   
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
   
   public boolean isOnline;
   
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
         try 
         {
            while ( true ) 
            { 
               // System.out.println("MainCallable: going to call take()");
               NetVisMsg msg = (NetVisMsg) mQueue.take();
               // System.out.println("MainCallable: got "+msg);
               NetVisMsgReceiver receiver = msg.getMsgReceiver();
               try
               {
                  receiver.msgReceived(msg);
               }
               catch( Exception e)
               {
                  System.out.println("MainCallable: cought: "+e);
               }
            }
         } 
         catch (InterruptedException e) 
         {
            System.out.println("MainCallable: exiting call().");
            // Allow our thread to be interrupted
            Thread.currentThread().interrupt();
            return ( null ); // this will never run, but the compiler needs it
         }
      }
   }
   public void sendMsg( NetVisMsg msg )
   {
      BlockingQueue<NetVisMsg> theQueue = this.getQueue();
      try
      {
         // System.out.println("MainCallable: going to call theQueue.put(msg)");
         theQueue.put(msg);
         // System.out.println("MainCallable: returned from theQueue.put(msg)");
      } 
      catch (InterruptedException e)
      {
         logger.error("sendMsg() cought: " + e);
      }
   }
   
  
 
   private NetVisMain(String[] args) 
   {
      logger.info("info:NetVisMain.<ctor>() called.");
      // System.out.println("System.out.println:NetVisMain.<ctor>() called.");
      
      mNetVisModel = new Model( this );
      mNetVisFrame = new NetVisFrame( this );
      

      this.mQueue = new ArrayBlockingQueue<>(100);
      ExecutorService threadPool = Executors.newFixedThreadPool(1); 
      Future<NetVisMsg> sum = threadPool.submit(new MainCallable()); 
      
      mTraceRouter = new Traceroute( this );
      mTracerouteScheduler = new TracerouteScheduler( this, mTraceRouter );
      
      if( args.length == 0 )
      {
         isOnline = true;

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
            
//            mTracerouteScheduler.addTargetName( "www.yahoo.com" );
//            mTracerouteScheduler.addTargetName( "blog.fefe.de" );
//            mTracerouteScheduler.addTargetName( "www.google.com" );
//            mTracerouteScheduler.addTargetName( "www.spiegel.de" );
//            mTracerouteScheduler.addTargetName( "www.taz.de" );
//            mTracerouteScheduler.addTargetName( "www.9gag.com" );
//            mTracerouteScheduler.traceNextTarget();
         } 
         catch (UnknownHostException e)
         {
            e.printStackTrace();
         }
       
         
         Watchdog wd = new Watchdog();
         Thread watchdogThread = new Thread( wd );
         watchdogThread.start();
      }
      else
      {
         FileReader fr = new FileReader();
         // fr.filename = args[0];
         
         nvpl = new NetVisPackageListener( this, pcapHandle, mNetVisModel );
         
         isOnline = false;
         mFileReaderThread = new Thread( fr );
         
         mFileReaderThread.start();
      }
      
      mNetVisFrame.setSize(1000, 500);
      mNetVisFrame.setVisible( true );
   }     
     
   
   private class FileReader implements Runnable
   {

      // public String filename;
      
      public void run()
      {
         System.out.println("FileReader.run() called");
         
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
                  System.out.println("EOF");
                  break;
               } 
               catch (NotOpenException e)
               {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
               }
            }
         } catch (PcapNativeException e) {
            System.out.println("cought "+e);
         }
         
      }
   }
   
   
   private class Watchdog implements Runnable
   {
      public void run()
      {
         System.out.println("Watchdog.run() called");
         boolean watching = true;
         
         while ( watching )
         {
            try
            {
               Thread.sleep(10000);
               
               long now = System.currentTimeMillis();
               if ( ( nvpl.timeOfLastPackage + 5000 ) < now )
               {
                  System.out.println("Watchdog.run() last package["+ nvpl.counter +"] received " 
                           + (now - nvpl.timeOfLastPackage) + " ms ago..");
                     
                  StackTraceElement[] stackTrace = mListeningStartThread.getStackTrace();
                  System.out.println("getStackTrace()");
                  for (int i = 1; i < stackTrace.length; i++)
                      System.out.println("\tat " + stackTrace[i]);
               
                  mListeningStartThread.interrupt();
               }
            }
            catch( InterruptedException ie )
            {
               //...
            }
         }
      }
      
   }
   
   
   
   private class MyListeningStarter implements Runnable
   {
   
      public void run()
      {
         System.out.println("MyListeningStarter.run() called");
         
         boolean keepReentring = true;
         while( keepReentring )
         {
            System.out.println("startListening to " + COUNT +" packages.");

            List<PcapNetworkInterface> allDevs = null;
            try 
            {
               allDevs = Pcaps.findAllDevs();
            } 
            catch (PcapNativeException e) 
            {
               e.printStackTrace();
            }

            // int nifIdx = 0;
            int nifIdx = 2;
            nif = allDevs.get(nifIdx);
            System.out.println("MyListeningStarter.run(): nifIdx:"+nifIdx+", got nif "+nif.getName());

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

            System.out.println("MyListeningStarter.run(): nifIdx:"+nifIdx+", start  pcapHandle.loop()");
            
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

            pcapHandle.close();      
         }
         
         System.out.println("startListening() done.");
      }
   }
   
   
   public static void main(String[] args)
   {
      handle = new NetVisMain( args );
   }
}
