package ds;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
/*import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.Scanner;
import java.util.StringTokenizer;*/
import java.util.Timer;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;


public class Service {

	public static void main(String[] args) {
		System.setProperty("javax.net.ssl.keyStore","mykeystore");
		
		System.setProperty("javax.net.ssl.keyStorePassword","123456");
		
		System.setProperty("javax.net.ssl.trustStore", "mykeystore");

		//System.setProperty("javax.net.debug","all");
		
		CmdLineArgs argsBean = new CmdLineArgs();
		
		CmdLineParser parser = new CmdLineParser(argsBean);
		
		try {
			parser.parseArgument(args);
			String serverId = argsBean.getServerID();

			String serverAddress = argsBean.getAddress();
			int clientPort = argsBean.getClientPort();
			int coordinationPort = argsBean.getCoordinationPort();
			
			String whetherMain = argsBean.getMainServer();
			
			String mainServerAddress = argsBean.getMainServerAddress();
			int mainServerCordinationPort = argsBean.getMainServerCoordiNationPort();
			
			Timer timer = new Timer();
			
			/*String filePath = argsBean.getFilePath();
			Scanner inputStream = null;
			inputStream = new Scanner (new FileInputStream(new File(filePath)));
			while (inputStream.hasNextLine()){
				String command = inputStream.nextLine();
				StringTokenizer st = new StringTokenizer(command, "\t");
				
				String configServerID = st.nextToken();
				serverAddress = st.nextToken();
				clientPort = Integer.parseInt(st.nextToken());
				coordinationPort = Integer.parseInt(st.nextToken());
				Server configServer = new Server(configServerID, serverAddress, clientPort, coordinationPort);
				
				if (configServerID.equals(serverId)){
					server = configServer;
					FailureHandler fh = new FailureHandler(server,timer);//add
				}
			}
			server.setup();
			
			inputStream.close();*/
			
			if (whetherMain.equals("other")){

				Server localServer = new Server(serverId, serverAddress, clientPort, coordinationPort);
				
				
				SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
				SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(mainServerAddress, mainServerCordinationPort);
				
				SendServerConnection send = new SendServerConnection(sslsocket);
				send.start();
				String ns = localServer.newServer(serverId, serverAddress, clientPort, coordinationPort).toJSONString();			
				send.write(ns);
				while (true){
					String jsmsg = send.getMessageQueue().take();
					
					JSONParser changeJSON = new JSONParser();
					JSONObject jsonMsg = (JSONObject) changeJSON.parse(jsmsg);
					String type = (String) jsonMsg.get("type");
					if (type.equals("complete")){
						break;
					}
					else if(type.equals("existServer")){
						String tempid = (String) jsonMsg.get("serverID");
						String tempad = (String) jsonMsg.get("serversAddress");
						int tempcp = Integer.valueOf((String) jsonMsg.get("serversClientPort")) ;
						int temocor = Integer.valueOf((String) jsonMsg.get("serversCordinatPort")) ;
						Server configServer = new Server(tempid, tempad, tempcp, temocor);
					}
					else if(type.equals("currentchatroom")){
						String temRoomid =  (String) jsonMsg.get("roomid");
						String tempServerid = (String) jsonMsg.get("serverid");
						ServerState.getInstance().getRoomList().put(temRoomid, tempServerid);
					}
				}	
				FailureHandler fh = new FailureHandler(localServer,timer);
				localServer.setup(); 
			}
			else if (whetherMain.equals("main")){
				Server mainServer = new Server(serverId, serverAddress, clientPort, coordinationPort);
				FailureHandler fh = new FailureHandler(mainServer,timer);
				mainServer.setup();
			}
			
			
		}
		
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		catch (CmdLineException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
