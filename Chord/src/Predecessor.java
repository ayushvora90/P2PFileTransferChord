import java.io.Serializable;
import java.math.BigInteger;


public class Predecessor implements Serializable{
	private String ipAddr;
	private int port;
	private BigInteger id;
	Predecessor(){
		ipAddr = null;
		port = 0;
		id = null;
	}
	Predecessor(String ip,int pt, BigInteger id){
		ipAddr = ip;
		port = pt;
		this.id = id;
	}
	public String getIpAddr() {
		return ipAddr;
	}
	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public BigInteger getId() {
		return id;
	}
	public void setId(BigInteger id) {
		this.id = id;
	}
	public String toString(){
		String info = "IP: "+ ipAddr+"\t"+"Port: " +port+ "\t" +"Identifier: " + id;
		return info;
	}

}
