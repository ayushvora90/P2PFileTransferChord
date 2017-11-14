import java.io.Serializable;


public class FileMetaData implements Serializable {
	String hashFingerPrint;
	long numOfParts;
	FileMetaData(String hash, long partNo){
		this.hashFingerPrint = hash;
		this.numOfParts = partNo;
	}
	public String getHashFingerPrint() {
		return hashFingerPrint;
	}
	public void setHashFingerPrint(String hashFingerPrint) {
		this.hashFingerPrint = hashFingerPrint;
	}
	public long getNumOfParts() {
		return numOfParts;
	}
	public void setNumOfParts(long numOfParts) {
		this.numOfParts = numOfParts;
	}
	

}
