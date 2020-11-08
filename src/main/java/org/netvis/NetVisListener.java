package org.netvis;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.PcapHandle.BlockingMode;

public class NetVisListener implements Runnable {

  private static final  Logger logger = LoggerFactory.getLogger(NetVisListener.class);
  private NetVisMain mMain;
  public NetVisMain getMain() { return mMain; };
  
  Thread mListeningStartThread; 
  
  private PcapNetworkInterface nif;
  public PcapNetworkInterface getNif() {
     return nif;
  }
  
  private PcapHandle pcapHandle; 
  public PcapHandle getPcapHandle() {
     return pcapHandle;
  }

  NetVisPacketListener mNVPL;
  
  private static final String COUNT_KEY = NetVisListener.class.getName() + ".count";
  private static final int COUNT = Integer.getInteger(COUNT_KEY, 20);
  
  private static final String SNAPLEN_KEY = NetVisListener.class.getName() + ".snaplen";
  private static final int SNAPLEN = Integer.getInteger(SNAPLEN_KEY, 65536); // [bytes]

  private static final String READ_TIMEOUT_KEY = NetVisListener.class.getName() + ".readTimeout";
  private static final int READ_TIMEOUT = Integer.getInteger(READ_TIMEOUT_KEY, 100); // [ms]

  private int nifIdx = 2;
  public int getCurrentNifIdx() {return nifIdx; };
  boolean keepReentring = true;
  
  public NetVisListener(NetVisMain main) {
    mMain = main;
    mNVPL = new NetVisPacketListener( this );
  }
  
  public void start() {
    logger.debug("NetVisListener.start() called, nifIdx= {} ", nifIdx);
    
    stop();
    keepReentring = true;
    mListeningStartThread = new Thread (this );
    mListeningStartThread.start();
  }
  
  
  public void stop() {
    logger.debug("NetVisListener.stop() called, nifIdx= {} ", nifIdx);
    keepReentring = false;
    
    if( mListeningStartThread !=  null ) {
      mListeningStartThread.interrupt();
      try {
        mListeningStartThread.join(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
  
  public void clear() {
    logger.debug("NetVisListener.clear() called");
    mMain.getNetVisFrame().getNetVisGraphComponent().clear();
    mMain.getModel().clear();
  }
  
  
  public void selectNif( int nif ) {
    logger.debug("NetVisListener.selectNif( {} ) called", nif);
    
    nifIdx = nif;
    stop();
    
    keepReentring = true;
    mListeningStartThread = new Thread (this );
    mListeningStartThread.start();
  }
  
  
  @Override
  public void run() {
    logger.debug("NetVisListener.run() called");
   
    while( keepReentring ) {
      logger.debug("startListening to {} packages.", COUNT);

      List<PcapNetworkInterface> allDevs = null;
      try {
        allDevs = Pcaps.findAllDevs();
      } 
      catch (PcapNativeException e) {
        e.printStackTrace();
      }
      
      if( allDevs != null ) {
        nif = allDevs.get(nifIdx);
        if ( nif != null ) {
          logger.debug("NetVisListener.run(): nifIdx:{}, got nif {}", nifIdx, nif.getName());

          try {
            pcapHandle = nif.openLive(SNAPLEN, PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);
            // pcapHandle.setBlockingMode(BlockingMode.NONBLOCKING);
            pcapHandle.setBlockingMode(BlockingMode.BLOCKING);
          } 
          catch (PcapNativeException e1) {
            e1.printStackTrace();
          }
          catch (NotOpenException noe ) {
            noe.printStackTrace();
          }

          if( pcapHandle.isOpen() ) {
            logger.debug("NetVisListener.run(): nifIdx:{}, start pcapHandle.loop()", nifIdx);

            try {
              pcapHandle.loop(COUNT, mNVPL);
            } 
            catch (PcapNativeException e) {
              e.printStackTrace();
              keepReentring = false;
            } 
            catch (NotOpenException e) {
              e.printStackTrace();
              keepReentring = false;
            } 
            catch (InterruptedException e) {
              e.printStackTrace();
              keepReentring = true;
            } 

            logger.debug("MyListeningStarter.run(): nifIdx:{}, pcapHandle.loop() exited", nifIdx);
          } else {
            logger.debug("MyListeningStarter.run(): unable to open nifIdx:{}", nifIdx);
          }

          pcapHandle.close();
          logger.debug("MyListeningStarter.run(): pcapHandle.close(), nifIdx:{}, got nif {}", nifIdx, nif.getName());
        }
      }
    }
    logger.debug("startListening() done.");
  }
}
