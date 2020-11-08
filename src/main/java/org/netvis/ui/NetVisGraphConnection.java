package org.netvis.ui;

import org.netvis.NetVisMain;
import org.netvis.model.Connection;

public class NetVisGraphConnection {

  private final Connection conn;
  public Connection getConnection() { return conn; };
  
  private final NetVisGraphNode srcGraphNode;
  public NetVisGraphNode getSrcGraphNode() { return srcGraphNode; };
  private final NetVisGraphNode dstGraphNode;
  public NetVisGraphNode getDstGRaphNode() { return dstGraphNode; };
  
  public NetVisGraphConnection( Connection c ) {
    conn = c;
    
    NetVisGraphComponent nvgc = NetVisMain.getMain().getNetVisFrame().getNetVisGraphComponent();
    srcGraphNode = c.getSrc().getGraphNode();
    dstGraphNode = c.getDst().getGraphNode();
    
    nvgc.addGraphConnection( this );
  }
  
  
}
