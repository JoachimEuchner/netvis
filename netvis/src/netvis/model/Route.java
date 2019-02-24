package netvis.model;

import java.net.Inet4Address;
import java.util.Vector;

import netvis.traceroute.TraceRouteNode;

public class Route
{
   Model mMain;
   Inet4Address mSrcAddr;
   Inet4Address mDstAddr;
   
   private final Vector<netvis.traceroute.TraceRouteNode> mTraceRouteNodes;
   
   public Route( Model m, Inet4Address src, Inet4Address dst )
   {
      System.out.println("##new Route: from "+src+" to " +dst);
      mMain = m;
      mSrcAddr = src;
      mDstAddr = dst;
      this.mTraceRouteNodes = new Vector<netvis.traceroute.TraceRouteNode>(30,30);
      TraceRouteNode trn = new TraceRouteNode(src, dst, src, 0);
      this.mTraceRouteNodes.add(trn);
   }
   
   
   public void dumpRoute()
   {
      synchronized( mTraceRouteNodes )
      {
         for ( TraceRouteNode trn : mTraceRouteNodes )
         {
            System.out.println(".... Route <"+mSrcAddr+", "+mDstAddr+">, have: "
                     + trn.mReplyingAddr +" @depth="+trn.mDepth );
         }
      }      
      
   }
  
   public void addTraceRouteNode( TraceRouteNode newTrn )
   {  
      TraceRouteNode trn = null;

      synchronized( mTraceRouteNodes )
      {
         for ( TraceRouteNode _trn : mTraceRouteNodes )
         {
            if( Model.equalsAddr ( _trn.mReplyingAddr, newTrn.mReplyingAddr ) 
                     && Model.equalsAddr ( _trn.mSrcAddr, newTrn.mSrcAddr ) 
                     && Model.equalsAddr ( _trn.mOrigDstAddr, newTrn.mOrigDstAddr ) 
                     && (_trn.mDepth == newTrn.mDepth) )
            {
               trn = _trn;
               break;
            }
         }
      }     
      // dumpRoute();
      
//      if ( trn != null)
//         System.out.println( "----- addTraceRouteNode(" + newTrn.mReplyingAddr + ") found " + trn.mReplyingAddr);
//      else
//         System.out.println( "----- addTraceRouteNode(" + newTrn.mReplyingAddr + ") found null");

      if( trn == null )
      {  
         // not found in existing trns
         trn = newTrn;
         this.mTraceRouteNodes.add( trn );
         // dumpRoute();
      }

      // find previous node along route:
      int depth = trn.mDepth;
      TraceRouteNode previousNode = null;
      if( depth > 0 )
      {
         int searchDepth = depth - 1;

         while ( ( searchDepth >= 0 ) && ( previousNode == null ))
         {
            System.out.println("Route <"+mSrcAddr+", "+mDstAddr+">: searching @depth="
                     +searchDepth+" in "+mTraceRouteNodes.size()+" trns");
            synchronized( mTraceRouteNodes )
            {
               for ( TraceRouteNode _trn : mTraceRouteNodes )
               {
                  if ( _trn.mDepth == searchDepth )
                  {
                     previousNode = _trn;
                     break;
                  }
               }
            }
            if( previousNode != null )
            {
               System.out.println("Route <"+mSrcAddr+", "+mDstAddr+">: found "+ previousNode.getAddr() + " while searching @depth="
                        + searchDepth + " in "+mTraceRouteNodes.size()+" trns");
            }

            searchDepth--;
         }
      }

      if( previousNode != null )
      {
         System.out.println("Route <"+mSrcAddr+", "+mDstAddr+">: found previous node="+
                  previousNode.mReplyingAddr + "@depth=" + previousNode.mDepth);

         if ( mMain.findNode(previousNode.mReplyingAddr) == null )
         {
            mMain.addNode( previousNode );
         }
         if ( mMain.findNode(trn.mReplyingAddr) == null )
         {
            mMain.addNode( trn );
         }
         
         Link l = mMain.findLink(previousNode, trn);
         if( l == null )
         {
            l = new Link( mMain.findNode(previousNode.mReplyingAddr), mMain.findNode(trn.mReplyingAddr)); 
            mMain.addLink( l );
         }
         l.setTimeSeenLastPacket( System.currentTimeMillis() );
         
         mMain.removeLink( previousNode.getAddr(), trn.mOrigDstAddr );
         Node finalNode =  mMain.findNodeAndAdd( trn.mOrigDstAddr );
         Link finalLink = new Link(mMain.findNode(trn.mReplyingAddr), finalNode);
         finalLink.setTimeSeenLastPacket( System.currentTimeMillis() );
         mMain.addLink( finalLink );
         
         System.out.println("Route <"+mSrcAddr+", "+mDstAddr+">: link("
                  + previousNode.mReplyingAddr+" to "+ trn.mReplyingAddr  +"@depth="+trn.mDepth+")");
      }
      else
      {
         System.out.println("Route <"+mSrcAddr+", "+mDstAddr+">: found no previous node.");
      }

      dumpRoute();

      System.out.println("Route <"+mSrcAddr+", "+mDstAddr+">.added( trn: "+ 
               trn.mReplyingAddr+" at depth "+ trn.mDepth  +") got "+mTraceRouteNodes.size()+" nodes.");

   }
   
}
