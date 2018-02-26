package ds;

import java.util.ArrayList;
import java.util.HashMap;

public class ServerState {	
	private static ServerState instance;
	private ArrayList<Server> listOfServer;
	private HashMap<String, String> roomList;	
	private ServerState() {
		listOfServer = new ArrayList<>();
		roomList = new HashMap<String, String> ();
	}	
	public static synchronized ServerState getInstance() {
		if(instance == null) {
			instance = new ServerState();
		}
		return instance;
	}	
	public synchronized ArrayList<Server> getListOfServer() {
		return listOfServer;
	}

	public synchronized HashMap<String, String> getRoomList() {
		return roomList;
	}

}
