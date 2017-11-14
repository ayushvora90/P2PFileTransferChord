import java.io.FileNotFoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Stabilize extends Thread {
	private ChordNode peer;

	public Stabilize(ChordNode peer) {
		this.peer = peer;
		// TODO Auto-generated constructor stub
	}

	public void run() {
		while (true) {
			try {
				peer.stabilize();
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (NotBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
