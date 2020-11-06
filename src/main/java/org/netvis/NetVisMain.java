package org.netvis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetVisMain {

  private static final Logger logger = LoggerFactory.getLogger(NetVisMain.class);
  
  private static NetVisMain handle;
  
  private NetVisListener mNVL;
  Thread mListeningStartThread; 
  
  private boolean mIsOnline;
  public boolean isOnline() {return mIsOnline;}
  
  
  private NetVisMain ( String[] args )
  {
    if( args.length == 0 )
    {
       mIsOnline = true;
       mNVL = new NetVisListener();
       
       mListeningStartThread = new Thread (mNVL );
       mListeningStartThread.start();
    }
  }
  
  
  public static void main(String[] args) {
    logger.info("info:NetVisMain.<ctor>() called.");
    
    handle = new NetVisMain( args );
  }

}
