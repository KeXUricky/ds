package ds;

import org.kohsuke.args4j.Option;

public class CmdLineArgs {

	@Option(required=true, name="-n", usage="ServerID")
	private String serverID;
	
	@Option(required=false, name="-c", usage="ClientPort")
	private int clientPort;
	
	@Option(required=false, name="-h", usage="Address")
	private String address;
	
	@Option(required=false, name="-d", usage="CoordiNationPort")
	private int coordinationPort;
	
	@Option(required=true, name="-m", usage="Main Server")
	private String mainServer;
	
	@Option(required=false, name="-mh", usage="Main Server Address")
	private String mainServerAddress;
	
	@Option(required=false, name="-md", usage="Main Server CoordiNationPort")
	private int mainServerCoordiNationPort;

	public String getServerID() {
		return serverID;
	}

	public int getClientPort() {
		return clientPort;
	}

	public String getAddress() {
		return address;
	}

	public int getCoordinationPort() {
		return coordinationPort;
	}

	public String getMainServer() {
		return mainServer;
	}

	public String getMainServerAddress() {
		return mainServerAddress;
	}

	public int getMainServerCoordiNationPort() {
		return mainServerCoordiNationPort;
	}
	
}
