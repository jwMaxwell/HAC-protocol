/**
 * @author Joshua Maxwell
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

package hac_server;

import java.io.IOException;
import java.net.*;
import java.security.SecureRandom;

public class Client {
  DatagramSocket socket = null;
  Node parent = null;
  boolean gotPackyBoi = false;
  final int PACKET_SIZE = 1028;
  
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
        new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
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
        new AreYouThere().run();
        
        DatagramPacket incomingPacket = 
            new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
        socket.receive(incomingPacket);
        gotPackyBoi = true;
        String response = new String(incomingPacket.getData());
        
        System.out.println("Response from server:" + response);
        
        switch(response.split("//s+")[0]) {
          case "FUCC":
            send("PING", this.parent.getIp(), this.parent.getPort());
            break;
          default:
            send("PING", this.parent.getIp(), this.parent.getPort());
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
  }
  
  private void yikes() {
    // give up on life
    System.exit(0);
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
   * basically a straight copy/paste of the send pings thread in the server class
   */
  class SendPingyBois extends Thread {
    SecureRandom rand = new SecureRandom();
    private static final long LONGEST_TIME = 30000;
    
    @Override
    public void run() {
      super.run();
      
      while(true) { // while go brrr
        long temp = rand.nextLong(); //temp location to make sure that value is valid
        try {
          Thread.sleep(temp >= 0 ? temp % LONGEST_TIME : -temp % LONGEST_TIME); // sleep for abs(temp % MAX_TIME) millis
          socket.send( //send a packet
            new DatagramPacket("PING".getBytes(), "PING".getBytes().length, parent.getIp(), parent.getPort()) // ping pong time
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
  
  
  /**
   * @author josh
   * makes sure the server is alive
   */
  class AreYouThere extends Thread {
    /*
     * This is the longest time that the client will wait for a response
     * before giving up on life. This is in milliseconds and so
     * when we go through the conversion process, we will find that
     * this is equal to exactly equal to 30 seconds, which just so
     * happens to be the amount of time that we want to wait before
     * the client simply gives up on life
     */
    private static final long LONGEST_TIME = 30000;
    
    /**
     * This is the function that runs on a separate thread while the rest
     * of the program goes about its own business. All this does is wait and
     * see if the server responds within the allotted amount of time. If the
     * server does not respond fast enough, the client will give up and die
     * or something.
     */
    @Override
    public void run() {
      /*
       * I don't really know why I need this. I probably don't. Eclipse just
       * kind slapped this bad boy in here for whatever reason. I generally 
       * don't really use threads so I don't know whether or not this is
       * really needed for the thread to work
       */
      super.run();
      /*
       * This is the amount of time that the client should wait. This is a 
       * separate variable that we can use in case we need to extend the 
       * amount of time we should for wait
       */
      long timeForWait = LONGEST_TIME;
      /*
       * Fun fact: tiempo probably means time in Spanish. This is an apt
       * name because this variable holds the start time. This is used
       * to help the while loop figure out when it should fall into a 
       * deep depression and temporary sleep. It wouldn't be forever
       * because at some point this while loop would probably do stuff
       * again
       */
      long tiempo = System.currentTimeMillis();
      /*
       * This while loop is used to figure out whether or not the server
       * responded in an appropriate amount of time. If the server didn't
       * respond in the proper amount of time, we can go ahead and assume
       * that it has failed and that we need to wait for it to come back
       * online. The comparison function that the loop uses calculates if
       * the current time is larger that the start time plus the time that
       * it should wait.
       */
      while(!(System.currentTimeMillis() > tiempo + timeForWait)) {
        /*
         * This checks to see if a packet (also known as a packy boi) was
         * recieved. If one wasn't recieved, then the loop will keep on
         * skadoodling, otherwise, it will reset the "gotPackyBoi" 
         * variable and add some more time to the clock
         */
        if(gotPackyBoi) {
          /*
           * This resets the variable to the original value so that it will
           * be prepared to look for another packet. If this isn't reset
           * then the client won't work like it is supposed to. It's 
           * prolly best if you just leave this in here
           */
          gotPackyBoi = false;
          /*
           * This will add more time to the time to wait for the next
           * packet (also known as a packy boi) this is so that the
           * loop keeps on a going. If it doesn't, the client sure
           * will be confused
           */
          timeForWait += LONGEST_TIME;
        }
      }
      /*
       * Uh-oh spaghettios. If the code got to this point then the
       * client probably didn't receive a packet in the right amount
       * of time. If it ever gets to this point, then we can conclude
       * that the server went and died. That's a big yikes. Hence the
       * name of the function
       */
      yikes();
    }
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
