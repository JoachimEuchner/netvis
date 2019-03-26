package netvis;

public class NetVisMsg
{
   private NetVisMsgReceiver rec;
   public NetVisMsg(NetVisMsgReceiver localRec)
   {
      rec = localRec;
   }
   public NetVisMsgReceiver getMsgReceiver()
   {
      return rec;
   }
}
