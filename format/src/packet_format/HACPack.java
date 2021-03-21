/**
 * @author Joshu Maxwell
 * @version March 20, 2021
 * This class creates forces a packet structure that is compatible with the HAC protocol
 */
package packet_format;

import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;

import packet_format.Node.Status;

public class HACPack {
	
	// UDP info
	private int port;		// Max value 65536
	
	// Header
	//   Source info
	private int id;			// Max value 65536
	private Inet4Address address;
	//   Packet info
	private PacketType type;
	private int length;		// Max value 65411
	private short numFields;  	// Max value 4093

	// Body
	//   Data
	private Byte[] data;	// Max length 65411 bytes
	
	public enum PacketType {
		RAW((byte) 0b10000000),
		PING((byte) 0b01000000),
		ACK((byte) 0b00100000),
		INIT((byte) 0b00010000),
		CRQ((byte) 0b00001000),
		CSUM((byte) 0b00000100),
		STATUS((byte) 0b00000010),
		RESEND((byte) 0b00000001),
		UNKNOWN((byte) 0b00000000); // DO NOT USE
		
		public final byte value;
		
		private PacketType(byte value) {
			this.value = value;
		}
		
		public static PacketType getFromByte(byte b) {
			if ((b) == RAW.value) { return RAW; }
			else if (b == PING.value) { return PING; }
			else if (b == ACK.value) { return ACK; }
			else if (b == INIT.value) { return INIT; }
			else if (b == CRQ.value) { return CRQ; }
			else if (b == CSUM.value) { return CSUM; }
			else if (b == STATUS.value) { return STATUS; }
			else if (b == RESEND.value) { return RESEND; }
			else { return UNKNOWN; }
		}
		
		public String toString() {
			if (value == RAW.value) { return "RAW"; }
			else if (value == PING.value) { return "PING"; }
			else if (value == ACK.value) { return "ACK"; }
			else if (value == INIT.value) { return "INIT"; }
			else if (value == CRQ.value) { return "CRQ"; }
			else if (value == CSUM.value) { return "CSUM"; }
			else if (value == STATUS.value) { return "STATUS"; }
			else if (value == RESEND.value) { return "STATUS"; }
			else { return "error"; }	// Yikes
		}
	}
	
	/**
	private String header = null; //the command
	private String body = null; //the not command
	private int port;
	private InetAddress IP;
	*/

	/** 
	 * Constructs a HAC packet object
	 * @param id the source node's ID
	 * @param addr the source node's IP address
	 * @param fields the number of node info fields in the data block
	 * @param nodes an array of fields containing node info
	 * @throws PacketTypeDataMismatchException
	 */
	public HACPack(int id, Inet4Address addr, Short numFields, Node nodes[]) throws PacketTypeDataMismatchException {
		this.id = id;
		this.address = addr;
		this.type = PacketType.STATUS;
		this.numFields = numFields;
		this.length = numFields * 12;	// Each node info field is 12 bytes 
		for (Node n: nodes) {
			n.toByteArray();
		}
	}

	/**
	 * Constructs a HAC packet object
	 * @param id the source node's ID
	 * @param addr the source node's IP address
	 * @param type the packet type
	 * @param length the length of the data block
	 * @param data packet data
	 */
	public HACPack(int id, Inet4Address addr, PacketType type, short length, Byte[] data){
		this.id = id;
		this.address = addr;
		this.type = type;
		// Ping, Ack, Init, CRQ, and Resend packets do not carry information in data field
		//  TODO Why did I add a resend flag? What is this for?
		if (type == PacketType.PING
			|| type == PacketType.ACK
			|| type == PacketType.INIT
			|| type == PacketType.CRQ
			|| type == PacketType.RESEND) {
			this.length = 0;
		} else {
			this.length = length;
		}
		this.numFields = 0;
		this.data = data;
	}

	
	/**
	 * Copy constructor
	 * @param hp the HAC packet object to copy
	 */
	public HACPack(HACPack hp) {
		this.port = hp.port;
		this.address = hp.address; 
		this.id = hp.id;
		this.type = hp.type;
		this.length = hp.length;
		this.numFields = hp.numFields;
		this.data = hp.data;
	}
	
	/**
	 * Constructs a HACPack object from a byte array (used when decoding
	 * DatagramPacket objects)
	 */
	public HACPack(Byte[] b) {
		ArrayList<Byte> tmp = new ArrayList<Byte>();
		// Populate
		Collections.addAll(tmp, b);
		
		// Get source address
		// Inet4Address is actually just a 32 bit value, 4 bytes
		byte[] sourceAddr = {tmp.get(3), tmp.get(2), tmp.get(1), tmp.get(0)};
		
		try {
			// This call will attempt to contact the host at ip
			this.address = (Inet4Address) Inet4Address.getByAddress(sourceAddr);
		} catch (UnknownHostException e) {
			System.err.printf("Error: Unknown host: %o.%o.%o.%o",
					sourceAddr[0], sourceAddr[1], sourceAddr[2], sourceAddr[3]);
		}
		
		// Get source id
		// Int is 32 bits wide, but we're only using lower 16, 2 bytes
		this.id = (tmp.get(4) << 8) + (tmp.get(5));

		// Get source length
		// Int is 32 bits wide, but we're only using lower 16, 2 bytes
		this.length = (tmp.get(6) << 8) + (tmp.get(7));
		
		// Get type flags
		// Type is already stored in a byte
		this.type = PacketType.getFromByte(tmp.get(8));
	
		// Next 12 bits are reserved (empty)
		// Empty byte, skip [9]
		
		// Get number of fields
		// Highest 4 bits are empty            !
		this.id = ((tmp.get(10) << 8) & 0x0F) + (tmp.get(11));
		
		// Load the rest of the data into the data block
		Byte[] a = new Byte[tmp.subList(12, tmp.size()).size()];
		this.data = tmp.subList(12, tmp.size()).toArray(a);
	}
	
	
	/**
	 * @return the source node's id
	 */
	public int getSourceId() {
		return id;
	}

	/**
	 * @param id the source node's id
	 */
	public void setSourceId(int id) {
		this.id = id;
	}

	/**
	 * @return the type of the packet
	 */
	public PacketType getPacketType() {
		return type;
	}

	/**
	 * @param type the type of the packet
	 */
	/*
	public void setType(PacketType type) {
		this.type = type;
	}*/

	/**
	 * @return the length of the data block in bytes
	 */
	public int getLength() {
		return length;
	}

	/**
	 * @param length the length of the data block in bytes
	 */
	/*
	public void setLength(int length) {
		this.length = length;
	}*/

	/**
	 * Only applies to STATUS packets
	 * @return the number of fields in the data block
	 * @throws PacketTypeDataMismatchException if this is not a status packet
	 */
	public short getNumFields() throws PacketTypeDataMismatchException {
		if (this.type != PacketType.STATUS) {
			throw new PacketTypeDataMismatchException("Non-status packets do not contain fields.");
		}
		return numFields;
	}

	/**
	 * Only applies to STATUS packets
	 * @param numFields the number of node info fields present in the data
	 * block  
	 */
	/*
	public void setNumFields(short numFields) {
		this.numFields = numFields;
	}*/

	/**
	 * @return the packet's data block as a byte array
	 */
	public Byte[] getData() {
		return data;
	}

	/**
	 * @param data the packet's data block
	 */
	/*
	public void setData(Byte[] data) {
		this.data = data;
	}*/
	
	/**
	 * @return the source node's listening port
	 */
	public int getSourcePort() {
		return this.port;
	}

	/**
	 * @param port the source node's listening port
	 */
	public void setSourcePort(int port) {
		this.port = port;
	}
	
	/**
	 * @return the source node's address
	 */
	public Inet4Address getSourceAddress() {
		return address;
	}

	/**
	 * @param address the source node's address
	 */
	public void setSourceAddress(Inet4Address address) {
		this.address = address;
	}
	
	/**
	 * Converts HAC packet object to an array of bytes
	 */
	public Byte[] toByteArray() {
		ArrayList<Byte> tmp = new ArrayList<Byte>();
		
		// Add source IP address
		// Inet4Address is actually just a 32 bit value
		tmp.add((byte) this.address.getAddress()[3]);	// Byte 3
		tmp.add((byte) this.address.getAddress()[2]);	// Byte 2
		tmp.add((byte) this.address.getAddress()[1]);	// Byte 1
		tmp.add((byte) this.address.getAddress()[0]);	// Byte 0
		
		// Add source id
		// Int is 32 bits wide, but we're only using lower 16
		tmp.add((byte) ((this.id >> 8) & 0xFF));	// Higher byte
		tmp.add((byte) (this.id & 0xFF));			// Lower byte
		
		// Add body length
		// Int is 32 bits wide, but we're only using lower 16
		tmp.add((byte) ((this.length >> 8) & 0xFF));	// Higher byte
		tmp.add((byte) (this.length & 0xFF));			// Lower byte
		
		// Add type flags
		// Type is already stored in a byte
		tmp.add((byte) (this.type.value & 0xFF));
		
		// Next 12 bits are reserved (empty)
		// Add an empty byte and add the other 4 empty bits in the next step
		// Empty byte, reserved for possible future use
		tmp.add((byte) 0);
		
		// Add number of fields
		// Highest 4 bits are empty            !
		tmp.add((byte) ((this.numFields >> 8) & 0x0F));	// Higher byte
		tmp.add((byte) (this.numFields & 0xFF));		// Lower byte
		
		// Add data block
		Collections.addAll(tmp, this.data);
		
		Byte[] b = new Byte[tmp.size()];
		return tmp.toArray(b);		
	}

	/**
	 * Converts HAC packet to a DatagramPacket that can be sent by a socket
	 * @param IP address of recipient
	 * @param port port number of recipient
	 * @return DatagramPacket representation of this HACPacket
	 */
	public DatagramPacket buildDatagramPacket(InetAddress IP, int port) {
		return new DatagramPacket(this.toString().getBytes(),
				this.toString().getBytes().length,
				IP,
				port);
	}

	public HACPack copy() {
		return new HACPack(this);
	}

	/**
	 * Checks if the values of this are equal to the values of another HACPack
	 * @param hp the HACPack to compare with
	 * @return take a guess
	 */
	public boolean equals(HACPack hp) { // This should work perfectly
		if (this.port == hp.port
				&& this.address == hp.address 
				&& this.id == hp.id 
				&& this.type == hp.type
				&& this.length == hp.length
				&& this.numFields == hp.numFields
				&& this.data.equals(hp.data)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * bippity boppity it's now a string, would you look at that
	 * @return a string representation of this object
	 */
	@Override
	public String toString() {
		return this.id + " " + this.address + " " + this.type.toString() + " " + length + " " + data;
	}
	
	
	/**
	 * @author cameron
	 * Tis an Exception. Use when the user makes a non-status packet and tries
	 * to add node info fields
	 */
	public class PacketTypeDataMismatchException extends Exception {
		public PacketTypeDataMismatchException(String s) {
			super(s);
		}
	}
}
