package hac_p2p;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SocketListener {
	
	int port;
	DatagramSocket socket;
		
	private class ListenerThread extends Thread {
		
		private DatagramSocket socket = null;
		
		public void run() {
			try 
	        {
	            socket = new DatagramSocket();
	            InetAddress IPAddress = InetAddress.getByName("localhost");
	            byte[] incomingData = new byte[1024];
	            DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
	            socket.receive(incomingPacket);
	            String message = new String(incomingPacket.getData());
	            System.out.println("Response from server:" + message);
	            socket.close();
	        }
	        catch (UnknownHostException e) 
	        {
	            e.printStackTrace();
	        } 
	        catch (SocketException e) 
	        {
	            e.printStackTrace();
	        } 
	        catch (IOException e) 
	        {
	            e.printStackTrace();
	        }
		}
		
    }	
	
	
}
