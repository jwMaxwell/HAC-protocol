/**
 * @author Cameron Krueger
 * 
 * P2P implementation of the notify and update functionality of a basic HAC
 * protocol
 */

package hac_p2p;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

import packet_format.*;
import packet_format.HACPacket.MaxDataLengthExceededException;
import packet_format.HACPacket.PacketType;
import packet_format.HACPacket.PacketTypeDataMismatchException;

import java.util.Random;

public class P2P {

	// Default config file location
	private static String cfgPath = "./res/config";

	// The network port used for communication
	private static int port = 9876;

	// Max packet size
	private static final int PACKET_SIZE = 65536;

	// Socket receive timeout
	private static final int RECV_TIMEOUT = 1000;

	// Node lifespan - nodes are assumed to be dead after this long without
	// contact
	private static final long NODE_TIMEOUT = 30000;

	// Node index
	private static ArrayList<Node> nodeIndex = new ArrayList<Node>();

	// Formal command values
	private static enum Commands {
		QUIT, DISPLAY, SEND_HBEAT, INVALID, NONE;
	}
	
	// This node's unique ID
	private static int id = -1;
	
	// This node's address
	private static Inet4Address address = null;
	
	// Universal input scanner
	static Scanner input = null;

	/**
	 * It really do just be the main method tho
	 * 
	 * @param args args[0] can optionally contain an alternate config file path
	 * @throws IOException for call to System.in.available(). Not sure when.
	 * @throws PacketTypeDataMismatchException 
	 */
	public static void main(String[] args) throws IOException, PacketTypeDataMismatchException {
		// Load from specified path, if provided
		if (args.length > 0 && args[0] != null) {
			cfgPath = args[0];
		}
		
		loadConfig(cfgPath);
		input = new Scanner(System.in);
		
		// Try to get IP address
		try (final DatagramSocket testSocket = new DatagramSocket()){
			testSocket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			address = (Inet4Address) testSocket.getLocalAddress();
			System.out.println("This node's address: " + address.getHostAddress());
		} catch (UnknownHostException e) {

			System.err.println("ERROR: Could not get this node's IP address");
			stdTerm(false, 3);
		}

		// Loop control, for use later
		// In current implementation, will never be true
		boolean quit = false;

		// RNG for timing pings
		Random random = new Random();

		// Store last time the nodeIndex was displayed
		long lastDisplay = System.currentTimeMillis();

		// MAIN LOOP
		while (!quit) {
			// Ping all known hosts to let them know you've arrived
			sendHeartbeat();

			// The amount of time spent receiving/accepting commands
			// This ensures a random interval between pings between 0 and 30
			// seconds
			long timeToReceive = random.nextInt(30000);

			// socket for later
			DatagramSocket socket = null;

			// Set time of last ping
			long lastPingTime = System.currentTimeMillis();

			// Wait for incoming packets
			while (!(System.currentTimeMillis() - lastPingTime > timeToReceive)) {
				// Show current list of nodes if more than 5 seconds have
				// passed
				if (System.currentTimeMillis() - lastDisplay > 5000) {
					sortNodes();
					displayNodes();
					System.out.println(); // Blank line
					lastDisplay = System.currentTimeMillis();
				}

				// Incoming data packet (want this to be reset each time)
				byte[] incomingData = new byte[PACKET_SIZE];
				DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);

				// Sender IP address
				InetAddress senderIP = null;
				int senderPort = 0;
				
				// Try to receive data with timeout
				try {
					// Instantiate socket and set the timeout
					socket = new DatagramSocket(port);
					socket.setSoTimeout(RECV_TIMEOUT);

					// Attempt to receive
					boolean timedOut = false;
					while (!timedOut) {
						try {
							socket.receive(incomingPacket);
							senderIP = incomingPacket.getAddress();
							senderPort = incomingPacket.getPort();
						} catch (SocketTimeoutException e) {
							// Set flag if timeout reached
							timedOut = true;
						}
					}
					socket.close();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				// If STATUS packet received, update corresponding record in
				// nodeIndex
				if (senderIP != null) {		// Should only be set if packet was received
					HACPacket hacPacket = new HACPacket(incomingPacket.getData());
					switch (hacPacket.getPacketType()) {
					
						case PING:
							for (Node n : nodeIndex) {
								if (n.getAddress().equals(senderIP)) {
									n.setStatus(Node.Status.ACTIVE);
									n.setTOLC(System.currentTimeMillis());
									// System.out.println("Ping received"); // DEBUG
									break;
								}
							}
							break;
							
						case STATUS:
							Node[] recNodes = hacPacket.getDataAsNodeList();
							
							// Go through each local node record and update info
							System.out.println(hacPacket.getNumFields());
							
							// Update records for other nodes and add new nodes, if present
							for (Node receivedNode: recNodes) {
								boolean isNewNode = true; 
								for (Node n: nodeIndex) {
									if (receivedNode.getAddress().equals(n.getAddress())) {
										// If node is in list, but has different ID, change ID to match received status
										if (receivedNode.getId() == n.getId()) {
											isNewNode = false;
											n.setStatus(receivedNode.getStatus());
											n.setTOLC(receivedNode.getTOLC());
										} else {
											isNewNode = false;
											n.setId(receivedNode.getId());
											n.setStatus(receivedNode.getStatus());
											n.setTOLC(receivedNode.getTOLC());
										}
									}
								}
								if (isNewNode) {
									System.out.println("ID: " + receivedNode.getId());
									nodeIndex.add(receivedNode);									
								}
							}
							
							// Update record for sender node
							boolean senderInIndex = false;
							int nodeCount = 0;
							for (Node n: nodeIndex) {
								if (n.getAddress().equals(senderIP)) {
									senderInIndex = true;
									if (n.getStatus() == Node.Status.OFFLINE) {
										System.out.println("INFO: Node at " + n.getAddress() + " is back online\n");
									}
									n.setStatus(Node.Status.ACTIVE);
									n.setTOLC(System.currentTimeMillis());
									break;	// save a little time
								}
								nodeCount++;
							}
							// If sender is not in nodeIndex, add
							if (!senderInIndex) {
								Node sender = new Node(senderIP.getHostAddress(), senderPort);
								sender.setId(nodeCount + 1);
								sender.setStatus(Node.Status.ACTIVE);
								sender.setTOLC(System.currentTimeMillis());
								nodeIndex.add(sender);
							}
							break;
							
						default:
							System.err.printf("ERROR: Unknown packet type. Raw byte %x", hacPacket.getPacketType().value);
							break;
					}
				}

				// If a node has not been heard from in more than NODE_TIMEOUT
				// milliseconds, it is considered dead
				for (Node n : nodeIndex) {
					if (n.getTSLC() > NODE_TIMEOUT) {
						n.setStatus(Node.Status.OFFLINE);
					}
				}

				// Check for user input and if available, parse for command
				String usrIn = null;
				Commands command = Commands.NONE;
				if (System.in.available() > 0) {
					usrIn = input.nextLine();
					// Skip if user entered whitespace
					if (!usrIn.isBlank()) {
						command = parseCommand(usrIn);
					}
				}

				// Handle a command from the user
				switch (command) {
				case QUIT:
					stdTerm(true, 0);
					break;

				case DISPLAY:
					lastDisplay = System.currentTimeMillis();
					System.out.println();
					displayNodes();
					break;
					
				case SEND_HBEAT:
					sendHeartbeat();
					break;

				case INVALID:
					System.out.println("Invalid command.\n");
					break;

				default: // Also handles Commands.NONE
					break;
				}
			}
		}
	}

	/**
	 * Loads configuration data from a configuration file
	 * 
	 * @param path the path of the configuration file
	 */
	private static void loadConfig(String path) {
		// Open config file
		File cfgFile = new File(path);
		Scanner cfgScanner = null;
		try {
			cfgScanner = new Scanner(cfgFile);
		} catch (FileNotFoundException e) {
			System.err.println("Error: " + path + " does not exist or you do not have permission to access it.");
			stdTerm(false, 1); // Exit with error code 1
		}

		// Load each address into a new node in nodeIndex
		int lineNumber = 0;
		ArrayList<Integer> idsFound = new ArrayList<Integer>(); 
		while (cfgScanner.hasNextLine()) {
			lineNumber++;
			String line = cfgScanner.nextLine();
			
			// Skip comments and blank lines
			if (line.startsWith("#") || line.isBlank() || line.isEmpty()) {
				continue;
			}
			
			// Separate line on comma (basically CSV)
			String tokens[] = line.split(",");
			
			// Strip all whitespace
			for (int i = 0; i < tokens.length; i++) {
				tokens[i] = tokens[i].strip();
			}
			
			// If there are not three tokens, alert user
			if (tokens.length < 3) {
				System.err.println("Error: Incorrect configuration file format at line " + lineNumber + ".");
				continue;	// Don't add invalid node
			}
			
			// Check for repeat ID numbers
			int currentId = Integer.parseInt(tokens[0]);
			for (int id: idsFound) {
				if (currentId == id) {
					System.err.println("Error: Incorrect configuration file format at line " + lineNumber + ".");
					String addr = "[error]";
					for (Node n: nodeIndex) {
						if (n.getId() == id) {
							addr = n.getAddress().toString();
						}
					}
					System.err.println(tokens[0] + ": ID number already assigned to node at " + addr + ".");
					continue;	// Don't add invalid node
				}
			}
			
			// Try to make an InetAddress object to see if IP token is valid
			try {
				// Must actually assign the value to make this throw an
				//  exception. Suppress unused variable warning.
				@SuppressWarnings("unused")
				InetAddress tmp = Inet4Address.getByName(tokens[1]);
			} catch (UnknownHostException e) {
				System.err.println("Error: Incorrect configuration file format at line " + lineNumber + ".");
				System.err.println(tokens[1] + ": not a valid IP address or hostname.");
				continue;	// Don't add invalid node
			}
			
			// If port number is not valid
			try {
				int port = Integer.parseInt(tokens[2]);
				if (port > 65536 || port < 0) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException e) {
				System.err.println("Error: Incorrect configuration file format at line " + lineNumber + ".");
				System.err.println(tokens[2] + ": not a valid port number.");
				continue;	// Don't add invalid node
			}
			
			// Add node's ID to idsFound
			idsFound.add(currentId);
			
			// Add the node to the index
			nodeIndex.add(new Node(currentId, tokens[1], port));
		}
		
		// Warn and exit if no node records were provided in the configuration 
		//  file
		if (nodeIndex.size() == 0) {
			System.err.println("Error: No nodes provided in configuration file.");
			System.out.println("Terminating...");
			stdTerm(false, 2);	// Exit with error code 2
		}
	}

	/**
	 * Prints information about all nodes in nodeIndex
	 */
	private static void displayNodes() {
		System.out.println("------------ Node Index ------------");
		System.out.println(String.format("%-4s  %-15s  %-6s  %s", "ID", "Address", "Status", "TSLC (ms)"));

		// Output node info
		for (Node n : nodeIndex) {
			String status = n.isOnline() ? "Up" : "Down";
			String addr = "";
			
			// Use host.domain text address if provided
			if (n.getAddress().toString().substring(0, n.getAddress().toString().indexOf("/")).length() > 0) {
				addr = n.getAddress().toString().substring(0, n.getAddress().toString().indexOf("/"));
			} else {
				addr = n.getAddress().toString().substring(n.getAddress().toString().indexOf("/") + 1);
			}
			
			// Print that b!
			System.out.println(String.format("%-4s  %-15s  %-6s  %d", n.getId(), addr, status, n.getTSLC()));
		}
	}
	
	/**
	 * Sends heartbeat and status to all nodes in nodeIndex
	 */
	// This is used in continuous state-transfer architecture
	public static void sendHeartbeat() {
		System.out.println("INFO: Pinging all nodes\n");
		// Set up socket
		DatagramSocket socket = null;

		// Construct a byte array containing nodeIndex for the HACPack
		// data block
		byte[] nodeIndexByteArray = new byte[nodeIndex.size() * Node.BYTES];
		int i = 0;
		for (Node n : nodeIndex) {
			for (byte b: n.toByteArray()) {
				nodeIndexByteArray[i++] = b;
			}
		}

		for (Node n : nodeIndex) {
			// Create a HAC STATUS packet, add all records in nodeIndex, and
			// put it in the data block
			byte[] outgoingData = null;
			try {
				outgoingData = (new HACPacket(id, address, PacketType.STATUS, nodeIndexByteArray)).toByteArray();
			} catch (MaxDataLengthExceededException e1) {
				// Ping packets are safe. Will not throw this exception
				e1.printStackTrace();
			}
			DatagramPacket outgoingPacket = new DatagramPacket(outgoingData, outgoingData.length);

			try {
				socket = new DatagramSocket(port);
				socket.connect(n.getAddress(), port);
				socket.send(outgoingPacket);
				socket.close();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void sortNodes() {
		// Sort nodes by ID
		boolean isInOrder = false;
		while (!isInOrder) {
			isInOrder = true;
			for (int i = 0; i < nodeIndex.size() - 1; i++) {
				if (nodeIndex.get(i).getId() > nodeIndex.get(i + 1).getId()) {
					Collections.swap(nodeIndex, i, i+1);
					isInOrder = false;
				}
			}
		}
		
		// Renumber all to remove gaps in IDs
		int i = 0;
		for (Node n: nodeIndex) {
			n.setId(i++);
		}
	}

	/**
	 * Sends the PING signal to all nodes in nodeIndex
	 */
	@Deprecated // This is NOT used in continuous state-transfer architecture
	public static void pingAll() {
		// Set up socket
		DatagramSocket socket = null;

		for (Node n : nodeIndex) {
			byte[] b = {};
			// Create a HAC ping packet and put it in the data block
			byte[] outgoingData = null;
			try {
				outgoingData = (new HACPacket(id, address, PacketType.PING, b)).toByteArray();
			} catch (MaxDataLengthExceededException e1) {
				// Ping packets are safe. Will not throw this exception
				e1.printStackTrace();
			}
			DatagramPacket outgoingPacket = new DatagramPacket(outgoingData, outgoingData.length);

			try {
				socket = new DatagramSocket(port);
				socket.connect(n.getAddress(), port);
				socket.send(outgoingPacket);
				socket.close();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Update all other nodes that a failure has been detected
	 * @param n the node which has failed
	 */
	@Deprecated
	public static void notifyAllOfFailure(Node n) {
		// Set up socket
		DatagramSocket socket = null;
		
		for (Node node : nodeIndex) {
			byte[] b = {};
			// Create a HAC ping packet and put it in the data block
			byte[] outgoingData = null;
			try {
				outgoingData = (new HACPacket(id, address, PacketType.STATUS, n.toByteArray()).toByteArray());
			} catch (MaxDataLengthExceededException e1) {
				// This mathematicallly cannot happen, as only one node is
				// being passed to the HACPack constructor 
				e1.printStackTrace();
			}
			DatagramPacket outgoingPacket = new DatagramPacket(outgoingData, outgoingData.length);

			try {
				socket = new DatagramSocket(port);
				socket.connect(n.getAddress(), port);
				socket.send(outgoingPacket);
				socket.close();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * Provides a standard way to terminate. This helps prevent resource leaks
	 * If confirmation is requested, the user has 5 seconds to respond before
	 * the program ignores the quit command and continues normally. This is a
	 * cheap way to prevent "blocking" without using threads
	 * 
	 * @param conf true if user confirmation is desired, else false
	 * @param val the error code value to pass to System.exit()
	 */
	public static void stdTerm(boolean conf, int val) {
		if (conf) {
			System.out.print("Really quit? Ignoring in 5 seconds... (y/n) ");
			long tor = System.currentTimeMillis();
			while (System.currentTimeMillis() - tor < 5000) {
				try {
					if (System.in.available() > 0) {
						String usrIn = input.next().strip().toLowerCase();
						if (usrIn.equals("y")) {
							System.out.println("Terminating...");
							// Close input if still open
							if (input != null) {
								input.close();
							}
							System.exit(val);
						} else if (usrIn.equals("n")) {
							System.out.println("Aborting.\n");
							return;
						} else {
							System.out.println("Invalid choice.");
							System.out.println("Aborting.\n");
							return;
						}
					}
				} catch (IOException e) {
					System.err.println("Error: Unknown IO error while waiting for input.");
					return;
				}
			}
			System.out.println("\nNo response. Continuing...\n");
		} else {
			// Close input if still open
			if (input != null) {
				input.close();
			}
			System.exit(val);
		}
	}

	/**
	 * Parses user input for a command
	 * 
	 * @param in user input
	 * @return a member of the Commands enum corresponding to the command parsed
	 *         from in String
	 */
	public static Commands parseCommand(String in) {
		in = in.strip().toLowerCase();
		if (in.equals("q")) {
			return Commands.QUIT;
		} else if (in.equals("d")) {
			return Commands.DISPLAY;
		} else if (in.equals("p")) {
			return Commands.SEND_HBEAT;
		} else {
			return Commands.INVALID;
		}
	}
}
