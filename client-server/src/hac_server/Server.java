/**
 * @author Joshua Maxwell
 * @author Cameron Krueger
 * @version March 6, 2021
 * 
 * This programs acts as the server mode for the HAC protocol
 * 
 * TODO
 * 1. detect when a client becomes inactive
 * 2. fill the cache and keep it a reasonable size
 * 3. probably something else
 */

package hac_server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.ArrayList;

import packet_format.HACPack;
import packet_format.HACSock;

public class Server {
	HACSock socket = null;
	ArrayList<Node> nodes = new ArrayList<Node>(); //yikes
	ArrayList<String> cache = new ArrayList<String>();
	final int PORT = 9876;
	final int PACKET_SIZE = 1028;
	
	
	public Server() {}
	
	/**
	 * Listens for client messages and responds accordingly
	 */
	public void createAndListenSocket() 
	{
    try 
    {
      //makes the socket
      socket = new HACSock(PORT);
      byte[] incomingData = new byte[PACKET_SIZE];
      
      new SendPings().run();
      new CheckPings().run();
      
      while (true) 
      {
        //listens for shit
        HACPack incomingPacket = socket.receive();
        String message = incomingData.toString();
        InetAddress IPAddress = incomingPacket.getAddress();
        int port = incomingPacket.getPort();
        
        //bleep bloop, now it's on your screen
        System.out.println("Received message from client: " + message);
        System.out.println("Client IP: "+ IPAddress.getHostAddress());
        System.out.println("Client port: " + port);
        
        //split up that bad boy and get the first word
        String command = incomingPacket.getHeader();
        
        if(command.equals("JOIN")) {
          HACPack replyPacket = 
              new HACPack(goTo(new Node(IPAddress, port)), IPAddress, port);
          
          socket.send(replyPacket);
        }
        else 
          //sends a pingy boi
          socket.send(new HACPack("PING", "null", IPAddress, port));
      }
    } 
    catch (SocketException e) 
    {
      e.printStackTrace();
    } 
    catch (IOException i) 
    {
      i.printStackTrace();
    } 
	}
	
	/**
	 * Adds the client to the list of known clients
	 * @param s the Client
	 * @return the string to send the the client (where to go)
	 */
	public String goTo(Node s) {
	  if(!nodes.contains(s)) {
	    nodes.add(s);
	  }
	  return "GOTO SERVER";
	}
	
	/**
	 * read the name of the method
	 */
	public void closeSocket() {
	  this.socket.close();
	}
	
	/**
	 * @author josh
	 * Sends pings periodically to let nodes know that the server
	 * is still alive and available
	 */
	private class SendPings extends Thread {
	  //both variables are used to calculate time to send
	  private SecureRandom rand = new SecureRandom();
	  private static final long MAX_TIME = 30000;
	  
	  /**
	   * Sends pings periodically to let nodes know that the server
	   * is still alive and available
	   */
	  @Override
	  public void run() {
	    super.run(); // ?
	    
	    while(true) { // while go brrr
  	    long temp = rand.nextLong(); //temp location to make sure that value is valid
  	    try {
          Thread.sleep(temp >= 0 ? temp % MAX_TIME : -temp % MAX_TIME); // sleep for abs(temp % MAX_TIME) millis
          for(Node n : nodes) // for each node in the node list
            socket.send( //send a packet
              new HACPack("PING", "null", n.getIp(), n.getPort()) // ping pong time
            );
        } catch (InterruptedException e) { // uh-oh
          System.out.println("lol computer borked");
          e.printStackTrace(/* say wtf happened and where*/);
        }
        catch (IOException e) { //lmao, hope this doesn't happen
          System.out.println("Yikes, extra hecka borked");
          e.printStackTrace(/* say wtf happened and where*/);
        }
	    }
	  }
	}
	
	private class CheckPings extends Thread {
	  private static final long MAX_TIME = 30000;
	  
	  @Override
	  public void run() {
	    super.run();
	    while(true) // keep on going
	      for(Node n : nodes) // for each node
	        if(n.getLPTime() + MAX_TIME < System.currentTimeMillis()) // if its been too long
	          nodes.remove(nodes.indexOf(n)); //yeet that mfer
	  }
	}
	
	/**
	 * @author josh
	 * This is for use in main. Closes the socket upon termination
	 */
	private static class Exit extends Thread {
	  Server server = null;
	  public Exit(Server s) {
	    this.server = s;
	  }
	  
	  public void run() {
	    try {
        Thread.sleep(2000); //zzz
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
	    this.server.closeSocket();
	  }
	}

  public static void main(String[] args) 
  {
    Server server = new Server();
    Runtime.getRuntime().addShutdownHook(new Exit(server));
    server.createAndListenSocket();
    
  }
}
