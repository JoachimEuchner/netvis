package netvis.model;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netvis.traceroute.TraceRouteNode;

public class Route
{
   private static final Logger logger = LoggerFactory.getLogger(Route.class);

   Model mMain;
   Inet4Address mSrcAddr;
   Inet4Address mDstAddr;
   
   private final ArrayList<TraceRouteNode> mTraceRouteNodes;
   private final ArrayList<Link> mLinks;
   
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
      logger.debug("##new Route: from {} to {}", src, dst);
      mMain = m;
      mSrcAddr = src;
      mDstAddr = dst;
      this.mTraceRouteNodes = new ArrayList<>(30);
      TraceRouteNode trn = new TraceRouteNode(src, dst, src, 0);
      this.mTraceRouteNodes.add(trn);
      
      this.mLinks = new ArrayList<>(30);      
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
            logger.trace(".... Route <{}->{}>, have: {} @depth={}", mSrcAddr, mDstAddr, trn.getReplyingAddr(), trn.getDepth() );
         }
      }      
   }
   
   private TraceRouteNode getNodeAtDepth(int depth, boolean forward)
   {
      TraceRouteNode trn = null;
      synchronized( mTraceRouteNodes )
      {
         if( forward )
         {
            for ( TraceRouteNode _trn : mTraceRouteNodes )
            {
               if ( _trn.getDepth() == depth )
               {
                  trn = _trn;
                  break;
               }
            }
         }
         else
         {
            for(  TraceRouteNode _trn : new ReverseIterator<TraceRouteNode>(mTraceRouteNodes) )
            {
               if ( _trn.getDepth() == depth )
               {
                  trn = _trn;
                  break;
               }
            }
         }
      }
      return trn;
   }
   
   private TraceRouteNode getPreviousNode(int depth )
   {
      TraceRouteNode trn = null;
      
      if( depth > 0 )
      {
         int searchDepth = depth - 1;
         while ( searchDepth >= 0 )
         {
            logger.trace("Route <{}, {}>: searching @depth={} in {} trns.",
                  mSrcAddr, mDstAddr, searchDepth, mTraceRouteNodes.size());
          
            trn = getNodeAtDepth(searchDepth, false);
            if( trn != null )
            {
               logger.trace("Route <{}, {}>: found {} @depth={} in {} trns.",
                        mSrcAddr, mDstAddr, trn.getReplyingAddr(), searchDepth, mTraceRouteNodes.size());
               break;
            }
            searchDepth--;
         }
      }
      
      return trn;
   }
   
  
   public void addTraceRouteNode( TraceRouteNode newTrn )
   {  
      TraceRouteNode trn = null;

      synchronized( mTraceRouteNodes )
      {
         for ( TraceRouteNode _trn : mTraceRouteNodes )
         {
            if ( Model.equalsAddr ( _trn.getSrc(), newTrn.getSrc() ) 
                 && Model.equalsAddr ( _trn.getDst(), newTrn.getDst() ) )
            {
               if( Model.equalsAddr ( _trn.getReplyingAddr(), newTrn.getReplyingAddr() ) 
                        && (_trn.getDepth() == newTrn.getDepth()) )
               {
                  trn = _trn;
                  break;
               }
            }
            else
            {
               logger.error("addTraceRouteNode({}, {}->{}) should not have been added here: ({}->{})",
                        newTrn.getReplyingAddr(), newTrn.getSrc(), newTrn.getDst(),
                        _trn.getSrc(), _trn.getDst());
            }
         }
      }     
 
      if( trn == null )
      {  
         // not found in existing trns
         trn = newTrn;
         this.mTraceRouteNodes.add( trn );
      }

      // find previous node along route:
      int depth = trn.getDepth();
      TraceRouteNode previousNode =  getPreviousNode( depth );

      if( previousNode != null )
      {
         logger.debug("Route <{}, {}>: found {} @depth={} in {} trns.",
                  mSrcAddr, mDstAddr, previousNode.getReplyingAddr(), previousNode.getDepth(), mTraceRouteNodes.size()); 

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
         
         logger.trace("Route <{}, {}>: link({} to {}, @depth={} ", 
                  mSrcAddr, mDstAddr, previousNode.getReplyingAddr(), trn.getReplyingAddr(), trn.getDepth());
      }
      else
      {
         logger.trace("Route <{}, {}>: found no previous node.", 
                  mSrcAddr, mDstAddr);
      }

      logger.trace("Route <{}, {}>.added( trn: {} at depth {}, got {} nodes.", 
               mSrcAddr, mDstAddr, trn.getReplyingAddr(), trn.getDepth(), mTraceRouteNodes.size() );

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
