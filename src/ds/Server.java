package ds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Server {
	
	private ArrayList<ChatRoom> localRoom;
	private String serverID;
	private HashMap <String, ClientConnection> currentClient;
	private String serverAddress;
	private int clientPort, coordinationPort;
	private HashSet <LockIdentity> lockClientID;
	//private ServerSocket coordinationSocket = null;
	private ChatRoom mainHall;
	private HashSet <LockRoomID> lockRoomID;
	/*private ArrayList<SendServerConnection> listOfSSC;
	private ArrayList<ReceiveServerConnection> listOfRSC;*/
	
	Server() {
		serverID = null;
	}
	
	Server(String serverID, String serverAddress, int clientPort, int coordinationPort) {
		this.serverID = serverID;
		this.serverAddress = serverAddress;
		this.clientPort = clientPort;
		this.coordinationPort = coordinationPort;
		localRoom = new ArrayList<ChatRoom> ();
		currentClient = new HashMap <String, ClientConnection> ();
		lockClientID = new HashSet <LockIdentity> ();
		lockRoomID = new HashSet <LockRoomID> ();
		
		mainHall = new ChatRoom("MainHall-" + serverID, this, "");
		localRoom.add(mainHall);
		ServerState.getInstance().getRoomList().put(mainHall.getRoomID(), serverID);
		ServerState.getInstance().getListOfServer().add(this);
		
		/*ServerSocket coordinationSocket = null;
		try{
			coordinationSocket = new ServerSocket(coordinationPort, 0, InetAddress.getByName(serverAddress));
			ServerConnection serverConnection = new ServerConnection (coordinationSocket, this);
			serverConnection.start();
		}
		
		catch (Exception e) {
			e.printStackTrace();
		}*/
		
		//setup();
		
	}
	
	public String getServerID() {
		return serverID;
	}
	
	public String getServerAddress() {
		return serverAddress;
	}

	public int getClientPort() {
		return clientPort;
	}
	
	public int getCoordinationPort() {
		return coordinationPort;
	}

	public HashMap<String, ClientConnection> getCurrentClient() {
		return currentClient;
	}

	public HashSet <LockIdentity> getLockClientID() {
		return lockClientID;
	}
	
	public ChatRoom getMainHall() {
		return mainHall;
	}

	public HashSet<LockRoomID> getLockRoomID() {
		return lockRoomID;
	}

	public void setup() {
		SSLServerSocket coordinationSocket = null;
		
		try{
			SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory
					.getDefault();
			SSLServerSocket sslserversocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(coordinationPort,
					0, InetAddress.getByName(serverAddress));
			
			coordinationSocket = sslserversocket;
			ServerConnection serverConnection = new ServerConnection (coordinationSocket, this);
			serverConnection.start();
		}
		
		catch (Exception e) {
			e.printStackTrace();
		}
		
		
		SSLServerSocket listeningSocket = null;
		try{
			SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory
					.getDefault();
			SSLServerSocket ssllistenserversocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(
					clientPort, 0, InetAddress.getByName(serverAddress));
			
			listeningSocket = ssllistenserversocket;
			//int clientNum = 0;
			
			while (true) {
				SSLSocket clientSocket = (SSLSocket) listeningSocket.accept();
				//clientNum++;
				ClientConnection clientConnection = new ClientConnection(clientSocket, this);
				//clientConnection.setName("Thread" + clientNum);
				clientConnection.start();
			}
		}
		
		catch (Exception e) {
			e.printStackTrace();
		}
		
		finally {
			if(listeningSocket != null) {
				try {
					listeningSocket.close();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public boolean localNew(String newID){
		for (String client : currentClient.keySet()){
			if (client.equals(newID))
				return false;
		}
		for (LockIdentity lockClient: lockClientID){
			if (lockClient.getLockIdentity().equals(newID))
				return false;
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject lockIdentity(String lockID){
		JSONObject lock = new JSONObject();
		lock.put("type", "lockidentity");
		lock.put("serverid", serverID);
		lock.put("identity", lockID);
		return lock;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized JSONObject voteLockIdentity(String lockID, String requestServerID){
		JSONObject vote = new JSONObject();
		vote.put("type", "lockidentity");
		vote.put("serverid", serverID);
		vote.put("identity", lockID);
		if (localNew(lockID)) {
			lockClientID.add(new LockIdentity(lockID, requestServerID));
			vote.put("locked", "true");
		}
		else {
			vote.put("locked", "false");
		}
		return vote;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject newIdentity(String newID, boolean approved){
		JSONObject newIdentity = new JSONObject();
		newIdentity.put("type", "newidentity");
		if (approved) {
			newIdentity.put("approved", "true");
		}
		else {
			newIdentity.put("approved", "false");
		}
		return newIdentity;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject releaseIdentity(String lockID){
		JSONObject releaseIdentity = new JSONObject();
		releaseIdentity.put("type", "releaseidentity");
		releaseIdentity.put("serverid", serverID);
		releaseIdentity.put("identity", lockID);
		return releaseIdentity;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject list(){
		JSONObject list = new JSONObject();
		list.put("type", "roomlist");
		JSONArray rooms = new JSONArray();
		for (String roomID : ServerState.getInstance().getRoomList().keySet())
			rooms.add(roomID);
		list.put("rooms", rooms);
		return list;
	}
	
	
	
	public void addIdentity(String newID, ClientConnection clientConnection){
		currentClient.put(newID, clientConnection);
	}
	
	public void releaseIdentity (String lockID, String lockServerID){
		LockIdentity removeIdentity = null;
		for (LockIdentity lock : lockClientID){
			if (lock.getLockIdentity().equals(lockID) && lock.getServerID().equals(lockServerID))
				removeIdentity = lock;
		}
		lockClientID.remove(removeIdentity);
	}
	
	//function for lockRoom
	@SuppressWarnings("unchecked")
	public JSONObject lockRoomID(String roomID){
		JSONObject lockRoomID = new JSONObject();
		lockRoomID.put("type", "lockroomid");
		lockRoomID.put("serverid", serverID);
		lockRoomID.put("roomid", roomID);
		return lockRoomID;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized JSONObject voteLockRoomID(String roomID, String lockServerID){
		JSONObject voteLockRoomID = new JSONObject();
		voteLockRoomID.put("type", "lockroomid");
		voteLockRoomID.put("serverid", serverID);
		voteLockRoomID.put("roomid", roomID);
		boolean lock = true;
		for (LockRoomID lockRoom : lockRoomID){
			if (lockRoom.getLockRoomID().equals(roomID))
				lock = false;
		}
		if (lock){
			voteLockRoomID.put("locked", "true");
			lockRoomID.add(new LockRoomID(roomID, lockServerID));
		}
		else {
			voteLockRoomID.put("locked", "false");
		}
		return voteLockRoomID;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject releaseRoomID(String roomID, boolean release){
		JSONObject releaseRoomID = new JSONObject();
		releaseRoomID.put("type", "releaseroomid");
		releaseRoomID.put("serverid", serverID);
		releaseRoomID.put("roomid", roomID);
		if (release)
			releaseRoomID.put("approved", "true");
		else
			releaseRoomID.put("approved", "false");
		return releaseRoomID;
	}
	
	public void releaseRoom(String roomID, String serverID){
		LockRoomID remove = null;
		for (LockRoomID lockRoom : lockRoomID){
			if (lockRoom.getLockRoomID().equals(roomID) && lockRoom.getServerID().equals(serverID))
				remove = lockRoom;
		}
		lockRoomID.remove(remove);
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject createRoom(String roomID, boolean approved){
		JSONObject createRoom = new JSONObject();
		createRoom.put("type", "createroom");
		createRoom.put("roomid", roomID);
		if (approved)
			createRoom.put("approved", "true");
		else
			createRoom.put("approved", "false");
		return createRoom;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject heartBeat(){
		JSONObject heartbeat = new JSONObject();
		heartbeat.put("type", "heart");
		heartbeat.put("serverId", serverID);
		return heartbeat;
	}
	
	public ArrayList<SendServerConnection> connectOtherServer() {
		ArrayList<SendServerConnection> listOfSSC = new ArrayList<SendServerConnection> ();
		boolean flag = false;	//added
		Server crashedServer = null;
		for (Server otherServer: ServerState.getInstance().getListOfServer()) {
			if (this != otherServer){
				try {
					SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
					SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(InetAddress.getByName(otherServer.getServerAddress()), 
							otherServer.getCoordinationPort());
					
					SendServerConnection ssc = new SendServerConnection(sslsocket);
					listOfSSC.add(ssc);
					ssc.start();
				} 
				
				catch (UnknownHostException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					System.out.println(otherServer.serverID+" no respond");//Added
					crashedServer = otherServer;	
					flag = true;
				}
			}
		}
		
		if(flag)
		{
			ServerState.getInstance().getListOfServer().remove(crashedServer);	//added
			ArrayList<String> removeList = new ArrayList<>();
			for(String key:ServerState.getInstance().getRoomList().keySet())
			{
				if(ServerState.getInstance().getRoomList().get(key).equals(crashedServer.serverID))
					removeList.add(key);
					
			}
			for(String key:removeList)
			{
				ServerState.getInstance().getRoomList().remove(key);
			}
		}
		
		return listOfSSC;
	}
	
	/*for (Server otherServer: ServerState.getInstance().getListOfServer()) {
		if (this != otherServer) {
			Socket receiveServerSocket = coordinationSocket.accept();
			ReceiveServerConnection rsc = new ReceiveServerConnection(receiveServerSocket, otherServer, this);
			listOfRSC.add(rsc);
			rsc.start();
		}
	}*/
	
	public void addLocalRoom(ChatRoom chatRoom){
		localRoom.add(chatRoom);
	}
	
	public boolean isLocalRoom(String roomID){
		for(ChatRoom chatRoom : localRoom){
			if (chatRoom.getRoomID().equals(roomID))
				return true;
		}
		return false;
	}
	
	public ChatRoom findLocalRoom(String roomID){
		ChatRoom finded = null;
		for(ChatRoom chatRoom : localRoom){
			if (chatRoom.getRoomID().equals(roomID))
				finded = chatRoom;
		}
		return finded;
	}
	
	public void removeClient(String id){
		currentClient.remove(id);
	}
	
	public void deleteLocalRoom(ChatRoom chatRoom){
		localRoom.remove(chatRoom);
		ServerState.getInstance().getRoomList().remove(chatRoom.getRoomID());
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject newServer(String serverID, String serverAddress, int clientPort, int coordinationPort){
		JSONObject ns = new JSONObject();
		String clientPort1 = String.valueOf(clientPort);
		String coordinationPort1 = String.valueOf(coordinationPort);
		ns.put("type", "newServer");
		ns.put("serverID", serverID);
		ns.put("serverAddress", serverAddress);
		ns.put("clientPort", clientPort1);
		ns.put("coordinationPort", coordinationPort1);
		return ns;
	}
	
}
