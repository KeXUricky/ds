package ds;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ssl.SSLSocket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ReceiveServerConnection extends Thread {
	private SSLSocket receiveSocket;
	private Server receiveServer;
	private BufferedReader reader;
	private BufferedWriter writer;
	private BlockingQueue<String> messageQueue;

	public BlockingQueue<String> getMessageQueue() {
		return messageQueue;
	}

	public ReceiveServerConnection() {
		receiveSocket = null;
	}
	
	public ReceiveServerConnection(SSLSocket receiveSocket, Server receiveServer) {
		try {
			this.receiveSocket = receiveSocket;
			this.receiveServer = receiveServer;
			reader = new BufferedReader(new InputStreamReader(receiveSocket.getInputStream(), "UTF-8"));
			writer = new BufferedWriter(new OutputStreamWriter(receiveSocket.getOutputStream(), "UTF-8"));
			messageQueue = new LinkedBlockingQueue<String>();
		}
		
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public void run() {
		try {
			String msg = null;
			while((msg = reader.readLine()) != null) {
				JSONParser parser = new JSONParser();
				JSONObject jsonMsg = (JSONObject) parser.parse(msg);
				String type = (String) jsonMsg.get("type");
				
				if (type.equals("lockidentity")){
					write(receiveServer.voteLockIdentity((String) jsonMsg.get("identity"), 
							(String) jsonMsg.get("serverid")).toJSONString());
				}
				
				else if (type.equals("releaseidentity")){
					receiveServer.releaseIdentity((String) jsonMsg.get("identity"), (String) jsonMsg.get("serverid"));
					break;
				}
				
				else if (type.equals("lockroomid")){
					write(receiveServer.voteLockRoomID((String) jsonMsg.get("roomid"), (String) jsonMsg.get("serverid")).toJSONString());
				}
				
				else if (type.equals("releaseroomid")){
					receiveServer.releaseRoom((String) jsonMsg.get("roomid"), (String) jsonMsg.get("serverid"));
					String approved = (String) jsonMsg.get("approved");
					if (approved.equals("true"))
						ServerState.getInstance().getRoomList().put((String) jsonMsg.get("roomid"), (String) jsonMsg.get("serverid"));
					break;
				}
				
				else if (type.equals("deleteroom")){
					ServerState.getInstance().getRoomList().remove((String) jsonMsg.get("roomid"));
					break;
				}
				
				else if(type.equals("heart")){
					System.out.println("Heartbeat signal received from "+(String) jsonMsg.get("serverId"));
					break;
				}
				
				else if(type.equals("newServer")){
					
					respond();
					
					String configServerID = (String) jsonMsg.get("serverID");
					String serverAddress = (String) jsonMsg.get("serverAddress");
					int clientPort = Integer.valueOf((String) jsonMsg.get("clientPort")) ;
					int coordinationPort = Integer.valueOf((String) jsonMsg.get("coordinationPort")) ;
					
					addServer(configServerID, serverAddress, clientPort, coordinationPort);
					
					new Server(configServerID, serverAddress, clientPort, coordinationPort);
					
					break;
				}
				else if (type.equals("addserver")){

					String configServerID = (String) jsonMsg.get("serverID");
					String serverAddress = (String) jsonMsg.get("serverAddress");
					int clientPort = Integer.valueOf((String) jsonMsg.get("serversClientPort")) ;
					int coordinationPort = Integer.valueOf((String) jsonMsg.get("serversCordinatPort")) ;
					new Server(configServerID, serverAddress, clientPort, coordinationPort);
					
					break;
				}
				
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
				JSONObject jso = new JSONObject();
				jso.put("type", "exit");
				write(jso.toJSONString());
				receiveSocket.close();
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void respond(){
			
		
		for (Server currentServer: ServerState.getInstance().getListOfServer()){
			JSONObject respond = new JSONObject();
			String currentServer1 = String.valueOf(currentServer.getClientPort());
			String CoordinationPort = String.valueOf(currentServer.getCoordinationPort());
			respond.put("type", "existServer");
			respond.put("serverID", currentServer.getServerID());
			respond.put("serversAddress", currentServer.getServerAddress());
			respond.put("serversClientPort",currentServer1 );
			respond.put("serversCordinatPort",CoordinationPort );
			String respondToNew = respond.toJSONString();
			write(respondToNew);
		}
		for (String currentchatroom: ServerState.getInstance().getRoomList().keySet()){
			JSONObject chatRoom = new JSONObject();
			chatRoom.put("type", "currentchatroom");
			chatRoom.put("roomid", currentchatroom);
			chatRoom.put("serverid",  ServerState.getInstance().getRoomList().get(currentchatroom));
		//	chatRoom.put("clientID", ServerState.getInstance().getRoomList().get(currentchatroom));
			String ccr = chatRoom.toJSONString();
			write(ccr);
		}
		
		JSONObject complete = new JSONObject();
		complete.put("type", "complete");
		write(complete.toJSONString());
		

	}
	@SuppressWarnings("unchecked")
	public void addServer(String serverid, String serveraddress, int clientport, int cordinationport){
		JSONObject addserver = new JSONObject();
		String clientport1 = String.valueOf(clientport);
		String cordinationport1 = String.valueOf(cordinationport);
		addserver.put("type", "addserver");
		addserver.put("serverID", serverid);
		addserver.put("serversAddress", serveraddress);
		addserver.put("serversClientPort", clientport1);
		addserver.put("serversCordinatPort", cordinationport1);		
		
		ArrayList<SendServerConnection> listOfSSC = receiveServer.connectOtherServer();
			for (SendServerConnection ssc: listOfSSC){
				ssc.write(addserver.toJSONString());
			}
	}
	
	
}
