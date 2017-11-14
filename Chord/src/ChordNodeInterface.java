import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.rmi.*;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.List;


public interface ChordNodeInterface extends Remote{
	public BigInteger performNodeHash(String ipAddr, int port) throws NoSuchAlgorithmException,RemoteException;
	public ChordNodeInfo sendInfoMsg() throws IOException, ClassNotFoundException,RemoteException;
	public Successor getSucc()throws RemoteException;
	public void setSucc(Successor succ)throws RemoteException;
	public Predecessor getPred()throws RemoteException;
	public void setPred(Predecessor pred)throws RemoteException;
	public ChordNodeInfo getInfo()throws RemoteException;
	public void setInfo(ChordNodeInfo info)throws RemoteException;
	public List<ChordNodeInfo> getFingerTable()throws RemoteException;
	public void setFingerTable(List<ChordNodeInfo> fingerTable)throws RemoteException;
	public boolean isAlive()throws RemoteException;
	public void setAlive(boolean alive)throws RemoteException;
	public void infoMsgReceiver()throws RemoteException;
	public void join(ChordNodeInfo newPeerInfo) throws RemoteException, NotBoundException;
	public ChordNodeInfo findSuccessor(BigInteger newPeerId) throws RemoteException,NotBoundException;
	public void notify(ChordNodeInfo info) throws RemoteException, NotBoundException, FileNotFoundException;	
	public void distributeKeys() throws FileNotFoundException, NotBoundException, RemoteException;
	public void checkPredecessor()throws RemoteException;
	public void stabilize() throws RemoteException, NotBoundException, FileNotFoundException;
	public void leave() throws RemoteException, NotBoundException, FileNotFoundException;
	public void fixFingers() throws RemoteException, NotBoundException;
	
	
}
