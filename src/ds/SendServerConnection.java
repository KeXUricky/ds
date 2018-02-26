package ds;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ssl.SSLSocket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SendServerConnection extends Thread {
	private SSLSocket sendSocket;
	/*private Server sendServer;
	private Server receiveServer;*/
	private BufferedReader reader;
	private BufferedWriter writer;
	private BlockingQueue<String> messageQueue;

	public BlockingQueue<String> getMessageQueue() {
		return messageQueue;
	}

	public SendServerConnection() {
		sendSocket = null;
	}
	
	public SendServerConnection(SSLSocket sendSocket) {
		try {
			this.sendSocket = sendSocket;
			/*this.sendServer = sendServer;
			this.receiveServer = receiveServer;*/
			reader = new BufferedReader(new InputStreamReader(sendSocket.getInputStream(), "UTF-8"));
			writer = new BufferedWriter(new OutputStreamWriter(sendSocket.getOutputStream(), "UTF-8"));
			messageQueue = new LinkedBlockingQueue<String>();
		}
		
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void run() {
		try {
			String msg = null;
			while ((msg = reader.readLine()) != null) {
				JSONParser parser = new JSONParser();
				JSONObject jsonMsg = (JSONObject) parser.parse(msg);
				String type = (String) jsonMsg.get("type");
				if (type.equals("exit")){
					break;
				}
				messageQueue.add(msg);
				
			}
			
		}
		
		catch (IOException e) {
			e.printStackTrace();
		} 
		catch (ParseException e) {
			e.printStackTrace();
		}
		
		finally {
			try {
				sendSocket.close();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	public void write(String msg) {
		try {
			writer.write(msg + "\n");
			writer.flush();
		} 
		
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
