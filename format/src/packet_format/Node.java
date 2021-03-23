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
	
	public static final int BYTES = 16; // A node uses 12 bytes when stored in a byte array
	
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
	public Node(byte[] b) throws UnknownHostException {		
		// Inet4Address is actually just a 32 bit value, 4 bytes
		byte[] addr = {b[0], b[1], b[2], b[3]};
		
		try {
			// This call will attempt to contact the host at ip
			this.address = (Inet4Address) Inet4Address.getByAddress(addr);
		} catch (UnknownHostException e) {
			System.err.printf("Error: Unknown host: %d.%d.%d.%d",
					addr[0] & 0xff, addr[1] & 0xff, addr[2] & 0xff, addr[3] & 0xff);
		}
		
		// Int is 32 bits wide, but we're only using lower 16, 2 bytes
		this.id = ((b[4] & 0xff) << 8) + (b[5] & 0xff);
		
		// Port is really unsigned short -- 16 bits
		port = ((b[6] & 0xff) << 8) + (b[7] & 0xff);
		
		// Status is already stored in a byte
		this.status = Status.getFromByte(b[8]);
		
		// Empty byte, reserved for possible future use, skip [9]
		
		// tslc is a long, but we only use 48 bits. 6 bytes
		//  (2^48 ms is roughly 8925.5 years)
		long tslc = ((b[10] * 0xff) << 40) + ((b[11] * 0xff) << 32) + ((b[12] * 0xff) << 24)
				+ ((b[13] * 0xff) << 16) + ((b[14] * 0xff) << 8) + (b[15] * 0xff);
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
	public byte[] toByteArray() {
		ArrayList<Byte> tmp = new ArrayList<Byte>();
		
		// Inet4Address is actually just a 32 bit value
		tmp.add((byte) this.address.getAddress()[0]);	// High byte
		tmp.add((byte) this.address.getAddress()[1]);	// Byte 1
		tmp.add((byte) this.address.getAddress()[2]);	// Byte 2
		tmp.add((byte) this.address.getAddress()[3]);	// Low byte
		
		// Int is 32 bits wide, but we're only using lower 16
		tmp.add((byte) ((this.id >> 8) & 0xFF));	// Higher byte
		tmp.add((byte) (this.id & 0xFF));			// Lower byte
		
		// Port is really unsigned short -- 16 bits
		tmp.add((byte) ((this.port >> 8) & 0xFF));
		tmp.add((byte) (this.port  & 0xFF));
		
		// Status is already stored in a byte, move into upper 3 bits
		tmp.add((byte) (this.status.value << 5));
		
		// Empty byte, reserved for possible future use
		tmp.add((byte) 0);
		
		// tslc is a long, but we only use 48 bits
		//  (2^48 ms is roughly 8925.5 years)
		long tslc = getTSLC();
		tmp.add((byte) ((tslc >> 40) & 0xFF));	// Byte 5
		tmp.add((byte) ((tslc >> 32) & 0xFF));	// Byte 4
		tmp.add((byte) ((tslc >> 24) & 0xFF));	// Byte 3
		tmp.add((byte) ((tslc >> 16) & 0xFF));	// Byte 2
		tmp.add((byte) ((tslc >> 8) & 0xFF));	// Byte 1
		tmp.add((byte) (tslc & 0xFF));			// Byte 0
		
		// Must convert ArrayList to Byte Array
		Byte[] b = new Byte[tmp.size()];
		b = tmp.toArray(b);
		
		// Now convert Byte array to byte array (Java is terrible)
		byte[] out = new byte[b.length];
		int i = 0;
		for (Byte bo: b) {
			out[i++] = (byte) bo;
		}
		return out;
	}
}
