package ds;

public class LockRoomID {
	private String lockRoomID;
	private String serverID;
	
	public LockRoomID() {
		lockRoomID = null;
		serverID = null;
	}
	
	public LockRoomID(String lockRoomID, String serverID){
		this.lockRoomID = lockRoomID;
		this.serverID = serverID;
	}

	public String getLockRoomID() {
		return lockRoomID;
	}

	public String getServerID() {
		return serverID;
	}
}
