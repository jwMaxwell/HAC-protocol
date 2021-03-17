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

package hac_protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class Server {
	DatagramSocket socket = null;
	ArrayList<Node> slaves = new ArrayList<Node>(); //yikes
	ArrayList<String> cache = new ArrayList<String>();
	final int PORT = 9876;
	
	public Server() {}
	
	/**
	 * Listens for client messages and responds accordingly
	 */
	public void createAndListenSocket() 
	{
    try 
    {
      //makes the socket
      socket = new DatagramSocket(PORT);
      byte[] incomingData = new byte[Boot.PACKET_SIZE];

      while (true) 
      {
        //listens for shit
        DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
        socket.receive(incomingPacket);
        String message = new String(incomingPacket.getData());
        InetAddress IPAddress = incomingPacket.getAddress();
        int port = incomingPacket.getPort();
        
        //bleep bloop, now it's on your screen
        System.out.println("Received message from client: " + message);
        System.out.println("Client IP: "+ IPAddress.getHostAddress());
        System.out.println("Client port: " + port);
        
        //split up that bad boy and get the first word
        String command = message.split("//s+")[0];
        
        if(command.equals("JOIN")) {
          String reply = goTo(new Node(IPAddress, port)); //says where to go
          byte[] data = reply.getBytes();
          DatagramPacket replyPacket =
                  new DatagramPacket(data, data.length, IPAddress, port);
          
          socket.send(replyPacket);
        }
        else if(command.equals("BACK")) {
          //time to spam that ding dong to high heaven
          for(String t : this.cache) { 
            byte[] data = t.getBytes();
            DatagramPacket replyPacket =
                    new DatagramPacket(data, data.length, IPAddress, port);
            
            socket.send(replyPacket);
          }
        }
        else 
          //sends a pingy boi
          socket.send(
              new DatagramPacket("PING".getBytes(), "PING".getBytes().length, IPAddress, port)
              );
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
	  if(!slaves.contains(s)) {
	    slaves.add(s);
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

