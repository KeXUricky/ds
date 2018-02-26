package ds;

import java.util.HashMap;

public class PredifinedUser {
	private static PredifinedUser instance;
	private HashMap<String, String> userList;
	
	private PredifinedUser() {
		userList = new HashMap<String, String> ();
		userList.put("wcc123", "123wcc");
		userList.put("lhc123", "123lhc");
		userList.put("xk123", "123xk");
		userList.put("peter123", "123peter");
	}
	
	public HashMap<String, String> getUserList() {
		return userList;
	}

	public static PredifinedUser getInstance() {
		if(instance == null) {
			instance = new PredifinedUser();
		}
		return instance;
	}
}
