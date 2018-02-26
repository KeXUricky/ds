package ds;

import java.util.ArrayList;

import org.json.simple.JSONObject;

public class TimerTaskTest extends java.util.TimerTask {
		
		public Server server;
		
		public TimerTaskTest(Server server)
		{
			this.server = server;
		}
		
		public void run(){
			//build connection
			ArrayList<SendServerConnection> listOfSSC = server.connectOtherServer();
			for (SendServerConnection ssc: listOfSSC){
				ssc.write(server.heartBeat().toJSONString());
				JSONObject jso = new JSONObject();
				jso.put("type", "exit");
				ssc.write(jso.toJSONString());
			}
		}
	}
