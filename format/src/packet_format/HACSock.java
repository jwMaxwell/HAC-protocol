/**
 * @author Joshu Maxwell
 * @version March 20, 2021
 * This class creates forces a packet structure that is compatible with the HAC protocol
 */
package packet_format;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class HACPack {
  private String header = null; //the command
  private String body = null; //the not command
  private int port;
  private InetAddress IP;
  
  /**
   * This constructor will accept two strings and turn them into a usable packet
   * @param header the command which the receiver should act upon
   * @param body the body of the command or message
   */
  public HACPack(String header, String body, InetAddress IP, int port) {
    this.header = header != null ? header : "NONE";
    this.body = body;
    this.IP = IP;
    this.port = port;
  }
  
  /**
   * This constructor will accept a single string and splits it into a usable packet
   * If you only want to use the body, use HACPack(null, body)
   * @param headerBody the string holding all of the packet data
   */
  public HACPack(String headerBody, InetAddress IP, int port){
    this.header = headerBody.split("//s+")[0];
    this.body = headerBody.substring(1);
    this.IP = IP;
    this.port = port;
  }
  
  /**
   * Accepts a datagram packet and turns it into a usable packet
   * @param packet datagram packet to format
   */
  public HACPack(DatagramPacket packet) {
    String temp = new String(packet.getData());
    this.header = temp.split("//s+")[0];
    this.body = temp.substring(1);
    this.IP = packet.getAddress();
    this.port = packet.getPort();
  }
  
  public HACPack(HACPack hp) {
    this.header = hp.header;
    this.body = hp.body;
    this.IP = hp.IP;
    this.port = hp.port;
  }

  /**
   * Gets the header text of the packet
   * @return header text
   */
  public String getHeader() {
    return this.header;
  }
  
  /**
   * Get the body text of the packet
   * @return body text
   */
  public String getBody() {
    return this.body;
  }
  
  public InetAddress getAddress() {
    return this.IP;
  }
  
  public int getPort() {
    return this.port;
  }
  
  /**
   * Converts HACPack to a DatagramPacket that can be sent by a socket
   * @param IP address of recipient
   * @param port port number of recipient
   * @return DatagramPacket representation of this HACPacket
   */
  public DatagramPacket build(InetAddress IP, int port) {
    return new DatagramPacket(this.toString().getBytes(),
                              this.toString().getBytes().length,
                              IP,
                              port);
  }
  
  public HACPack copy() {
    return new HACPack(this);
  }
  
  /**
   * Checks if the values of this are equal to the values of another HACPack
   * @param hp the HACPack to compare with
   * @return take a guess
   */
  public boolean equals(HACPack hp) { // This should work perfectly
    return this.header.equals(new HACPack(hp).header) 
           && this.body.equals(new HACPack(hp).body) 
           ? true 
           : false;
  }
  
  /**
   * bippity boppity it's now a string, would you look at that
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return this.header + " " + this.body;
  } 
}
