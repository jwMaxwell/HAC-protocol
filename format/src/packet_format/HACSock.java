package packet_format;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class HACSock {
  DatagramSocket socket = null;
  private static final int PACKET_SIZE = 1028;
  
  public HACSock(int port) throws SocketException {
    this.socket = new DatagramSocket(port);
  }
  
  public HACPack receive() throws IOException {
    byte[] incomingData = new byte[PACKET_SIZE];
    DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
    socket.receive(incomingPacket);
    return new HACPack(incomingPacket);
  }
  
  public boolean send(HACPack packet) throws IOException {
    if(packet.getaddress() == null || packet.getPort() == 0)
      return false;
    
    socket.send(
        new DatagramPacket(packet.toString().getBytes(),
                           packet.toString().getBytes().length,
                           packet.getaddress(), packet.getPort())
        );
    return true;
  }
  
  public void close() {
    this.socket.close();
  }
}
