package netvis.model;

public class Packet
{
   public long ts;
   public Node src;
   public Node dst;
   public int size;
   
   public Packet( long _ts, Node _src, Node _dst, int _size )
   {
      ts = _ts;
      src = _src;
      dst = _dst;
      size = _size;
   }  
}

