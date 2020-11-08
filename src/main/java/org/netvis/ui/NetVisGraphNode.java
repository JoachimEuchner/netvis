package org.netvis.ui;

import org.netvis.NetVisMain;
import org.netvis.model.Node;

public class NetVisGraphNode {
  
  private final Node mNode;
  Node getNode() { return mNode; }
  
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
  
  
  public NetVisGraphNode( Node  n )  {
    mNode = n;
    
    NetVisGraphComponent nvgc = NetVisMain.getMain().getNetVisFrame().getNetVisGraphComponent();
    nvgc.addGraphNode( this );
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
