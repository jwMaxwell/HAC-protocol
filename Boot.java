package hac_protocol;

import java.io.IOException;
import java.util.Scanner;

public class Boot {
  public static final int PACKET_SIZE = 1024;
  
  public static void main(String[] args) {
    System.out.println("Would you like to launch in SERVER, P2P, or CLIENT mode?\n");
    Scanner cin = new Scanner(System.in);
    switch(cin.nextLine().toUpperCase()) {
     case "SERVER":
       new Server();
       break;
     case "P2P":
       //new P2P();
       break;
     case "CLIENT":
       try {
         new Client();
       } catch (IOException e) {
         e.printStackTrace();
       }
     default:
       System.out.println("learn to type\n");
    }
    cin.close();
  }
}
