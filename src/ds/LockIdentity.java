package ds;

public class LockIdentity {
	private String lockIdentity;
	private String serverID;
	
	public LockIdentity() {
		lockIdentity = null;
		serverID = null;
	}
	
	public LockIdentity(String lockIdentity, String serverID){
		this.lockIdentity = lockIdentity;
		this.serverID = serverID;
	}

	public String getLockIdentity() {
		return lockIdentity;
	}

	public String getServerID() {
		return serverID;
	}
}
