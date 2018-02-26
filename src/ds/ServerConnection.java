package ds;

import java.io.IOException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

public class ServerConnection extends Thread {
	private SSLServerSocket coordinationSocket;
	private Server server;
	
	public ServerConnection() {
		coordinationSocket = null;
		server = null;
	}
	
	public ServerConnection(SSLServerSocket coordinationSocket, Server server) {
		this.coordinationSocket = coordinationSocket;
		this.server = server;
	}
	
	public void run() {
		try {
			while (true){
				SSLSocket receiveServerSocket = (SSLSocket) coordinationSocket.accept();
				ReceiveServerConnection rsc = new ReceiveServerConnection(receiveServerSocket, server);
				rsc.start();
			}
		}
		
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
