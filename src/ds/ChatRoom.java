package ds;

import java.util.ArrayList;

public class ChatRoom {
	
	private String roomID;
	private Server server;
	private ArrayList<ClientConnection> roomClient;
	private String owner;
	
	ChatRoom() {
		roomID = null;
		server = null;
		roomClient = null;
		owner = null;
	}
	
	ChatRoom(String roomID, Server server, String owner) {
		this.roomID = roomID;
		this.server = server;
		roomClient = new ArrayList<ClientConnection> ();
		this.owner = owner;
	}

	public String getRoomID() {
		return roomID;
	}

	public Server getServer() {
		return server;
	}

	public ArrayList<ClientConnection> getRoomClient() {
		return roomClient;
	}
	
	public String getOwner() {
		return owner;
	}

	public void addClient(ClientConnection clientConnection){
		roomClient.add(clientConnection);
	}
	
	public void removeClient (ClientConnection clientConnection){
		roomClient.remove(clientConnection);
	}
	
	public void delete(){
		roomID = null;
		server = null;
		roomClient = null;
		owner = null;
	}
}
