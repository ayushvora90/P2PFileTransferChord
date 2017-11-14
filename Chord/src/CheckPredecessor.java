import java.rmi.RemoteException;

public class CheckPredecessor extends Thread {
	private ChordNode peer;

	public CheckPredecessor(ChordNode peer) {
		this.peer = peer;
	}

	public void run() {
		while (true) {
			try {
				peer.checkPredecessor();
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
