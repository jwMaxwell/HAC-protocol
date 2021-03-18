/**
 * @author Joshua Maxwell
 * @version March 7, 2021
 * This class holds the information about a given node.
 * This is essentially a struct with extra steps
 */

package hac_server;

import java.net.InetAddress;

class Node {
  private final InetAddress ip;
  private final int port;
  private long lastPingTime;
  
  /**
   * Constructor
   * @param ip the IP address for the client
   * @param port the port number for the client
   */
  public Node(InetAddress ip, int port) {
    this.ip = ip;
    this.port = port;
    this.lastPingTime = System.currentTimeMillis();
  }
  
  /**
   * Gets this IP
   * @return this IP
   */
  public InetAddress getIp() {
    return this.ip;
  }
  
  /**
   * Gets this PORT
   * @return this PORT
   */
  public int getPort() {
    return this.port;
  }
  
  /**
   * Gets the last time that the node pinged the server
   * @return the last time that the node pinged the server
   */
  public long getLPTime() {
    return this.lastPingTime;
  }
  
  /**
   * Sets the last time a node pinged the server
   * @param time the last time the node pinged the server
   */
  public void setLPTime(long time) {
    this.lastPingTime = time;
  }
  
  /**
   * Checks if this is equal to that
   * @param other The other client
   * @return whether or not they are equal
   */
  public boolean equals(Node other) {
    return this.ip == other.ip && this.port == other.port;
  }
}
