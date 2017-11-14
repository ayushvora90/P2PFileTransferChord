import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class FixFingers extends Thread {

	private ChordNode peer;

	public FixFingers(ChordNode peer) {
		this.peer = peer;
	}

	public void run() {
		while (true) {
			try {
				peer.fixFingers();
			} catch (RemoteException | NotBoundException e1) {
				e1.printStackTrace();
			}

			try {
				Thread.sleep(8000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
