import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class FileRequestManager extends Thread{
	int port;

	public FileRequestManager(int port) {
		this.port = port;
	}
	public void run(){
		ServerSocket fileServerSoc = null;
		try {
			fileServerSoc = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("Coudnt connect to port");
			System.exit(10);
		}
		while(true){
			try {
				Socket responseSoc = fileServerSoc.accept();
				String fileToDownload;
				String fileToUpload;
				byte[] dataBlock = new byte[4096];
				DataOutputStream out = new DataOutputStream(responseSoc.getOutputStream());
				DataInputStream in = new DataInputStream(responseSoc.getInputStream());
				String request = in.readUTF();
				if(request.equals("PUT")){
					fileToUpload = in.readUTF();
					File newFolder = new File("ChordPartFiles/");
//					System.out.println("Creating a new folder");
					if(!newFolder.exists()){
						System.out.println("Folder created");
						newFolder.mkdir();
					}
//					else {
//						System.out.println("Not created");
//					}
					File uploadFile = new File("ChordPartFiles/"+fileToUpload);
					if(uploadFile.exists()){
						uploadFile.delete();
					}
					uploadFile.createNewFile();
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(uploadFile));
					int numOfbytes;
					while((numOfbytes = in.read(dataBlock))>0){
						bos.write(dataBlock, 0, numOfbytes);
					}
					responseSoc.close();
					out.close();
					in.close();
				} else if(request.equals("GET")){
					fileToDownload = in.readUTF();
					File newFolder = new File("ChordPartFiles/");
					if(!newFolder.exists()){
						newFolder.mkdir();
					}
					File downloadFile = new File("ChordPartFiles/"+fileToDownload);
					if(!downloadFile.exists()){
						out.flush();
						out.writeUTF("File not found!");
						out.flush();
						in.close();
						out.close();
						responseSoc.close();
						return;
					}
					out.flush();
					out.writeUTF("File found!");
					out.flush();
					BufferedInputStream bis = new BufferedInputStream(new FileInputStream(downloadFile));
					int numOfbytes;
					while((numOfbytes = bis.read(dataBlock))>0){
						out.flush();
						out.write(dataBlock, 0, numOfbytes);
						out.flush();
					}
					in.close();
					out.close();
					responseSoc.close();
					bis.close();
				}
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
		
		
	}

}
