/**
 * @author Cameron Krueger
 * 
 * Node class. Contains basic information about nodes in the cluster such as
 * IP address, availability, and time of last contact (TOLC)
 * 
 * Yes, this class is final. Yes, that's okay. I don't want this class to be
 * abstracted. It is what it needs to be. Just let it happen...
 */
 
package packet_format;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
 
public final class Node {
	private int id;
	private Inet4Address address;
	private Status status;
	private int port;
	private long tolc;
	
	// Status bits
	public enum Status {
		ACTIVE((byte)0b001),	// Node is online and is operating normally
		ONLINE((byte)0b010),	// Node is online but unresponsive (application
								//  failure)
		OFFLINE((byte)0b011),	// Node is not reachable (total node failure)
		UNKNOWN((byte)0b100);	// Node status not known
		// 3 other values possible, but not necessary
		
		public final byte value;
		
		/**
		 * Converts a byte into its corresponding status
		 * @param b
		 * @return
		 */
		public static Status getFromByte(byte b) {
			if ((b & 0x07) == ACTIVE.value) { return ACTIVE; }
			else if ((b & 0x07) == ONLINE.value) { return ONLINE; }
			else if ((b & 0x07) == OFFLINE.value) { return OFFLINE; }
			else { return UNKNOWN; }
		}
		
		private Status(byte value) {
			this.value = value;
		}
	}

	/**
	 * Constructs a Node object
	 * @param id the node's unique ID
	 * @param addr the node's address
	 * @param port the port on which the node's HAC P2P application is listening
	 */
	public Node(int id, String addr, int port) {
		this.id = id;
		try {
			// This call will attempt to contact the host at addr
			this.address = (Inet4Address) Inet4Address.getByName(addr);
		} catch (UnknownHostException e) {
			System.err.println("Error: Unknown host: " + addr);
		} finally {
			this.port = port;
			this.status = Status.OFFLINE;
			this.tolc = -1;
		}
	}
	
	/**
	 * Constructs a Node object with id set to -1
	 * @param addr the node's address
	 * @param port the port on which the node's HAC P2P application is listening
	 */
	public Node(String addr, int port) {
		try {
			// This call will attempt to contact the host at ip
			this.address = (Inet4Address) Inet4Address.getByName(addr);
		} catch (UnknownHostException e) {
			System.err.println("Error: Unknown host: " + addr);
		} finally {
			this.id = -1;
			this.port = port;
			this.status = Status.OFFLINE;
			this.tolc = -1;
		}
	}	
	
	
	/**
	 * Constructs a node object from a byte array (used when decoding packets
	 * @param b a byte array containing node data
	 * @throws UnknownHostException if the host cannot be found
	 */
	public Node(Byte[] b) throws UnknownHostException {
		ArrayList<Byte> tmp = new ArrayList<Byte>();
		// Populate
		Collections.addAll(tmp, b);
		
		// Int is 32 bits wide, but we're only using lower 16, 2 bytes
		this.id = (tmp.get(0) << 8) + (tmp.get(1));
		
		// Inet4Address is actually just a 32 bit value, 4 bytes
		byte[] addr = {tmp.get(5), tmp.get(4), tmp.get(3), tmp.get(2)};
		
		try {
			// This call will attempt to contact the host at ip
			this.address = (Inet4Address) Inet4Address.getByAddress(addr);
		} catch (UnknownHostException e) {
			System.err.printf("Error: Unknown host: %o.%o.%o.%o", addr[0], addr[1], addr[2], addr[3]);
		}
		
		// Status is already stored in a byte
		this.status = Status.getFromByte(tmp.get(6));
		
		// Empty byte, reserved for possible future use, skip [7]
		
		// Port is really unsigned short -- 16 bits
		port = (tmp.get(8) << 8) + tmp.get(9);
		
		// tslc is a long, but we only use 48 bits. 6 bytes
		//  (2^48 ms is roughly 8925.5 years)
		long tslc = (tmp.get(10) << 40) + (tmp.get(11) << 32) + (tmp.get(12) << 24)
				+ (tmp.get(13) << 16) + (tmp.get(14) << 8) + tmp.get(15);
		if (tslc > 1) {
			this.tolc = System.currentTimeMillis() - tslc;
		} else {
			this.tolc = -1;
		}
	}
	
	/**
	 * @return id the node's ID, -1 if unknown
	 */
	public int getId() {
		return id;
	}
	
	
	/**
	 * @param id the node's ID
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	/**
	 * @return the node's IP address
	 */
	public Inet4Address getAddress() {
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
	public void setPort(short port) {
		this.port = port;
	}
	
	/**
	 * @return true if status is ACTIVE, else false
	 */
	public boolean isActive() {
		return this.status == Status.ACTIVE;
	}

	/**
	 * @return true if status is ACTIVE or ONLINE, else false
	 */
	public boolean isOnline() {
		return this.status == Status.ACTIVE || this.status == Status.ONLINE;
	}

	/**
	 * @param status the node's status
	 */
	public void setStatus(Status status) {
		this.status = status;
	}
	
	/**
	 * @return the node's status
	 */
	public Status getStatus() {
		return this.status;
	}

	/**
	 * @return the time of last contact with this node
	 */
	public long getTOLC() {
		return tolc;
	}

	/**
	 * @param tolc time of last contact (preferably obtained with
	 *             System.currenTimeMillis()
	 */
	public void setTOLC(long tolc) {
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
	
	/**
	 * Converts the entire node object to a byte array
	 */
	public Byte[] toByteArray() {
		ArrayList<Byte> tmp = new ArrayList<Byte>();
		
		// Int is 32 bits wide, but we're only using lower 16
		tmp.add((byte) ((this.id >> 8) & 0xFF));	// Higher byte
		tmp.add((byte) (this.id & 0xFF));			// Lower byte
		
		/**
		 * private Status status;
		 * private int port;
		 * private long tolc;
		 */
		
		// Inet4Address is actually just a 32 bit value
		tmp.add((byte) this.address.getAddress()[3]);	// Byte 3
		tmp.add((byte) this.address.getAddress()[2]);	// Byte 2
		tmp.add((byte) this.address.getAddress()[1]);	// Byte 1
		tmp.add((byte) this.address.getAddress()[0]);	// Byte 0
		
		// Status is already stored in a byte, move into upper 3 bits
		tmp.add((byte) (this.status.value << 5));
		
		// Empty byte, reserved for possible future use
		tmp.add((byte) 0);
		
		// Port is really unsigned short -- 16 bits
		tmp.add((byte) ((this.port >> 8) & 0xFF));
		tmp.add((byte) (this.port  & 0xFF));
		
		// tslc is a long, but we only use 48 bits
		//  (2^48 ms is roughly 8925.5 years)
		long tslc = getTSLC();
		tmp.add((byte) ((tslc >> 40) & 0xFF));	// Byte 5
		tmp.add((byte) ((tslc >> 32) & 0xFF));	// Byte 4
		tmp.add((byte) ((tslc >> 24) & 0xFF));	// Byte 3
		tmp.add((byte) ((tslc >> 16) & 0xFF));	// Byte 2
		tmp.add((byte) ((tslc >> 8) & 0xFF));	// Byte 1
		tmp.add((byte) (tslc & 0xFF));			// Byte 0
		
		Byte[] b = new Byte[tmp.size()];
		return tmp.toArray(b);
	}
}
