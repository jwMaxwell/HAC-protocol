/**
 * @author Cameron Krueger
 * 
 * Node class. Contains basic information about nodes in the cluster such as
 * IP address, availability, and time of last contact (TOLC)
 * 
 * Yes, this class is final. Yes, that's okay. I don't want this class to be
 * abstracted. It is what it needs to be. Just let it happen...
 */
 
package hac_p2p;

import java.net.*;
 
final class Node {
	private InetAddress address;
	private int port;
	private boolean online;
	private long tolc;

	public Node(String ip, int port) {
		try {
			// This call will attempt to contact the host at ip
			this.address = Inet4Address.getByName(ip);
		} catch (UnknownHostException e) {
			System.err.println("Error: Unknown host: " + ip);
		} finally {
			this.port = port;
			this.online = false;
			this.tolc = -1;
		}
	}
	
	/**
	 * @return the node's IP address
	 */
	public InetAddress getAddress() {
		return address;
	}
	
	/**
	 * @return the node's port number
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the node's port number
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return online status
	 */
	public boolean isOnline() {
		return online;
	}

	/**
	 * @param online true if node is online, else false
	 */
	public void setOnline(boolean online) {
		this.online = online;
	}

	/**
	 * @return the time of last contact
	 */
	public long getTolc() {
		return tolc;
	}

	/**
	 * @param tolc time of last contact (preferrably obtained with
	 *             System.currenTimeMillis()
	 */
	public void setTolc(long tolc) {
		this.tolc = tolc;
	}	

	/**
	 * Calculates and returns the time elapsed since contact was last received from
	 * this node
	 * 
	 * @return the time in milliseconds which has elapsed since last contact with
	 *         this node
	 */
	public long getTSLC() {
		if (this.tolc > -1) {
			return System.currentTimeMillis() - this.tolc;
		}
		return -1;
	}
}