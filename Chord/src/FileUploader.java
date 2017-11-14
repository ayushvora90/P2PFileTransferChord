import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUploader extends Thread {
	private String fileName;
	private ChordNode peerUploader;

	public FileUploader(String FName, ChordNode peer) {
		fileName = FName;
		peerUploader = peer;
	}

	public BigInteger performKeyHash(String fName, long partNo)
			throws NoSuchAlgorithmException {
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

	public void run() {

		File uploadFile = new File(fileName);

		try {
			int numOfbytes;
			long partNo = 1;
			FileInputStream fis = new FileInputStream(uploadFile);
			MessageDigest hashFingerPrint = MessageDigest.getInstance("SHA1");
			byte[] uploadBytes = new byte[4096];
			// Splitting and uploading
			while ((numOfbytes = fis.read(uploadBytes)) > 0) {
				BigInteger partFileId = performKeyHash(fileName, partNo);
				ChordNodeInfo peer = peerUploader.findSuccessor(partFileId);
				Socket peerSoc = new Socket(peer.getIpAddr(), peer.getPort());
				DataOutputStream out = new DataOutputStream(
						peerSoc.getOutputStream());
				out.flush();
				out.writeUTF("PUT");
				out.flush();
				out.writeUTF(partFileId + "");
				out.flush();
				out.write(uploadBytes, 0, numOfbytes);
				out.flush();
				peerSoc.close();
				out.close();
				hashFingerPrint.update(uploadBytes, 0, numOfbytes);
				partNo++;
			}

			byte[] hashFingerPrintBytes = hashFingerPrint.digest();
			String hash = "";
			for (int i = 0; i < hashFingerPrintBytes.length; i++) {
				// getting an unsigned integer and then converting to a hex
				// string.
				String tmp = Integer
						.toHexString(hashFingerPrintBytes[i] & 0xff);
				// appending leading zeros to single digit hex strings
				if (tmp.length() == 1) {
					tmp = "0" + tmp;
				}
				hash += tmp;
			}

			FileMetaData fmd = new FileMetaData(hash, partNo - 1);
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(bs);
			os.writeObject(fmd);
			byte[] metaDataBytes = bs.toByteArray();
			fis.read(metaDataBytes);
			BigInteger partFileId = performKeyHash(fileName, 0);
			ChordNodeInfo peer = peerUploader.findSuccessor(partFileId);
			Socket peerSoc = new Socket(peer.getIpAddr(), peer.getPort());
			DataOutputStream out = new DataOutputStream(
					peerSoc.getOutputStream());
			out.flush();
			out.writeUTF("PUT");
			out.flush();
			out.writeUTF(partFileId + "");
			out.flush();
			out.write(metaDataBytes, 0, metaDataBytes.length);
			out.flush();
			peerSoc.close();
			out.close();
			bs.close();
			os.close();
			fis.close();
			System.out.println("File Uploaded");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
