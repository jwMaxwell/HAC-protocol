/**
 * @author Joshu Maxwell
 * @author Cameron Krueger
 * @version March 20, 2021
 * This class creates an application-layer packet structure that is
 * compatible with the HAC protocol
 */
package packet_format;

import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import packet_format.Node.Status;

public final class HACPacket {
	
	// UDP info
	private int port;		// Max value 65536
	
	// Header
	//   Source info
	private int id;			// Max value 65536
	private Inet4Address address;
	//   Packet info
	private PacketType type;
	private int length;		// Max value 65411 (this will cause issues)
	private short numFields;  	// Max value 4093 (this will cause issues)

	// Body
	//   Data
	private byte[] data;	// Max length 65411 bytes
	
	private byte[] EMPTY_DATA = {};
	
	// Constants
	public static final short NO_DATA= 0; // Use in instantiation to avoid (short)0
	public static final int MAX_FIELD_COUNT = 4093;	 // Max value for numFields
	public static final int MAX_DATA_LENGTH = 65411; // Max value for length
	
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
	 * Constructs a HAC packet object
	 * @param id the source node's ID
	 * @param addr the source node's IP address
	 * @param nodes an array of fields containing node info
	 * @throws MaxFieldCountExceededException if length of nodes[] is greater than MAX_FIELD_COUNT
	 */
	public HACPacket(int id, Inet4Address addr, Node[] nodes) throws MaxFieldCountExceededException {
		this.id = id;
		this.address = addr;
		this.type = PacketType.STATUS;
		if (nodes.length > MAX_FIELD_COUNT) {
			throw new MaxFieldCountExceededException("field count " + nodes.length
					+ " exceeds maximum packet field count of " + MAX_FIELD_COUNT + " fields.");
		} 
		else {
			this.numFields = (short) nodes.length;
		}
		this.length = nodes.length * Node.BYTES;	// Each node info field is 12 bytes 
		for (Node n: nodes) {
			n.toByteArray();  	// FIXME
		}
	}
	
	/**
	 * Constructs a HAC packet object
	 * @param id the source node's ID
	 * @param addr the source node's IP address
	 * @param type the packet type
	 * @param data packet data
	 * @throws MaxDataLengthExceededException if data is longer than max packet data block size
	 */
	public HACPacket(int id, Inet4Address addr, PacketType type, byte[] data) throws MaxDataLengthExceededException{
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
			if (data.length > MAX_DATA_LENGTH) {
				throw new MaxDataLengthExceededException("data byte array length " + data.length
						+ " exceeds maximum packet data block size of " + MAX_DATA_LENGTH + " bytes.");
			}
			this.length = data.length;
		}
		this.numFields = 0;
		this.data = data;
	}
	
	/**
	 * Constructs a HAC packet object. This constructor is only applicable for
	 * PING, ACK, INIT, CRQ, and RESEND packets, as their data block is empty 
	 * @param id the source node's ID
	 * @param addr the source node's IP address
	 * @param type the packet type
	 * @throws PacketTypeDataMismatchException if type is not one of the no-data packet types 
	 */
	public HACPacket(int id, Inet4Address addr, PacketType type) throws PacketTypeDataMismatchException {
		this.id = id;
		this.address = addr;
		this.type = type;
		// Ping, Ack, Init, CRQ, and Resend packets do not carry information in data field
		//  TODO Why did I add a resend flag? What is this for?
		if (!(type == PacketType.PING
			|| type == PacketType.ACK
			|| type == PacketType.INIT
			|| type == PacketType.CRQ
			|| type == PacketType.RESEND)) {
			throw new PacketTypeDataMismatchException("Packet type " + type.toString() + " not valid for no-data constructor.");
		}
		this.numFields = 0;
		this.data = EMPTY_DATA;
	}

	/**
	 * Constructs a HACPack object from a byte array (used when decoding
	 * DatagramPacket objects)
	 */
	public HACPacket(byte[] b) {		
		ArrayList<Byte> tmp = new ArrayList<Byte>();
		
		// Must convert byte array to Byte array
		Byte[] in = new Byte[b.length];
		int i = 0;
		for (byte bo: b) {
			in[i++] = (Byte) bo;
		}
		
		// Populate
		Collections.addAll(tmp, in);
		
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
		
		// Load the data into the data block (using original byte array avoids
		// O(n) type conversion)
		this.data = Arrays.copyOfRange(b, 12, b.length);
	}
	
	/**
	 * Copy constructor
	 * @param hp the HAC packet object to copy
	 */
	public HACPacket(HACPacket hp) {
		this.port = hp.port;
		this.address = hp.address; 
		this.id = hp.id;
		this.type = hp.type;
		this.length = hp.length;
		this.numFields = hp.numFields;
		this.data = hp.data;
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
	public byte[] getData() {
		return data;
	}
	
	public Node[] getDataAsNodeList() {
		Node[] nodes = new Node[this.numFields];
		for (int i = 0; i < this.numFields; i ++) {
			try {		// This is a nightmare.
				nodes[i] = new Node(Arrays.copyOfRange(this.data, i * Node.BYTES, (i * Node.BYTES) + Node.BYTES));
			} catch (UnknownHostException e) {			// Look at these majik numbers
				short id = (short) ((this.data[i * 12] << 8 & 0xFF) + this.data[i * 12] & 0xFF);
				System.err.println("ERROR: Could not convert node with ID " + id
						+ ". Node is unreachable");
				e.printStackTrace();
			}
		}
		return nodes;
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
	 * Converts HAC packet object to an array of Bytes
	 */
	// FIXME convert methods that return Byte or accept as param
	// need to be changed so that they input/output byte (primitive)
	// so that conversions aren't required everywhere
	public byte[] toByteArray() {
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
		
		// Must convert data byte array to Byte array
		Byte[] dataBytes = new Byte[this.data.length];
		int i = 0;
		for (byte bo: this.data) {
			dataBytes[i++] = (Byte) bo;
		}
		// Add data block
		Collections.addAll(tmp, dataBytes);
		
		// Must convert ArrayList to Byte Array
		Byte[] b = new Byte[tmp.size()];
		b = tmp.toArray(b);
		
		// Now convert Byte array to byte array (Java is the bane of my
		// existence)
		byte[] out = new byte[b.length];
		int j = 0;
		for (Byte bo: b) {
			out[j++] = (byte) bo;
		}
		return out;
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

	/**
	 * @return a copy of this HACPack object
	 */
	public HACPacket copy() {
		return new HACPacket(this);
	}

	/**
	 * Checks if the values of this are equal to the values of another HACPack
	 * @param hp the HACPack to compare with
	 * @return take a guess
	 */
	public boolean equals(HACPacket hp) { // This should work perfectly
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
	
	/**
	 * @author cameron
	 * Use when the user tries to specify a data block length greater than
	 * 65411 bytes
	 */
	public class MaxDataLengthExceededException extends Exception {
		public MaxDataLengthExceededException(String s) {
			super(s);
		}
	}
	
	/**
	 * @author cameron
	 * Use when the user tries to specify a node info field count greater than
	 * 4093 fields
	 */
	public class MaxFieldCountExceededException extends Exception {
		public MaxFieldCountExceededException(String s) {
			super(s);
		}
	}
}
