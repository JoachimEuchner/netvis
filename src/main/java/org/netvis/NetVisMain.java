package org.netvis;

import org.netvis.model.Model;
import org.netvis.ui.NetVisFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetVisMain {

  private static final Logger logger = LoggerFactory.getLogger(NetVisMain.class);

  private static NetVisMain handle;

  private NetVisListener mNVL;
  Thread mListeningStartThread; 

  private Model mModel;
  public Model getModel() { return mModel; }

  private boolean mIsOnline;
  public boolean isOnline() { return mIsOnline; }

  private NetVisFrame mNVF;
  public NetVisFrame getNetVisFrame() { return mNVF; }
  
  public static NetVisMain getMain() { return handle; };
  
  private NetVisMain ( String[] args ) {
    if( args.length == 0 ) {
      mIsOnline = true;
      mNVL = new NetVisListener( this );

      mListeningStartThread = new Thread (mNVL );
      mListeningStartThread.start();
    }
    else {
      // args[] will point to a pcap-file
    }

    mModel = new Model( this );
    
    mNVF = new NetVisFrame( this );
    
  }


  public static void main(String[] args) {
    logger.info("info:NetVisMain.<ctor>() called.");
    handle = new NetVisMain( args );
  }

}
