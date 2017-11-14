import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class ChordRing {
	private static void create(ChordNode peer) throws RemoteException {
		Successor succ = new Successor(peer.getInfo().getIpAddr(), peer
				.getInfo().getPort(), peer.getInfo().getId());
		Predecessor pred = new Predecessor(null,0,null);
		peer.setAlive(true);
		peer.setSucc(succ);
		peer.setPred(pred);
		peer.infoMsgReceiver();
	}

	public static void main(String[] args) throws NoSuchAlgorithmException,
			ClassNotFoundException, IOException, NotBoundException {
		// TODO Auto-generated method stub
		String op = args[1];
		String fileName = args[2];
		ChordNode peer = new ChordNode(Integer.parseInt(args[0]));
		System.out.println("The peer informartion for this node is\n"+ peer.getInfo());
		ChordNodeInfo newPeerInfo = peer.sendInfoMsg();
		if (newPeerInfo == null) {
			create(peer);
			System.out.println("Peer created successfully");
		} else {
			peer.join(newPeerInfo);
			System.out.println("The peer information for the new node is\n"+newPeerInfo);
			System.out.println("Peer joined successfully");
		}
		
		Stabilize sb = new Stabilize(peer);
		sb.start();
		FixFingers ff = new FixFingers(peer);
		ff.start();
		CheckPredecessor cp = new CheckPredecessor(peer);
		cp.start();
		FileRequestManager frm = new FileRequestManager(peer.getInfo().getPort());
		frm.start();
		
		if(op.equals("PUT")){
		FileUploader fu = new FileUploader(fileName,peer);
		fu.start();
		} else if(op.equals("GET")){
		FileDownloader fd = new FileDownloader(fileName,peer);
		fd.start();
		} else{
			
		}
	}

}
