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

		// Loop control, for use later
		// In current implementation, will never be true
		boolean quit = false;

		// RNG for timing pings
		Random random = new Random();

		// Input scanner
		Scanner input = new Scanner(System.in);

		// Store last time the nodeIndex was displayed
		long lastDisplay = System.currentTimeMillis();

		// MAIN LOOP
		while (!quit) {
			// Ping all known hosts to let them know you've arrived
			System.out.println("Pinging all nodes");
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
						if (n.ip.equals(senderIP)) {
							n.online = true;
							n.tolc = System.currentTimeMillis();
							// System.out.println("Ping received"); // DEBUG
							break;
						}
					}
				}

				// If a node has not been heard from in more than NODE_TIMEOUT
				// milliseconds, it is considered dead
				for (Node n : nodeIndex) {
					if (n.getTSLC() > NODE_TIMEOUT) {
						n.online = false;
					}
				}

				// Check for user input and if available, parse for command
				String usrIn = null;
				Commands command = Commands.NONE;
				if (System.in.available() != 0) {
					usrIn = input.next();
					command = parseCommand(usrIn);
				}

				// Handle a command from the user
				switch (command) {
				case QUIT:
					System.out.print("Really quit? (y/n) ");
					if (input.next().strip().toLowerCase().equals("y")) {
						System.out.println("Terminating...");
						System.exit(0);
					} else if (input.next().strip().toLowerCase().equals("n")) {
						System.out.println("Aborting.");
					} else {
						System.out.println("Invalid choice.");
						System.out.println("Aborting.");
					}
					break;

				case DISPLAY:
					lastDisplay = System.currentTimeMillis();
					displayNodes();
					break;

				case INVALID:
					System.out.println("Invalid command.");
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
			System.exit(1);
		}

		// Load each address into a new node in nodeIndex
		while (cfgScanner.hasNextLine()) {
			nodeIndex.add(new Node(cfgScanner.nextLine()));
		}
	}

	/**
	 * Prints information about all nodes in nodeIndex
	 */
	private static void displayNodes() {
		System.out.println("------ Node Index ------");
		System.out.println(String.format("%-15s  %-6s  %s", "IP", "Status", "TSLC (ms)"));

		// Output node info
		for (Node n : nodeIndex) {
			String status = n.online ? "Up" : "Down";
			try {
				System.out.println(
						String.format("%-15s  %-6s  %d", n.ip.toString().replace("/", ""), status, n.getTSLC()));
			} catch (IOException e) {
				System.out.println(
						String.format("%-15s  %-6s  %d", n.ip.toString().replace("/", ""), "Unknown", (long) -1));
			}
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
				socket.connect(n.ip, port);
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

/**
 * @author Cameron Krueger
 * 
 * Node class. Contains basic information about nodes in the cluster such as
 * IP address, availability, and time of last contact (TOLC)
 */
final class Node {
	public Inet4Address ip;
	public boolean online;
	public long tolc;

	public Node(String ip) {
		try {
			// This call will attempt to contact the host at ip
			this.ip = (Inet4Address) Inet4Address.getByName(ip);
		} catch (UnknownHostException e) {
			System.out.println("Unknown host: " + ip);
		} finally {
			this.online = false;
			this.tolc = -1;
		}
	}

	/**
	 * Calculates and returns the time elapsed since contact was last received from
	 * this node
	 * 
	 * @return the time in milliseconds which has elapsed since last contact with
	 *         this node
	 * @throws IOException should never happen
	 */
	public long getTSLC() throws IOException {
		if (this.tolc > -1) {
			return System.currentTimeMillis() - this.tolc;
		}
		return -1;
	}
}
