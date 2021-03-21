package hac_p2p;

import java.net.UnknownHostException;

import packet_format.*;

public class NodeBytesTest {

	public static void mainNOTMAINDONTRUNCHANGEFIRST(String[] args) throws UnknownHostException {
		Node node = new Node(10, "192.168.0.142", 9876);
		node.setTOLC(System.currentTimeMillis());
		
		// Display before
		System.out.println(String.format("%-5s  %-15s  %-6s  %s", "ID", "Address", "Status", "TSLC (ms)"));
		String status = node.isOnline() ? "Up" : "Down";
		String addr = "";
		
		// Use host.domain text address if provided
		if (node.getAddress().toString().substring(0, node.getAddress().toString().indexOf("/")).length() > 0) {
			addr = node.getAddress().toString().substring(0, node.getAddress().toString().indexOf("/"));
		} else {
			addr = node.getAddress().toString().substring(node.getAddress().toString().indexOf("/") + 1);
		}
		
		// Print that b!
		System.out.println(String.format("%-5d  %-15s  %-6s  %d", node.getId(), addr, status, node.getTSLC()));
		
		// Convert to bytes
		byte[] nodeBytes = node.toByteArray();
		
		// Convert back
		Node convNode = new Node(nodeBytes);
		
		System.out.println();
		
		
		// Display after
		System.out.println(String.format("%-5s  %-15s  %-6s  %s", "ID", "Address", "Status", "TSLC (ms)"));
		status = node.isOnline() ? "Up" : "Down";
		addr = "";
		
		// Use host.domain text address if provided
		if (convNode.getAddress().toString().substring(0, convNode.getAddress().toString().indexOf("/")).length() > 0) {
			addr = convNode.getAddress().toString().substring(0, convNode.getAddress().toString().indexOf("/"));
		} else {
			addr = convNode.getAddress().toString().substring(convNode.getAddress().toString().indexOf("/") + 1);
		}
		
		// Print that b!
		System.out.println(String.format("%-5d  %-15s  %-6s  %d", convNode.getId(), addr, status, convNode.getTSLC()));
		
		
	}

}
