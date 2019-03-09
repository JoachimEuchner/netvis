package netvis;

public class NetVisMsg
{
   private NetVisMsgReceiver rec;
   public NetVisMsg(NetVisMsgReceiver _rec)
   {
      rec = _rec;
   }
   public NetVisMsgReceiver getMsgReceiver()
   {
      return rec;
   }
}
