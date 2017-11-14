import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class ChordNode extends UnicastRemoteObject implements
		ChordNodeInterface, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Successor succ;
	private Predecessor pred;
	private ChordNodeInfo info;
	private List<ChordNodeInfo> fingerTable;
	private boolean alive;
	private static Registry rmiReg;

	public ChordNode(int port) throws UnknownHostException, RemoteException,
			NoSuchAlgorithmException {
		// TODO Auto-generated constructor stub
		String ip = InetAddress.getLocalHost().getHostAddress();
		BigInteger id = performNodeHash(ip, port);
		info = new ChordNodeInfo(ip, port, id);
		rmiReg = LocateRegistry.createRegistry(1099);
		rmiReg.rebind(id+"", this);
		succ = new Successor(null, 0, null);
		pred = new Predecessor(null, 0, null);
		fingerTable = new ArrayList<ChordNodeInfo>();
		alive = false;

	}

	public BigInteger performNodeHash(String ipAddr, int port)
			throws NoSuchAlgorithmException, RemoteException {
		String combNodeName = ipAddr + port;
		MessageDigest mD = MessageDigest.getInstance("SHA1");
		byte keyNameBytes[] = combNodeName.getBytes();
		byte hashVal[] = mD.digest(keyNameBytes);
		String hash = "";
		for (int i = 0; i < hashVal.length; i++) {
			// getting an unsigned integer and then converting to a hex string.
			String tmp = Integer.toHexString(hashVal[i] & 0xff);
			// appending leading zeros to single digit hex strings
			if (tmp.length() == 1) {
				tmp = "0" + tmp;
			}
			hash += tmp;
		}
		// Converting the hash which is a series of hex strings into a big
		// integer to get an identifier in the sha1 key-space.
		return new BigInteger(hash, 16);
	}

	public ChordNodeInfo sendInfoMsg() throws IOException,
			ClassNotFoundException, RemoteException{
		DatagramSocket ds = new DatagramSocket();
		ds.setBroadcast(true);
		ds.setReuseAddress(true);
		ByteArrayOutputStream infoStream = new ByteArrayOutputStream(2048);
		ObjectOutputStream out = new ObjectOutputStream(
				new BufferedOutputStream(infoStream));
		out.flush();
		out.writeObject(this.info);
		out.flush();
		out.close();
		byte info[] = infoStream.toByteArray();
		// Send broadcast to all
		DatagramPacket dp = new DatagramPacket(info, info.length,
				InetAddress.getByName("255.255.255.255"), 7000);
		
		ds.send(dp);
		ds.close();
		// Waiting for a reply from another node
		ServerSocket srvSoc = new ServerSocket(this.info.getPort());
		srvSoc.setSoTimeout(3000);
		try {
			Socket replySocket = srvSoc.accept();
			ObjectInputStream in = new ObjectInputStream(
					replySocket.getInputStream());
			ChordNodeInfo peerInfo = (ChordNodeInfo) in.readObject();
			srvSoc.close();
			in.close();
			return peerInfo;
		} catch (SocketTimeoutException ste) {
			System.out.println("No reply received");
			srvSoc.close();
			return null;
		}
	}

	public Successor getSucc() throws RemoteException {
		return succ;
	}

	public void setSucc(Successor succ) throws RemoteException {
		this.succ = succ;
	}

	public Predecessor getPred() throws RemoteException {
		return pred;
	}

	public void setPred(Predecessor pred) throws RemoteException {
		this.pred = pred;
	}

	public ChordNodeInfo getInfo() throws RemoteException {
		return info;
	}

	public void setInfo(ChordNodeInfo info) throws RemoteException {
		this.info = info;
	}

	public List<ChordNodeInfo> getFingerTable() throws RemoteException {
		return fingerTable;
	}

	public void setFingerTable(List<ChordNodeInfo> fingerTable) throws RemoteException {
		this.fingerTable = fingerTable;
	}

	public boolean isAlive() throws RemoteException {
		return alive;
	}

	public void setAlive(boolean alive) throws RemoteException {
		this.alive = alive;
	}

	public void infoMsgReceiver() throws RemoteException {
		MsgReceiver rec = new MsgReceiver(this.info);
		rec.start();
	}

	public void join(ChordNodeInfo newPeerInfo) throws RemoteException,
			NotBoundException {
		Registry newPeerReg = LocateRegistry.getRegistry(newPeerInfo
				.getIpAddr());
		ChordNodeInterface newPeer = (ChordNodeInterface) newPeerReg
				.lookup(newPeerInfo.getId()+"");
		System.out.println("New peer joined" + "\n" + newPeer.getInfo());
		ChordNodeInfo succTemp = newPeer.findSuccessor(this.getInfo().getId());
		Successor succ = new Successor(succTemp.getIpAddr(),
				succTemp.getPort(), succTemp.getId());
		System.out.println("Successor for "+this.getInfo()+"is:\n"+ succTemp);
		Predecessor pred = new Predecessor(null, 0, null);
		this.setSucc(succ);
		this.setPred(pred);
		this.setAlive(true);
		this.infoMsgReceiver();

	}

	public ChordNodeInfo findSuccessor(BigInteger newPeerId)
			throws RemoteException, NotBoundException {

		if (this.succ.getId() == this.info.getId()) { // only one node
			return new ChordNodeInfo(succ.getIpAddr(), succ.getPort(),
					succ.getId());
		} // when successor id is smaller than this node new node id must be
			// smaller than successor and larger than this node (twisted normal)
		else if (this.succ.getId().compareTo(this.info.getId()) == -1
				&& (newPeerId.compareTo(this.succ.getId()) == -1 || newPeerId
						.compareTo(this.info.getId()) == 1)) {
			return new ChordNodeInfo(succ.getIpAddr(), succ.getPort(),
					succ.getId());

		} else if (newPeerId.compareTo(this.info.getId()) == 1
				&& newPeerId.compareTo(this.succ.getId()) == -1) { // normal
																	// case
			return new ChordNodeInfo(succ.getIpAddr(), succ.getPort(),
					succ.getId());
		} else { // new node id is larger than successor
			ChordNodeInfo closPrecedPeerInfo = closestPrededingNode(newPeerId);
			Registry closPrecedPeerReg = LocateRegistry
					.getRegistry(closPrecedPeerInfo.getIpAddr());
			ChordNodeInterface closPrecedPeer = (ChordNodeInterface) closPrecedPeerReg
					.lookup(closPrecedPeerInfo.getId()+"");
			return closPrecedPeer.findSuccessor(newPeerId);
		}
	}

	private ChordNodeInfo closestPrededingNode(BigInteger newPeerId) throws RemoteException {

		for (int i = fingerTable.size() - 1; i >= 0; i--) {

			// fingerEntryID greater than this node
			if (fingerTable.get(i).getId().compareTo(this.info.getId()) == 1) {
				// normal case
				if (newPeerId.compareTo(fingerTable.get(i).getId()) == 1)
					return fingerTable.get(i);
				// case when new peer id goes behind or is lesser than this node
				// and it is lesser than the finger entry.
				else if (this.info.getId().compareTo(newPeerId) == 1
						&& newPeerId.compareTo(fingerTable.get(i).getId()) == -1) {
					return fingerTable.get(i);
				}
			}
			// fingerEntryID lesser than this node since it
			// wraps around the end of the ring
			else if (fingerTable.get(i).getId().compareTo(this.info.getId()) == -1) {
				// new peer id is lesser than this node entry and
				// greater than fingerEntryID .
				if (newPeerId.compareTo(this.info.getId()) == -1
						&& newPeerId.compareTo(fingerTable.get(i).getId()) == 1) {
					return fingerTable.get(i);
				}
			}
		}

		return new ChordNodeInfo(this.succ.getIpAddr(), this.succ.getPort(),
				this.succ.getId());
	}

	public void fixFingers() throws RemoteException, NotBoundException {
		System.out.println("Fingers being fixed");
		fingerTable.clear();
		BigInteger fingerEntryID;
		BigInteger two = new BigInteger(2 + "");
		BigInteger N = two.pow(160);
		for (int i = 1; i <= 160; i++) {
			fingerEntryID = this.info.getId().add(
					two.modPow(new BigInteger(i - 1 + ""), N));
			ChordNodeInfo succInfo = this.findSuccessor(fingerEntryID);
			if(fingerTable.isEmpty())
				fingerTable.add(succInfo);
			else {
				if (succInfo.getId().equals(this.info.getId())) {
					return;
				}
				for (ChordNodeInfo entry:fingerTable) {
					if(entry.getId().compareTo(succInfo.getId())==0){
						return;
					}
				}
				fingerTable.add(succInfo);
			}
		}
		System.out.println("Entire finger table:");
		System.out.println("Size of table " + fingerTable.size());
		for(int j = 0; j < fingerTable.size(); j++){
			System.out.println(fingerTable.get(j));
		}
	}
//			if (succInfo.getId().equals(this.info.getId())) {
//				return;
//			}
//			for (int j = 0; j < fingerTable.size(); j++) {
//				if (fingerTable.get(j).getId().equals(succInfo.getId())) {
//					return;
//				}
//			}
//			System.out.println("Finger Entry Added to the table = "+succInfo);
//			fingerTable.add(succInfo);
//			System.out.println("Entire finger table:");
//			System.out.println("Size of table " + fingerTable.size());
//			for(int j = 0; j < fingerTable.size(); j++){
//				System.out.println(fingerTable.get(j));
//			}
//		}

//	}

	public void stabilize() throws RemoteException, NotBoundException, FileNotFoundException {
		Registry succRegistry = LocateRegistry.getRegistry(this.succ
				.getIpAddr());
		ChordNodeInterface succChordNode = (ChordNodeInterface) succRegistry
				.lookup(this.succ.getId()+"");
		ChordNodeInfo x = new ChordNodeInfo(
				succChordNode.getPred().getIpAddr(), succChordNode.getPred()
						.getPort(), succChordNode.getPred().getId());
		// Only one node in ring
		if (this.info.getId().equals(this.succ.getId()) && x.getId() == null) {
			System.out.println("In Stabilize case 1");
			return;
		}
		// When another node enters the ring
		else if (!this.info.getId().equals(this.succ.getId())
				&& x.getId() == null) {
			System.out.println("In Stabilize case 2");
			succChordNode.notify(this.info);
		}
		// When node is its own successor but has a predecessor
		else if (succChordNode.getInfo().getId().equals(this.info.getId())
				&& x.getId() != null) {
			System.out.println("In Stabilize case 3 corrects its successor and corrects successors predecessor");
			this.setSucc(new Successor(x.getIpAddr(), x.getPort(), x.getId()));
			succRegistry = LocateRegistry.getRegistry(x.getIpAddr());
			ChordNodeInterface newSuccChordNode = (ChordNodeInterface) succRegistry
					.lookup(x.getId()+"");
			newSuccChordNode.notify(this.info);
			return;
		}
		// More than 2 nodes
		else {
			// Normal case when node gets added in between and also successor of
			// this node is larger than this node.
			if (x.getId().compareTo(this.info.getId()) == 1
					&& x.getId().compareTo(succChordNode.getInfo().getId()) == -1) {
				this.setSucc(new Successor(x.getIpAddr(), x.getPort(), x
						.getId()));
				System.out.println("In Stabilize case 4 normal node gets added in between successors predecessor is in between myself and my succ");
				succRegistry = LocateRegistry.getRegistry(x.getIpAddr());
				ChordNodeInterface newSuccChordNode = (ChordNodeInterface) succRegistry
						.lookup(x.getId()+"");
				newSuccChordNode.notify(this.info);
				return;
			}
			// Cases when predecessor of successor is not equal to this node and
			// successor is smaller than this node since it wraps around the
			// ring.
			else if (!x.getId().equals(this.info.getId())
					&& succChordNode.getInfo().getId().compareTo(this.info.getId()) == -1) {
				// Case when predecessor of successor has been set larger than
				// successor
				if (x.getId().compareTo(succChordNode.getInfo().getId()) == 1) {
					System.out.println("In Stabilize case 5 ");
					this.setSucc(new Successor(x.getIpAddr(), x.getPort(), x
							.getId()));
					succRegistry = LocateRegistry.getRegistry(x.getIpAddr());
					ChordNodeInterface newSuccChordNode = (ChordNodeInterface) succRegistry
							.lookup(x.getId()+"");
					newSuccChordNode.notify(this.info);
					return;
				}
				// Case when predecessor of successor has been set smaller than
				// successor
				else if (x.getId().compareTo(succChordNode.getInfo().getId()) == -1) {
					System.out.println("In Stabilize case 6");
					this.setSucc(new Successor(x.getIpAddr(), x.getPort(), x
							.getId()));
					succRegistry = LocateRegistry.getRegistry(x.getIpAddr());
					ChordNodeInterface newSuccChordNode = (ChordNodeInterface) succRegistry
							.lookup(x.getId()+"");
					newSuccChordNode.notify(this.info);
					return;
				}
			}

		}
		succChordNode.notify(this.info);

	}

	public void checkPredecessor() throws RemoteException {
		if (this.getPred().getId() == null) {
			return;
		}
		// Locate registry for predecessor if registry not available then
		// destroy predecessor for this node. It may be due to a failure
		try {
			rmiReg = LocateRegistry.getRegistry(this.getPred().getIpAddr());
		} catch (RemoteException e) {
			System.out.println("Predecessor not found");
			this.setPred(new Predecessor(null, 0, null));
			return;
		}
		// If predecessor is not found in the registry which maybe due to a
		// voluntary leave.
		try {
			ChordNodeInterface predChordNode = (ChordNodeInterface) rmiReg
					.lookup(this.getPred().getId()+"");
			if (predChordNode == null) {
				this.setPred(new Predecessor(null, 0, null));
			} else if (predChordNode.isAlive()) {
				return;
			}
		} catch (AccessException e) {
			this.setPred(new Predecessor(null, 0, null));
		} catch (RemoteException e) {
			this.setPred(new Predecessor(null, 0, null));
		} catch (NotBoundException e) {
			this.setPred(new Predecessor(null, 0, null));
		}

	}

	@Override
	public void notify(ChordNodeInfo actualNodeInfo) throws RemoteException,
			NotBoundException, FileNotFoundException {
		// When this node's successor's predecessor is null, set it and
		// distribute the keys.
		if (this.getPred().getId() == null) {
			this.setPred(new Predecessor(actualNodeInfo.getIpAddr(),
					actualNodeInfo.getPort(), actualNodeInfo.getId()));
			rmiReg = LocateRegistry.getRegistry(this.succ.getIpAddr());
			ChordNodeInterface succChordNode = (ChordNodeInterface) rmiReg
					.lookup(this.succ.getId()+"");
			succChordNode.distributeKeys();
		}
		// Usual case
		else if (this.info.getId().compareTo(actualNodeInfo.getId()) == 1
				&& actualNodeInfo.getId().compareTo(this.getPred().getId()) == 1) {
			this.setPred(new Predecessor(actualNodeInfo.getIpAddr(),
					actualNodeInfo.getPort(), actualNodeInfo.getId()));
		}
		// When actual nodes successor node is smaller than actual node's
		// successor's predecessor node id
		else if (this.info.getId().compareTo(this.getPred().getId()) == -1) {
			// For stabilize case 5
			if (actualNodeInfo.getId().compareTo(this.getPred().getId()) == 1) {
				this.setPred(new Predecessor(actualNodeInfo.getIpAddr(),
						actualNodeInfo.getPort(), actualNodeInfo.getId()));
			}
			// For stabilize case 6
			else if (actualNodeInfo.getId().compareTo(this.info.getId()) == -1) {
				this.setPred(new Predecessor(actualNodeInfo.getIpAddr(),
						actualNodeInfo.getPort(), actualNodeInfo.getId()));
			}
		}
	}
	
	public void leave() throws RemoteException, NotBoundException, FileNotFoundException{
		rmiReg = LocateRegistry.getRegistry(this.getPred().getIpAddr());
		ChordNodeInterface predChordNode = (ChordNodeInterface) rmiReg
				.lookup(this.getPred().getId()+"");
		predChordNode.setSucc(new Successor(this.getPred().getIpAddr(),this.getPred().getPort(),this.getPred().getId()));
		ChordNodeInterface succChordNode = (ChordNodeInterface) rmiReg
				.lookup(this.succ.getId()+"");
		succChordNode.setPred(new Predecessor(null,0,null));
		this.distributeKeys();
		this.setSucc(null);
		this.setPred(null);
		System.out.println("The peer " + this.getInfo() + " has left");
		System.exit(1);
	}
	@Override
	public void distributeKeys() throws FileNotFoundException, NotBoundException,RemoteException {
		int numOfbytes;
		String fileName;
		File folder = new File("ChordPartFiles/");
		byte[] partBytes = new byte[4096];
		File allPartFiles[] = folder.listFiles();
		if(!folder.exists()||allPartFiles.length==0){
			System.out.println("No files or folders");
			return;
		}
		else {
			System.out.println("Redistributing part files");
			for(int i=0;i<allPartFiles.length;i++){
				File partFile = allPartFiles[i];
				if(!partFile.getName().contains(".nfs")){
					BufferedInputStream bis = new BufferedInputStream(new FileInputStream(partFile));
					try {
						numOfbytes = bis.read(partBytes, 0, (int)partFile.length());
						fileName = partFile.getName();
						System.out.println(fileName);
						BigInteger partKey = new BigInteger(fileName,16);
						System.out.println(partKey+"" +" being distributed");
						partFile.delete();
						ChordNodeInfo peer = this.findSuccessor(partKey);
						Socket peerSoc = new Socket(peer.getIpAddr(),peer.getPort());
						DataOutputStream out = new DataOutputStream(peerSoc.getOutputStream());
						out.flush();
						out.writeUTF("PUT");
						out.flush();
						out.writeUTF(fileName);
						out.flush();
						out.write(partBytes, 0, numOfbytes);
						out.flush();
						peerSoc.close();
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
		}

	}

}
