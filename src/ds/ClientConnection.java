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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ClientConnection extends Thread {
	
	private SSLSocket clientSocket;
	private Server connectServer;
	private BufferedReader reader;
	private BufferedWriter writer;
	private BlockingQueue<Message> messageQueue;
	//private int clientNum;
	private String clientID;
	private ChatRoom currentRoom;

	public ClientConnection(){
		this.connectServer = null;
		this.clientSocket = null;
		reader = null;
		writer = null;
		messageQueue = null;
		clientID = "";
	}
	
	public ClientConnection(SSLSocket clientSocket, Server connectServer) {
		try {
			this.connectServer = connectServer;
			this.clientSocket = clientSocket;
			reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
			writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
			messageQueue = new LinkedBlockingQueue<Message>();
			//this.clientNum = clientNum;
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}	
	public String getClientID() {
		return clientID;
	}
	
	public BlockingQueue<Message> getMessageQueue() {
		return messageQueue;
	}
	
	public void setCurrentRoom(ChatRoom currentRoom) {
		this.currentRoom = currentRoom;
	}

	public void run() {
		try {
			ClientMessageReader messageReader = new ClientMessageReader(reader, messageQueue);
			//messageReader.setName(this.getName() + "Reader");
			messageReader.start();
			
			/*System.out.println(Thread.currentThread().getName() 
					+ " - Processing client " + clientNum + "  messages");*/
			
			while(true) {
				
				Message msg = messageQueue.take();
				
				if(!msg.isFromClient() && msg.getMessage().equals("exit")) {
					quit();
					break;
				}
				
				if(msg.isFromClient()) {
					
					JSONParser parser = new JSONParser();
					JSONObject jsonMsg = (JSONObject) parser.parse(msg.getMessage());
					String type = (String) jsonMsg.get("type");
					
					
					if (type.equals("newidentity")){
						String newID = (String) jsonMsg.get("identity");
						
						String username = (String) jsonMsg.get("username");
						String password = (String) jsonMsg.get("password");
						
						Object obj = new Object();
						synchronized(obj){
							if (validUser(username, password) && connectServer.localNew(newID) && properID(newID)){
								connectServer.getLockClientID().add(new LockIdentity(newID, connectServer.getServerID()));
								
								ArrayList<SendServerConnection> listOfSSC = connectServer.connectOtherServer();
								for (SendServerConnection ssc: listOfSSC){
									ssc.write(connectServer.lockIdentity(newID).toJSONString());
								}								
								boolean locked = true;
								for (SendServerConnection ssc: listOfSSC) {
									String sscMsg = ssc.getMessageQueue().take();
									JSONObject jsonRSCMsg = (JSONObject) parser.parse(sscMsg);
									String sscLocked = (String) jsonRSCMsg.get("locked");
	
									if (sscLocked.equals("true"))
										locked = locked && true;
									else
										locked = locked && false;
								}								
								if (locked){
									write(connectServer.newIdentity(newID, locked).toJSONString());
									connectServer.addIdentity(newID, this);
									connectServer.getMainHall().addClient(this);
									clientID = newID;
									currentRoom = connectServer.getMainHall();
									
									connectServer.releaseIdentity(newID, connectServer.getServerID());
									
									for (SendServerConnection ssc: listOfSSC){
										ssc.write(connectServer.releaseIdentity(newID).toJSONString());
									}
									
									String broadCastMsg = roomChange(newID, "", connectServer.getMainHall().getRoomID()).toJSONString();
									broadCast(broadCastMsg, connectServer.getMainHall().getRoomClient());
								}
								else {
									write(connectServer.newIdentity(newID, locked).toJSONString());
									
									connectServer.releaseIdentity(newID, connectServer.getServerID());
									
									for (SendServerConnection ssc: listOfSSC){
										ssc.write(connectServer.releaseIdentity(newID).toJSONString());
									}
									
									break;
								}
								
							}
							
							else if(!validUser(username, password)){
								write(invalid().toJSONString());
								break;
							}
							
							else{
								write(connectServer.newIdentity(newID, false).toJSONString());
								break;
							}
						
						}
						
					}
					
					else if(type.equals("list")){
						write(connectServer.list().toJSONString());
					}
					
					else if(type.equals("who")){
						write(who().toJSONString());
					}
					
					else if(type.equals("createroom")){
						String roomID = (String) jsonMsg.get("roomid");
						Object obj = new Object();
						synchronized(obj){
							if((!clientID.equals(currentRoom.getOwner())) && nonexistRoomID(roomID) && properID(roomID)){
								
								LockRoomID newLockRoom = new LockRoomID(roomID, connectServer.getServerID());
								connectServer.getLockRoomID().add(newLockRoom);
								
								ArrayList<SendServerConnection> listOfSSC = connectServer.connectOtherServer();
								for (SendServerConnection ssc: listOfSSC){
									ssc.write(connectServer.lockRoomID(roomID).toJSONString());
								}
								
								boolean approved = true;
								for (SendServerConnection ssc: listOfSSC) {
									String sscMsg = ssc.getMessageQueue().take();
									JSONObject jsonRSCMsg = (JSONObject) parser.parse(sscMsg);
									String sscLocked = (String) jsonRSCMsg.get("locked");
									if (sscLocked.equals("true"))
										approved = approved && true;
									else
										approved = approved && false;
								}
								if(approved){
									ChatRoom newRoom = new ChatRoom(roomID, connectServer, clientID);
									connectServer.addLocalRoom(newRoom);
									ServerState.getInstance().getRoomList().put(roomID, connectServer.getServerID());
									
									for (SendServerConnection ssc: listOfSSC){
										ssc.write(connectServer.releaseRoomID(roomID, approved).toJSONString());
									}
									
									write(connectServer.createRoom(roomID, approved).toJSONString());
									
									String broadCast = roomChange(clientID, currentRoom.getRoomID(), roomID).toJSONString();
									broadCast(broadCast, currentRoom.getRoomClient());
									
									currentRoom.removeClient(this);
									newRoom.addClient(this);
									currentRoom = newRoom;
									
								}
								else{
									for (SendServerConnection ssc: listOfSSC){
										ssc.write(connectServer.releaseRoomID(roomID, approved).toJSONString());
									}
									
									write(connectServer.createRoom(roomID, approved).toJSONString());
								}
								
								connectServer.getLockRoomID().remove(newLockRoom);
								
							}
							else
								write(connectServer.createRoom(roomID, false).toJSONString());
						}
					}
					
					else if (type.equals("join")){
						String roomID = (String) jsonMsg.get("roomid");
						if (nonexistRoomID(roomID) || clientID.equals(currentRoom.getOwner()) || roomID.equals(currentRoom.getRoomID())){
							String roomChange = roomChange(clientID, currentRoom.getRoomID(), currentRoom.getRoomID()).toJSONString();
							write(roomChange);
						}
						else if (connectServer.isLocalRoom(roomID)){
							String roomChange = roomChange(clientID, currentRoom.getRoomID(), roomID).toJSONString();
							ChatRoom changeRoom = connectServer.findLocalRoom(roomID);
							currentRoom.removeClient(this);
							broadCast(roomChange, currentRoom.getRoomClient());
							changeRoom.addClient(this);
							broadCast(roomChange, changeRoom.getRoomClient());
							currentRoom = changeRoom;
						}
						else{
							Server changeServer = findServer(roomID);
							String host = changeServer.getServerAddress();
							String port = String.valueOf(changeServer.getClientPort());
							write(route(roomID, host, port).toJSONString());
							String roomChange = roomChange(clientID, currentRoom.getRoomID(), roomID).toJSONString();
							currentRoom.removeClient(this);
							broadCast(roomChange, currentRoom.getRoomClient());
							currentRoom = new ChatRoom("", new Server(), "");
							
							connectServer.removeClient(clientID);
						}
					}
					
					else if (type.equals("movejoin")){
						String former = (String) jsonMsg.get("former");
						String roomID = (String) jsonMsg.get("roomid");
						String identity = (String) jsonMsg.get("identity");
						clientID = identity;
						connectServer.addIdentity(identity, this);
						/*Server formerServer = idBelongWhichServer(identity);
						if (formerServer != null)
							formerServer.removeClient(identity);*/
						if (connectServer.isLocalRoom(roomID)){
							currentRoom = connectServer.findLocalRoom(roomID);
							currentRoom.addClient(this);
						}
						else{
							connectServer.getMainHall().addClient(this);
							currentRoom = connectServer.getMainHall();
						}
						String roomChange = roomChange(clientID, former, currentRoom.getRoomID()).toJSONString();
						broadCast(roomChange, currentRoom.getRoomClient());
						write(serverChange().toJSONString());
					}
					
					else if (type.equals("deleteroom")){
						String roomID = (String) jsonMsg.get("roomid");
						if (clientID.equals(currentRoom.getOwner()) && roomID.equals(currentRoom.getRoomID())){
							deleteRoom(roomID);
							write(deleteRoom(roomID, true).toJSONString());
							
							ArrayList<SendServerConnection> listOfSSC = connectServer.connectOtherServer();
							for (SendServerConnection ssc: listOfSSC){
								ssc.write(deleteRoomToServer(roomID).toJSONString());
							}
						}
						else{
							write(deleteRoom(roomID, false).toJSONString());
						}
					}
					
					else if (type.equals("message")){
						String broadCast = jsonMessage(jsonMsg).toJSONString();
						broadCast(broadCast, currentRoom.getRoomClient());
					}
					
					else if (type.equals("quit")){
						String roomChange = roomChange(clientID, currentRoom.getRoomID(), "").toJSONString();
						quit();
						write(roomChange);
						break;
					}
					
				}
				
				else {
					write(msg.getMessage());
				}
				
			}
		}
		
		catch (Exception e) {
			quit();
			e.printStackTrace();
		}
		
		finally {
			try {
				clientSocket.close();
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
			//System.out.println(Thread.currentThread().getName() + " - Message sent to client " + clientNum);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void broadCast(String msg, ArrayList<ClientConnection> connectedClient){
		Message msgForThreads = new Message(false, msg);
		for(ClientConnection client : connectedClient)
			client.getMessageQueue().add(msgForThreads);
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject roomChange(String ID, String former, String roomid){
		JSONObject roomChange = new JSONObject();
		roomChange.put("type", "roomchange");
		roomChange.put("identity", ID);
		roomChange.put("former", former);
		roomChange.put("roomid", roomid);
		return roomChange;
	}
	
	public boolean properID(String identity){
		if (identity.length() > 16 || identity.length() < 3)
			return false;
		
		char initial = identity.charAt(0);
		if (!((initial >= 'a' && initial <= 'z') || (initial >= 'A' && initial <= 'Z')))
			return false;
		else
			return true;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject who(){
		JSONObject who = new JSONObject();
		who.put("type", "roomcontents");
		who.put("roomid", currentRoom.getRoomID());
		JSONArray identities = new JSONArray();
		for (ClientConnection currentClient : currentRoom.getRoomClient())
			identities.add(currentClient.getClientID());
		who.put("identities", identities);
		who.put("owner", currentRoom.getOwner());
		return who;
	}
	
	public boolean nonexistRoomID (String roomID){
		for (String room : ServerState.getInstance().getRoomList().keySet()){
			if (room.equals(roomID))
				return false;
		}
		return true;
	}
	
	public Server findServer(String roomID){
		String serverID = null;
		Server finded = null;
		for (String room : ServerState.getInstance().getRoomList().keySet()){
			if (room.equals(roomID))
				serverID = ServerState.getInstance().getRoomList().get(room);
		}
		for (Server server : ServerState.getInstance().getListOfServer()){
			if (server.getServerID().equals(serverID))
				finded = server;
		}
		return finded;
	}
	
	/*public Server idBelongWhichServer(String id){
		Server finded = null;
		for (Server server : ServerState.getInstance().getListOfServer()){
			if (server != connectServer){
				for (String client : server.getCurrentClient().keySet()){
					if (client.equals(id))
						finded = server;
				}
			}
		}
		return finded;
	}*/
	
	@SuppressWarnings("unchecked")
	public JSONObject route(String roomID, String host, String port){
		JSONObject route = new JSONObject();
		route.put("type", "route");
		route.put("roomid", roomID);
		route.put("host", host);
		route.put("port", port);
		return route;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject serverChange(){
		JSONObject serverChange = new JSONObject();
		serverChange.put("type", "serverchange");
		serverChange.put("approved", "true");
		serverChange.put("serverid", connectServer.getServerID());
		return serverChange;
	}
	
	public void deleteRoom(String roomID){
		if (clientID.equals(currentRoom.getOwner()) && roomID.equals(currentRoom.getRoomID())){
			for (ClientConnection client : currentRoom.getRoomClient()){
				String broadCast = roomChange(client.getClientID(), currentRoom.getRoomID(), 
						connectServer.getMainHall().getRoomID()).toJSONString();
				broadCast(broadCast, currentRoom.getRoomClient());
				broadCast(broadCast, connectServer.getMainHall().getRoomClient());
			}
			for (ClientConnection client : currentRoom.getRoomClient()){
				if (client != this){
					client.setCurrentRoom(connectServer.getMainHall());
					connectServer.getMainHall().addClient(client);
				}
			}
			//write(deleteRoom(roomID, true).toJSONString());
			connectServer.deleteLocalRoom(currentRoom);
			currentRoom.delete();
			currentRoom = connectServer.getMainHall();
			connectServer.getMainHall().addClient(this);
		}
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject deleteRoom(String roomID, boolean approved){
		JSONObject deleteRoom = new JSONObject();
		deleteRoom.put("type", "deleteroom");
		deleteRoom.put("roomid", roomID);
		if (approved)
			deleteRoom.put("approved", "true");
		else
			deleteRoom.put("approved", "false");
		return deleteRoom;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject deleteRoomToServer(String roomID){
		JSONObject deleteRoom = new JSONObject();
		deleteRoom.put("type", "deleteroom");
		deleteRoom.put("roomid", roomID);
		deleteRoom.put("serverid", connectServer.getServerID());
		return deleteRoom;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject jsonMessage(JSONObject jsonMsg){
		jsonMsg.put("identity", clientID);
		return jsonMsg;
	}
	
	public void quit(){
		connectServer.removeClient(clientID);
		currentRoom.getRoomClient().remove(this);
		String roomChange = roomChange(clientID, currentRoom.getRoomID(), "").toJSONString();
		broadCast(roomChange, currentRoom.getRoomClient());
		if (clientID.equals(currentRoom.getOwner())){
			for (ClientConnection client : currentRoom.getRoomClient()){
				String broadCast = roomChange(client.getClientID(), currentRoom.getRoomID(), 
						connectServer.getMainHall().getRoomID()).toJSONString();
				broadCast(broadCast, currentRoom.getRoomClient());
				broadCast(broadCast, connectServer.getMainHall().getRoomClient());
			}
			for (ClientConnection client : currentRoom.getRoomClient()){
				client.setCurrentRoom(connectServer.getMainHall());
				connectServer.getMainHall().addClient(client);
			}
			
			ArrayList<SendServerConnection> listOfSSC = connectServer.connectOtherServer();
			for (SendServerConnection ssc: listOfSSC){
				ssc.write(deleteRoomToServer(currentRoom.getRoomID()).toJSONString());
			}
			
			connectServer.deleteLocalRoom(currentRoom);
			currentRoom.delete();
		}
		//write(roomChange);
	}
	
	public boolean validUser(String username, String password){
		for (String usernameSet : PredifinedUser.getInstance().getUserList().keySet()){
			if (username.equals(usernameSet)){
				if (password.equals(PredifinedUser.getInstance().getUserList().get(username)))
					return true;
			}
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject invalid(){
		JSONObject invalid = new JSONObject();
		invalid.put("type", "invaliduser");
		return invalid;
	}
	
}
