package netvis.model;

public class Link
{
   public Node src;
   public Node dst;
   
   private int seenPackets;
   
   private long timeOfLastSeenPacket = 0;
   
   public Link( Node _src, Node _dst )
   {
      src = _src;
      dst = _dst;
      seenPackets = 0;
   }
   
   public void incPacketNr()
   {
      seenPackets++;
      timeOfLastSeenPacket = System.currentTimeMillis();
   }
   
   public long getTimeSinceLastSeenPacket()
   {
      return ( System.currentTimeMillis() - timeOfLastSeenPacket);
   }
   
   public void setTimeSeenLastPacket(long t)
   {
      timeOfLastSeenPacket = t;
   }
   
   
   
   public int getNrOfSeenPackets()
   {
      return seenPackets;
   }
}
