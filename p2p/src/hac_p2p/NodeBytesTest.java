package hac_p2p;

import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;

import packet_format.*;
import packet_format.HACPacket.MaxFieldCountExceededException;

public class NodeBytesTest {

	public static void mainNOTMAINDONTRUNCHANGEFIRST(String[] args) throws UnknownHostException, MaxFieldCountExceededException {
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
		
		Node[] n = {node};
		
		HACPacket hp = new HACPacket(10, (Inet4Address) Inet4Address.getLocalHost(), n);
		
		DatagramPacket dp = new DatagramPacket(hp.toByteArray(), hp.toByteArray().length);
				
		HACPacket retHp = new HACPacket(dp.getData());
		
		Node[] arn = retHp.getDataAsNodeList();
		
		// Convert back
		Node convNode = arn[0]; // Node(nodeBytes);
		
		System.out.println();
		
		
		// Display after
		System.out.println(String.format("%-5s  %-15s  %-6s  %s", "ID", "Address", "Status", "TSLC (ms)"));
		status = convNode.isOnline() ? "Up" : "Down";
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
