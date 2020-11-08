package org.netvis.ui;

import org.netvis.NetVisMain;
import org.netvis.model.Node;

public class NetVisGraphNode {
  
  private final Node mNode;
  Node getNode() { return mNode; }
  
  private boolean mIsInitiallyLayouted;
  public boolean isInitiallyLayouted() { return mIsInitiallyLayouted; }
  public void setIsInitiallyLayouted( boolean l ) { mIsInitiallyLayouted = l; } 
  
  private boolean mIsManuallyMoved;
  public boolean isManuallyMoved() { return mIsManuallyMoved; }
  public void setIsManuallyMoved( boolean l ) { mIsManuallyMoved = l; } 
  
  private int mx;
  public int getMx() { return mx; }
  public void setMx( int x) { mx = x; }
  
  private int my;
  public int getMy() { return my; }
  public void setMy( int y) { my = y; }
  
  private int mWidth;
  public int getWidth() { return mWidth; }
  public void setWidth( int width ) { mWidth = width; }
  
  private int mHeight;
  public int getHeight() { return mHeight; }
  public void setHeight( int height ) { mHeight = height; }
  
  private String mDisplayString;
  public String getDisplayString() { return mDisplayString; };
  public void setDisplayString( String s ) { mDisplayString = s; mStringWidth = -1; };
  private int mStringWidth;
  public int getStringWidth() { return mStringWidth; }
  public void setStringWidth( int w ) { mStringWidth = w; }
  
  int mLevelOfDetail = 0;
  public int getLod() { return mLevelOfDetail; }
  public void increaseLod() { mLevelOfDetail++; if( mLevelOfDetail > 2 ) { mLevelOfDetail = 0;} }
  public void resetLod() { mLevelOfDetail = 0; }
  
  public NetVisGraphNode( Node  n )  {
    mNode = n;
    mIsInitiallyLayouted = false;
    mIsManuallyMoved = false;
    
    mDisplayString = n.getAddr().toString();
    
    NetVisGraphComponent nvgc = NetVisMain.getMain().getNetVisFrame().getNetVisGraphComponent();
    nvgc.addGraphNode( this );
    
    mStringWidth = -1;
    mLevelOfDetail = 0;
  }
  
  
  public boolean contains ( int x, int y ) {
     boolean retval = false;
     if( ( x > mx ) && ( x < ( mx+mWidth ) ) ) {
        if( ( y > my ) && ( y < ( my+mHeight ) ) ) {
           retval = true;
        }
     }
     return ( retval );
  }
  
}
