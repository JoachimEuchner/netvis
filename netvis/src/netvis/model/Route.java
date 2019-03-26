package netvis.model;

import java.net.Inet4Address;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netvis.traceroute.TraceRouteNode;

public class Route
{
   private static final Logger logger = LoggerFactory.getLogger(Route.class);

   Model mMain;
   Inet4Address mSrcAddr;
   Inet4Address mDstAddr;
   
   private final Vector<TraceRouteNode> mTraceRouteNodes;
   private final Vector<Link> mLinks;
   public class ReverseIterator<T> implements Iterator<T>, Iterable<T> 
   {
      private final List<T> list;
      private int position;

      public ReverseIterator(List<T> list) {
          this.list = list;
          this.position = list.size() - 1;
      }

      @Override
      public Iterator<T> iterator() {
          return this;
      }

      @Override
      public boolean hasNext() {
          return position >= 0;
      }

      @Override
      public T next() {
          return list.get(position--);
      }

      @Override
      public void remove() {
          throw new UnsupportedOperationException();
      }
   }
   
   
   public Route( Model m, Inet4Address src, Inet4Address dst )
   {
      logger.debug("##new Route: from {0} to {0}", src, dst);
      mMain = m;
      mSrcAddr = src;
      mDstAddr = dst;
      this.mTraceRouteNodes = new Vector<TraceRouteNode>(30,30);
      TraceRouteNode trn = new TraceRouteNode(src, dst, src, 0);
      this.mTraceRouteNodes.add(trn);
      
      this.mLinks = new Vector<Link>(30,30);      
   }
   
   public void removeLink(  Inet4Address src , Inet4Address dst )
   {
    
      synchronized( mLinks )
      {
         for( Link l: new ReverseIterator<Link>(mLinks) )
         {
            if( ( l!= null) && ( l.getSrc() != null ) && ( l.getDst() != null ))
            {
               if( ( Model.equalsAddr( l.getSrc().getAddr(), src) )
                        && Model.equalsAddr( l.getDst().getAddr(), dst ) ) 
               {
                  mLinks.remove( l );
               }
            }
         }
      }
   }
   
   
   public void dumpRoute()
   {
      synchronized( mTraceRouteNodes )
      {
         for ( TraceRouteNode trn : mTraceRouteNodes )
         {
            logger.debug(".... Route <"+mSrcAddr+", "+mDstAddr+">, have: "
                     + trn.getReplyingAddr() +" @depth="+trn.getDepth() );
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
            if( Model.equalsAddr ( _trn.getReplyingAddr(), newTrn.getReplyingAddr() ) 
                     && Model.equalsAddr ( _trn.getSrc(), newTrn.getSrc() ) 
                     && Model.equalsAddr ( _trn.getDst(), newTrn.getDst() ) 
                     && (_trn.getDepth() == newTrn.getDepth()) )
            {
               trn = _trn;
               break;
            }
         }
      }     
      // dumpRoute();
      
//      if ( trn != null)
//         logger.debug( "----- addTraceRouteNode(" + newTrn.mReplyingAddr + ") found " + trn.mReplyingAddr);
//      else
//         logger.debug( "----- addTraceRouteNode(" + newTrn.mReplyingAddr + ") found null");

      if( trn == null )
      {  
         // not found in existing trns
         trn = newTrn;
         this.mTraceRouteNodes.add( trn );
         // dumpRoute();
      }

      // find previous node along route:
      int depth = trn.getDepth();
      TraceRouteNode previousNode = null;
      if( depth > 0 )
      {
         int searchDepth = depth - 1;

         while ( ( searchDepth >= 0 ) && ( previousNode == null ))
         {
            // logger.debug("Route <"+mSrcAddr+", "+mDstAddr+">: searching @depth="
            //          +searchDepth+" in "+mTraceRouteNodes.size()+" trns");
            synchronized( mTraceRouteNodes )
            {
               for ( TraceRouteNode _trn : mTraceRouteNodes )
               {
                  if ( _trn.getDepth() == searchDepth )
                  {
                     previousNode = _trn;
                     break;
                  }
               }
            }
            if( previousNode != null )
            {
               //  logger.debug("Route <"+mSrcAddr+", "+mDstAddr+">: found "+ previousNode.getAddr() + " while searching @depth="
               //          + searchDepth + " in "+mTraceRouteNodes.size()+" trns");
            }

            searchDepth--;
         }
      }

      if( previousNode != null )
      {
         // logger.debug("Route <"+mSrcAddr+", "+mDstAddr+">: found previous node="+
         //          previousNode.mReplyingAddr + "@depth=" + previousNode.mDepth);

         if ( mMain.findNode(previousNode.getReplyingAddr()) == null )
         {
            mMain.addNode( previousNode );
         }
         
         if ( mMain.findNode(trn.getReplyingAddr()) == null )
         {
            mMain.addNode( trn );
         }
         
         Link l = mMain.findLink(previousNode.getReplyingAddr(), trn.getReplyingAddr());
         if( l == null )
         {
            l = new Link( mMain.findNode(previousNode.getReplyingAddr()), mMain.findNode(trn.getReplyingAddr())); 
            mMain.addLink( l );
            mLinks.add(l);
         }
         l.setTimeSeenLastPacket( System.currentTimeMillis() );
    
         
         mMain.removeLink( previousNode.getAddr(), trn.getDst() );
         removeLink(previousNode.getAddr(), trn.getDst() );
         Node finalNode =  mMain.findNodeAndAdd( trn.getDst() );
         Link finalLink = new Link( mMain.findNode(trn.getReplyingAddr()), finalNode);
         finalLink.setTimeSeenLastPacket( System.currentTimeMillis() );
         mMain.addLink( finalLink );
         mLinks.add(finalLink);
         
         // logger.debug("Route <"+mSrcAddr+", "+mDstAddr+">: link("
         //          + previousNode.mReplyingAddr+" to "+ trn.mReplyingAddr  +"@depth="+trn.mDepth+")");
      }
      else
      {
         logger.debug("Route <"+mSrcAddr+", "+mDstAddr+">: found no previous node.");
      }

      // dumpRoute();

      logger.debug("Route <"+mSrcAddr+", "+mDstAddr+">.added( trn: "+ 
               trn.getReplyingAddr()+" at depth "+ trn.getDepth()  +") got "+mTraceRouteNodes.size()+" nodes.");

   }
   
   
   public void add( Packet p )
   {
      synchronized( mLinks )
      {
         long now = System.currentTimeMillis();
         for ( Link l : mLinks )
         {
            l.incPacketNr();
            l.setTimeSeenLastPacket(now);
         }
      }
   }
   
   
}
