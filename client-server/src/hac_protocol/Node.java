/**
 * @author Joshua Maxwell
 * @author Cameron Krueger
 * @version March 7, 2021
 * This class holds the information about a given node.
 * This is essentially a struct with extra steps
 */

package hac_protocol;

import java.net.InetAddress;

class Node {
  private final InetAddress ip;
  private final int port;
  
  /**
   * Constructor
   * @param ip the IP address for the client
   * @param port the port number for the client
   */
  public Node(InetAddress ip, int port) {
    this.ip = ip;
    this.port = port;
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
   * Checks if this is equal to that
   * @param other The other client
   * @return whether or not they are equal
   */
  public boolean equals(Node other) {
    return this.ip == other.ip && this.port == other.port;
  }
}
