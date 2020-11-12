package org.netvis.model;

import java.net.Inet4Address;
import org.netvis.ui.NetVisGraphNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Node {
  private static final Logger logger = LoggerFactory.getLogger(Node.class);

  private Inet4Address mAddr;
  public Inet4Address getAddr() { return ( mAddr ); }

  private final byte[] addressBytes;
  public byte[] getAddressBytes() { return addressBytes; }

  private int receivedPackets;
  public void incReceivedPackets() { receivedPackets++; }
  public int getReceivedPackets() { return receivedPackets; }

  private int sentPackets;
  public void incSentPackets() { sentPackets++; }
  public int getSentPackets() { return sentPackets; }
  
  private long timeOfLastSeenPacket = 0;
  public void setTimeOfLastSeenPacket( long now ) {
    timeOfLastSeenPacket = now;
  }

  private final NetVisGraphNode mGraphNode;
  public NetVisGraphNode getGraphNode() { return mGraphNode; }

  public Node( Inet4Address addr ) {
    setInet4Address( addr );
       
    addressBytes = mAddr.getAddress(); 
    receivedPackets = 0;
    sentPackets = 0;
    timeOfLastSeenPacket = System.currentTimeMillis();
    mGraphNode = new NetVisGraphNode(this);
    
    getHostName();
    
    logger.info("Node<ctor>:"+addr
              +", name\""+ mGraphNode.getDisplayString()+"\" "
              +", isAnyLocalAddress():"+addr.isAnyLocalAddress()
              +", isLinkLocalAddress():"+addr.isLinkLocalAddress()
              +", isMulticastAddress():"+addr.isMulticastAddress()
              +", isSiteLocalAddress():"+addr.isSiteLocalAddress()
              +", isLoopbackAddress():"+addr.isLoopbackAddress()
              +", isMCGlobal():"+addr.isMCGlobal()
              );
    
  }

  public void setInet4Address( Inet4Address addr ) {
    mAddr = addr;
  }
  
  private void getHostName() {
    String name4Numbers = mAddr.toString();
    if( name4Numbers.startsWith("/"))
    {
       name4Numbers = name4Numbers.substring(1, name4Numbers.length());
    }
    
    String hostname = getHostName( name4Numbers );
    if( !hostname.isEmpty()) {
      mGraphNode.setDisplayString( hostname );
    }
    else {
      mGraphNode.setDisplayString( name4Numbers );
    }
  }


  /**
   *  refound on:
   *  https://stackoverflow.com/a/8402645
   * 
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
