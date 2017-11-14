import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class FileDownloader extends Thread {
	private String fileName;
	private ChordNode peerDownloader;
	public FileDownloader(String FName, ChordNode peer) {
		fileName = FName;
		peerDownloader = peer;
	}
	public BigInteger performKeyHash(String fName, long partNo) throws NoSuchAlgorithmException{
		String combKeyName = fName + partNo;
		MessageDigest partKeyDigest = MessageDigest.getInstance("SHA1");
		byte keyNameBytes[] = combKeyName.getBytes();
		byte hashVal[] = partKeyDigest.digest(keyNameBytes);
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
	public void reconstructFile(String fName, long numOfParts, String hashFingerPrint) throws NoSuchAlgorithmException{
		int numOfbytes;
		File newFolder = new File("OutputPartFiles/");
		if(!newFolder.exists()){
			newFolder.mkdir();
		}
		File mainFile = new File("OutputPartFiles/"+fName);
		if(mainFile.exists()){
			mainFile.delete();
		}
		try {
			mainFile.createNewFile();
			byte[] partFileBytes;
			//To write at the end of the file
			FileOutputStream fos = new FileOutputStream(mainFile, true);
			FileInputStream fis;
			for(long i = 1;i<=numOfParts;i++){
				File partFile = new File("OutputPartFiles/"+performKeyHash(fName, i));
				fis = new FileInputStream(partFile);
				partFileBytes = new byte[(int) partFile.length()];
				numOfbytes = fis.read(partFileBytes, 0,(int) partFileBytes.length);
				fos.flush();
				fos.write(partFileBytes);
				fos.flush();
				fis.close();
				partFile.delete();
			}
			mainFile = new File("OutputPartFiles/"+fName);
			fis = new FileInputStream(mainFile);
			byte[] partCheckBytes = new byte[4096];
			MessageDigest hashFingerPrintCheck = MessageDigest.getInstance("SHA1");
			while((numOfbytes= fis.read(partCheckBytes))>0){
				hashFingerPrintCheck.update(partCheckBytes, 0, numOfbytes);
			}
			fis.close();
			byte hashFingerprintBytes[] = hashFingerPrintCheck.digest();
			String hash = "";
			for (int i = 0; i < hashFingerprintBytes.length; i++) {
				// getting an unsigned integer and then converting to a hex string.
				String tmp = Integer.toHexString(hashFingerprintBytes[i] & 0xff);
				// appending leading zeros to single digit hex strings
				if (tmp.length() == 1) {
					tmp = "0" + tmp;
				}
				hash += tmp;
			}
			if(hash.equals(hashFingerPrint)){
				System.out.println("Integrity Check passed");
			} else {
				System.out.println("Integrity Check failed");
			}
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public void run(){
		int numOfbytes;
		byte[] partBytes = new byte[4096];
		try {
			BigInteger metaDataId = performKeyHash(fileName,0);
			ChordNodeInfo metaDataNode = peerDownloader.findSuccessor(metaDataId);
			Socket metaDataSoc = new Socket(metaDataNode.getIpAddr(),metaDataNode.getPort());
			DataInputStream in = new DataInputStream(metaDataSoc.getInputStream());
			DataOutputStream out = new DataOutputStream(metaDataSoc.getOutputStream());
			out.flush();
			out.writeUTF("GET");
			out.flush();
			out.writeUTF(metaDataId+"");
			out.flush();
			String reply = in.readUTF();
			if(reply.equals("File not found!")){
				System.out.println(reply);
				metaDataSoc.close();
				in.close();
				out.close();
				return;
			}
			numOfbytes = in.read(partBytes);
			ByteArrayInputStream bi = new ByteArrayInputStream(partBytes, 0, numOfbytes);
			ObjectInputStream oi = new ObjectInputStream(bi);
			FileMetaData fmd = (FileMetaData) oi.readObject();
			for(long i=1;i<=fmd.numOfParts;i++){
				BigInteger partFileId = performKeyHash(fileName,i);
				ChordNodeInfo peer = peerDownloader.findSuccessor(partFileId);
				Socket peerSoc = new Socket(peer.getIpAddr(),peer.getPort());
				out = new DataOutputStream(peerSoc.getOutputStream());
				in = new DataInputStream(peerSoc.getInputStream());
				out.flush();
				out.writeUTF("GET");
				out.flush();
				out.writeUTF(partFileId+"");
				out.flush();
				reply = in.readUTF();
				if(reply.equals("File not found!")){
					System.out.println(reply);
					peerSoc.close();
					in.close();
					out.close();
					return;
				}
				File newFolder = new File("OutputPartFiles/");
				if(!newFolder.exists()){
					newFolder.mkdir();
				}
				File downloadFile = new File("OutputPartFiles/"+partFileId);
				if(downloadFile.exists()){
					downloadFile.delete();
				}
				downloadFile.createNewFile();
				FileOutputStream fos = new FileOutputStream(downloadFile);
				while((numOfbytes = in.read(partBytes))>0){
					fos.write(partBytes, 0, numOfbytes);
					fos.flush();
				}
				metaDataSoc.close();
				peerSoc.close();
				out.close();
				in.close();
				fos.close();
				
			}
			reconstructFile(fileName, fmd.getNumOfParts(), fmd.getHashFingerPrint());
			
			
			
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
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
