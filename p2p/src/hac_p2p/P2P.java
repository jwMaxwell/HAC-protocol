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
import java.util.Scanner;
import java.util.Random;

public class P2P {

	// Default config file location
	private static String cfgPath = "./res/config";

	// The network port used for communication
	private static int port = 9876;

	// Max packet size
	private static final int PACKET_SIZE = 1024;

	// Socket receive timeout
	private static final int RECV_TIMEOUT = 1000;

	// Node lifespan - nodes are assumed to be dead after this long without
	// contact
	private static final long NODE_TIMEOUT = 30000;

	// Node index
	private static ArrayList<Node> nodeIndex = new ArrayList<Node>();

	// Formal command values
	private static enum Commands {
		QUIT, DISPLAY, INVALID, NONE;
	}
	
	// Universal input scanner
	static Scanner input = null;

	/**
	 * It really do just be the main method tho
	 * 
	 * @param args args[0] can optionally contain an alternate config file path
	 * @throws IOException for call to System.in.available(). Not sure when.
	 */
	public static void main(String[] args) throws IOException {
		// Load from specified path, if provided
		if (args.length > 0 && args[0] != null) {
			cfgPath = args[0];
		}
		
		loadConfig(cfgPath);
		input = new Scanner(System.in);

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
			System.out.println("Pinging all nodes");
			System.out.println();
			pingAll();

			// The amount of time spent receiving/accepting commands
			// This ensures a random interval between pings between 0 and 30
			// seconds
			long timeToReceive = random.nextInt(30000);

			// socket for later
			DatagramSocket socket = null;

			// Sender IP address
			InetAddress senderIP = null;

			// Set time of last ping
			long lastPingTime = System.currentTimeMillis();

			// Wait for incoming packets
			while (!(System.currentTimeMillis() - lastPingTime > timeToReceive)) {
				// Show current list of nodes if more than 5 seconds have
				// passed
				if (System.currentTimeMillis() - lastDisplay > 5000) {
					displayNodes();
					System.out.println(); // Blank line
					lastDisplay = System.currentTimeMillis();
				}

				// Incoming data packet (want this to be reset each time)
				byte[] incomingData = new byte[PACKET_SIZE];
				DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);

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

				// If PING packet received, update corresponding record in
				// nodeIndex
				String packetDataString = new String(incomingPacket.getData()).strip().toLowerCase();
				if (packetDataString.contains("ping")) {
					for (Node n : nodeIndex) {
						if (n.getAddress().equals(senderIP)) {
							n.setOnline(true);
							n.setTolc(System.currentTimeMillis());
							// System.out.println("Ping received"); // DEBUG
							break;
						}
					}
				}

				// If a node has not been heard from in more than NODE_TIMEOUT
				// milliseconds, it is considered dead
				for (Node n : nodeIndex) {
					if (n.getTSLC() > NODE_TIMEOUT) {
						n.setOnline(false);
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
		while (cfgScanner.hasNextLine()) {
			lineNumber++;
			String line = cfgScanner.nextLine();
			
			// Skip comments and blank lines
			if (line.startsWith("#") || line.isBlank() || line.isEmpty()) {
				continue;
			}
			
			// Separate line on comma (basically CSV)
			String tokens[] = line.split(",");
			
			// If there are not two tokens, alert user
			if (tokens.length < 2) {
				System.err.println("Error: Incorrect configuration file format at line " + lineNumber + ".");
				continue;	// Don't add invalid node
			}
			
			// Try to make an InetAddress object to see if IP token is valid
			try {
				// Must actually assign the value to make this throw an
				//  exception. Suppress unused variable warning.
				@SuppressWarnings("unused")
				InetAddress tmp = Inet4Address.getByName(tokens[0]);
			} catch (UnknownHostException e) {
				System.err.println("Error: Incorrect configuration file format at line " + lineNumber + ".");
				System.err.println(tokens[0] + ": not a valid IP address or hostname.");
				continue;	// Don't add invalid node
			}
			
			// If port number is not valid
			try {
				int port = Integer.parseInt(tokens[1].strip());
				if (port > 65536 || port < 0) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException e) {
				System.err.println("Error: Incorrect configuration file format at line " + lineNumber + ".");
				System.err.println(tokens[1] + ": not a valid port number.");
				continue;	// Don't add invalid node
			}
			
			// Add the node to the index
			nodeIndex.add(new Node(tokens[0].strip(), port));
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
		System.out.println(String.format("%-15s  %-6s  %s", "IP", "Status", "TSLC (ms)"));

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
			System.out.println(String.format("%-15s  %-6s  %d", addr, status, n.getTSLC()));
		}
	}

	/**
	 * Sends the PING signal to all nodes in nodeIndex
	 */
	public static void pingAll() {
		// Set up socket
		DatagramSocket socket = null;

		for (Node n : nodeIndex) {
			byte[] outgoingData = "PING".getBytes();
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
		} else {
			return Commands.INVALID;
		}
	}
}
