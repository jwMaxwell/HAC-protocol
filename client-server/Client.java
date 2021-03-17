/**
 * @author Joshua Maxwell
 * @author Cameron Krueger
 * @version March 7, 2021
 * This program acts as a client which will be connected
 * to either a host (P2P) or a server
 * 
 * TODO
 * 2. Implement the PING message
 * 3. Implement the BACK message
 * 4. Implement the WTF command
 * 
 */

package hac_protocol;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class Client {
  DatagramSocket socket = null;
  Node parent = null;
  Node child = null;
  ArrayList<Node> nodes = new ArrayList<Node>();
  
  /**
   * constructor connects to the host
   * @throws IOException 
   */
  public Client() throws IOException 
  {
    socket = new DatagramSocket();
    InetAddress IPAddress = InetAddress.getByName("localhost");
    
    send("JOIN", IPAddress, 9876);
    
    //waiting for that GOTO msg
    DatagramPacket incomingPacket = 
        new DatagramPacket(new byte[Boot.PACKET_SIZE], Boot.PACKET_SIZE);
    socket.receive(incomingPacket);
    String response = new String(incomingPacket.getData());
    
    System.out.println("Response from server:" + response);
    
    goTo(new String(incomingPacket.getData()), incomingPacket); //feel free to make this better
    send("PING", this.parent.getIp(), this.parent.getPort());
  }
  
  /**
   * Sends and receives packets
   */
  public void createAndListenSocket() 
  {
    try 
    {
      while(true) {
        DatagramPacket incomingPacket = 
            new DatagramPacket(new byte[Boot.PACKET_SIZE], Boot.PACKET_SIZE);
        socket.receive(incomingPacket);
        String response = new String(incomingPacket.getData());
        
        System.out.println("Response from server:" + response);
        
        switch(response.split("//s+")[0]) {
          case "WTF":
          case "BACK":
          case "PING":
          case "FUCC":
            send("PING", this.parent.getIp(), this.parent.getPort());
            break;
          default:
            send("PING", this.parent.getIp(), this.parent.getPort());
            send(response, this.child.getIp(), this.child.getPort());
            break;
        }
      }
    }
    catch (IOException e) 
    {
      e.printStackTrace();
    }
  }
  
  private void goTo(String s, DatagramPacket p) throws NumberFormatException, UnknownHostException {
    String[] tokens = s.split("//s+");
    String hostType = tokens[1];
    
    //checks if host is a server
    if(hostType.equals("SERVER")) {
      this.parent = new Node(p.getAddress(), p.getPort());
    }
    //checks if host is a node in a p2p server
    else if(hostType.equals("P2P")) {
      this.parent = new Node(InetAddress.getByAddress(tokens[2].getBytes()),
          Integer.parseInt(tokens[3]));
      
      this.child = tokens.length > 4 ? 
          new Node(InetAddress.getByAddress(tokens[4].getBytes()),
              Integer.parseInt(tokens[5])) : null;
    }
  }
  
  /**
   * Sends a given message to a given destination
   * @param message the message to be sent
   * @param IPAddress the IP address to send it to
   * @param port the port number to aim at
   * @return whether or not the message was successfully sent
   */
  private boolean send(String message, InetAddress IPAddress, int port) {
    byte[] data = message.getBytes();
    DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, 9876);
    try {
      this.socket.send(sendPacket);
      System.out.println("Message sent from client");
      return true;
    }
    catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }
  
  public void closeSocket() {
    this.socket.close();
  }
  
  
  /**
   * @author josh
   * This is for use in main. Closes the socket upon termination
   */
  private static class Exit extends Thread {
    Client client = null;
    public Exit(Client s) {
      this.client = s;
    }
    
    public void run() {
      try {
        Thread.sleep(2000); //zzz
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
      this.client.closeSocket();
    }
  }
  
  public static void main(String[] args) throws IOException 
  {
    Client client = new Client();
    Runtime.getRuntime().addShutdownHook(new Exit(client));
    client.createAndListenSocket();
  }

}
