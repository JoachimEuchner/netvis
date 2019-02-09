package netvis.model;

import java.net.Inet4Address;

import org.pcap4j.packet.IpV4Packet;

public class Node
{
   private Inet4Address addr;
   public byte[] addressBytes;
   
   public int receivedPackets;
   public int sentPackets;
   
   public IpV4Packet lastSeenIpv4p;
   
   public String mDisplayName;
   public int mDisplayNameXoffset;
   
   public int mx;
   public int my;
   public int mWidth;
   public int mHeight;
   public boolean mbIsInitialLayouted;
   public boolean mbCanFlow;
   
   public double x;
   public double y;
   public double fx;
   public double fy;
   public double vx;
   public double vy;
   
   public boolean isLocal;
  
   public boolean isActive;  
   public int mLoD;
   
   public long timeOfLastSeenPacket;
   
   public Node( Inet4Address _addr )
   {
      mDisplayName = "";
      mDisplayNameXoffset = 0;
      isActive = true;
      mLoD = 1;
      
      receivedPackets = 0;
      sentPackets = 0;
      setInet4Address(_addr);
      
      addressBytes = addr.getAddress();      
      
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
   }
   
   public Inet4Address getAddr()
   {
      return ( addr );
   }
   
   
   
   
   
   public void setInet4Address( Inet4Address _addr )
   {
      addr = _addr;
      
      String name4Numbers = addr.toString();
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
                  // System.out.println(attrId + ": " + value);

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
            System.out.print(""); // NO-OP
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

      return retVal;
   }
}
