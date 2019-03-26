package netvis.model;

public class Packet
{
   private final long mTs;
   public long getTs() { return mTs; }
   private final Node mSrc;
   public Node getSrc() { return mSrc; }
   private final Node mDst;
   public Node getDst() { return mDst; }
   private final int mSize ;
   public int getSize() { return mSize; }
   
   public Packet( long ts, Node src, Node dst, int size )
   {
      mTs = ts;
      mSrc = src;
      mDst = dst;
      mSize = size;
   }  
}

