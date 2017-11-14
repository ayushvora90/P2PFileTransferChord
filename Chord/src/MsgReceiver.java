import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;


public class MsgReceiver extends Thread {
	ChordNodeInfo peerInfo;
	MsgReceiver(ChordNodeInfo peerInfo){
		this.peerInfo = peerInfo;
	}
	public void run(){
		
		try {
			boolean receiverAlive = true;
			byte replyMsg[] = new byte[1024];
			DatagramSocket ds = new DatagramSocket(7000);
			DatagramPacket dp = new DatagramPacket(replyMsg, replyMsg.length);
			while(receiverAlive){
			ds.receive(dp);
			ByteArrayInputStream infoStream = new ByteArrayInputStream(replyMsg);
			ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(infoStream));
			ChordNodeInfo newPeerInfo = (ChordNodeInfo) in.readObject();
			System.out.println("New peer connected"+"\n"+newPeerInfo);
			
			
			infoStream.close();
			Socket newPeerSoc = new Socket(dp.getAddress(), newPeerInfo.getPort());
			ObjectOutputStream newPeerStream = new ObjectOutputStream(newPeerSoc.getOutputStream());
			newPeerStream.flush();
			newPeerStream.writeObject(peerInfo);
			newPeerStream.flush();
			newPeerSoc.close();
			in.close();
			newPeerStream.close();
			}
			ds.close();
			
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
