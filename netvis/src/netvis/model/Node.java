package netvis.model;

import java.net.Inet4Address;

import org.pcap4j.packet.IpV4Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Node
{
   private static final Logger logger = LoggerFactory.getLogger(Node.class);
   
   private Inet4Address mAddr;
   private final byte[] addressBytes;
   public byte[] getAddressBytes() { return addressBytes; }
   
   private int receivedPackets;
   public void incReceivedPackets() { receivedPackets++; }
   public int getReceivedPackets() { return receivedPackets; }

   private int sentPackets;
   public void incSentPackets() { sentPackets++; }
   public int getSentPackets() { return sentPackets; }
   
   private IpV4Packet lastSeenIpv4p;
   public void setLastSeenIpv4Packet( IpV4Packet p ) { lastSeenIpv4p = p;}
   public IpV4Packet getLastSeenIpv4p() { return lastSeenIpv4p; };
   
   protected String mDisplayName;
   public String getDisplayName() {return mDisplayName;}
   // private int mDisplayNameXoffset;
   
   
   public static final int TYPE_ENDPOINT = 1;
   public static final int TYPE_ROUTEPOINT = 2; 
   protected int mType;
   public int getType() { return mType; }
   
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
   
   public boolean mbIsInitialLayouted;
   private boolean mbCanFlow;
   public boolean canFlow() { return mbCanFlow; }
   public void setCanFlow(Boolean s) { mbCanFlow = s; }
   
   private double x;
   public double getX() {return x;}
   public void setx( double tmpX ) { x = tmpX; }
   private double y;
   public void sety( double tmpY ) { y = tmpY; }
   public double getY() {return y;}

   public double fx;
   public double fy;
   public double vx;
   public double vy;
   
   public boolean isLocal;
  
   private boolean mbIsActive;
   public boolean isActive() { return mbIsActive; }
   public void setActive( boolean a ) { mbIsActive = a; }
   private int mLoD;
   public int getLoD() { return mLoD; }
   public void setLoD( int l ) { mLoD=l; }
   
   protected long timeOfLastSeenPacket;
   public long getTimeOfLastSeenPacket() { return timeOfLastSeenPacket; }
   
   public Node( Inet4Address addr )
   {
      mDisplayName = "";
      // mDisplayNameXoffset = 0;
      mbIsActive = true;
      mLoD = 1;

      mType = TYPE_ENDPOINT;
      
      receivedPackets = 0;
      sentPackets = 0;
      setInet4Address( addr );
      
      addressBytes = mAddr.getAddress();      
      
      if ( (addressBytes[0] == -64) && (addressBytes[1] == -88) && (addressBytes[2] == 2) )
      {
         isLocal = true;
      }
      else
      {
         isLocal = false;
      }
     
      mx = 0;
      my = 0;
      x= 0.0;
      y= 0.0;
      
      mWidth = 0;
      mHeight = 0;
      
      fx = 0.0;
      fy = 0.0;
      
      mbIsInitialLayouted = false;
      mbCanFlow = true;
      
      timeOfLastSeenPacket = 0;
      
      logger.debug("Node.<ctor> done: {0}'", mDisplayName);
   }
   
   public Inet4Address getAddr()
   {
      return ( mAddr );
   }
   
   public void setInet4Address( Inet4Address addr )
   {
      mAddr = addr;
      
      String name4Numbers = mAddr.toString();
      if( name4Numbers.startsWith("/"))
      {
         name4Numbers = name4Numbers.substring(1, name4Numbers.length());
      }
      
      String hostname = getHostName( name4Numbers );
      
      if( !hostname.isEmpty())
      {
         mDisplayName = hostname;
      }
      else
      {
         mDisplayName = name4Numbers;
      }
   }
   
   public boolean contains ( int x, int y )
   {
      boolean retval = false;
      if( ( x > mx ) && ( x < ( mx+mWidth ) ) )
      {
         if( ( y > my ) && ( y < ( my+mHeight ) ) )
         {
            retval = true;
         }
      }
      
      return ( retval );
   }
   
   
   
   
   /**
    * Do a reverse DNS lookup to find the host name associated with an IP address. Gets results more often than
    * {@link java.net.InetAddress#getCanonicalHostName()}, but also tries the Inet implementation if reverse DNS does
    * not work.
    * 
    * Based on code found at http://www.codingforums.com/showpost.php?p=892349&postcount=5
    * 
    * @param ip The IP address to look up
    * @return   The host name, if one could be found, or the IP address
    */
   private static String getHostName(final String ip)
   {
      String retVal = null;
      
      final String[] bytes = ip.split("\\.");
      if (bytes.length == 4)
      {
         try
         {
            final java.util.Hashtable<String, String> env = new java.util.Hashtable<String, String>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            final javax.naming.directory.DirContext ctx = new javax.naming.directory.InitialDirContext(env);
            final String reverseDnsDomain = bytes[3] + "." + bytes[2] + "." + bytes[1] + "." + bytes[0] + ".in-addr.arpa";
            final javax.naming.directory.Attributes attrs = ctx.getAttributes(reverseDnsDomain, new String[]
                     {
                              "PTR",
                     });
            for (final javax.naming.NamingEnumeration<? extends javax.naming.directory.Attribute> ae = attrs.getAll(); ae.hasMoreElements();)
            {
               final javax.naming.directory.Attribute attr = ae.next();
               final String attrId = attr.getID();
               for (final java.util.Enumeration<?> vals = attr.getAll(); vals.hasMoreElements();)
               {
                  String value = vals.nextElement().toString();

                  if ("PTR".equals(attrId))
                  {
                     final int len = value.length();
                     if (value.charAt(len - 1) == '.')
                     {
                        // Strip out trailing period
                        value = value.substring(0, len - 1);
                     }
                     retVal = value;
                  }
               }
            }
            ctx.close();
         }
         catch (final javax.naming.NamingException e)
         {
            // No reverse DNS that we could find, try with InetAddress
            logger.debug("cought: {0}",e); // NO-OP
         }
      }

      if (null == retVal)
      {
         try
         {
            retVal = java.net.InetAddress.getByName(ip).getCanonicalHostName();
         }
         catch (final java.net.UnknownHostException e1)
         {
            retVal = ip;
         }
      }

      logger.debug("getHostNameFromIp("+ip+") got: "+retVal);
      
      return retVal;
   }
}
