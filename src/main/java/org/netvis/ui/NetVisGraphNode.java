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
  
  private boolean mIsAutomaticallyMoved;
  public boolean isAutomatiallyMoved() { return mIsAutomaticallyMoved; }
  public void setIsAutomaticallyMoved( boolean l ) { mIsAutomaticallyMoved = l; }
  
  public boolean canFlow() { return ( !mIsManuallyMoved ); } // tbd
  
  private int mx;
  public int getMx() { return mx; }
  public void setMx( int ix) { mx = ix; x = (double)(ix);}
  
  private int my;
  public int getMy() { return my; }
  public void setMy( int iy) { my = iy; y = (double)(iy);}
  
  private double x;
  public double getX() { return x; }
  public void setX( double dx) { x = dx; mx= (int)(dx + 0.5); }
  
  private double y;
  public double getY() { return y; }
  public void setY( double dy) { y = dy; my= (int)(dy + 0.5); }
  
  
  private int mWidth;
  public int getWidth() { return mWidth; }
  public void setWidth( int width ) { mWidth = width; }
  
  private int mHeight;
  public int getHeight() { return mHeight; }
  public void setHeight( int height ) { mHeight = height; }
  
  // first attempt to some groupings:
  
  private static final int CATakamaitechnologies = 1;
  private static final int CATamazonaws = 2;
  private static final int CATgoogle1e100 = 3;
  private static final int CATcloudfront = 4;
  private static final int CATgoogleusercontent = 5;
  private static final int CATebay = 6;
  
  
  private String mDisplayString;
  private int mCategory;
  public int getCategory() { return mCategory; }
  public String getDisplayString() { return mDisplayString; };
  public void setDisplayString( String s ) { 
    mDisplayString = s; 
    mStringWidth = -1; 
    if( s.endsWith("static.akamaitechnologies.com" ) ) {
      mCategory = CATakamaitechnologies;
    } else if( s.endsWith("compute.amazonaws.com" ) ) {
      mCategory = CATamazonaws; 
    } else if( s.endsWith("compute.amazonaws.com" ) ) {
      mCategory = CATamazonaws; 
    } else if( s.endsWith("1e100.net" ) ) {
      mCategory = CATgoogle1e100; 
    } else if( s.endsWith("cloudfront.net" ) ) {
      mCategory = CATcloudfront; 
    } else if( s.endsWith("compute.amazonaws.com" ) ) {
      mCategory = CATamazonaws; 
    } else if( s.endsWith("googleusercontent.com" ) ) {
      mCategory = CATgoogleusercontent; 
    } else if( s.endsWith("ebay.com" ) ) {
      mCategory = CATebay; 
    }
  }
  private int mStringWidth;
  public int getStringWidth() { return mStringWidth; }
  public void setStringWidth( int w ) { mStringWidth = w; }
  private String mAnonDisplayString;
  public String getAnonDisplayString() { 
    if ((mDisplayString!=null) && ( mAnonDisplayString==null)) {
      mAnonDisplayString = mDisplayString.replaceAll("[a-z]", "x");
      mAnonDisplayString = mAnonDisplayString.replaceAll("[A-Z]", "X");
      mAnonDisplayString = mAnonDisplayString.replaceAll("[0-9]", "x");
    }
    return mAnonDisplayString;
  }
  
  
  
  int mLevelOfDetail = 0;
  public int getLod() { return mLevelOfDetail; }
  public void increaseLod() { mLevelOfDetail++; if( mLevelOfDetail > 2 ) { mLevelOfDetail = 0;} }
  public void resetLod() { mLevelOfDetail = 0; }
  
  public double fx;
  public double fy;
  
  public NetVisGraphNode( Node  n )  {
    mNode = n;
    mIsInitiallyLayouted = false;
    mIsManuallyMoved = false;
    mIsAutomaticallyMoved = false;
    
    mDisplayString = n.getAddr().toString();
    mCategory = 0;
    
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
