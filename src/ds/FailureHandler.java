package ds;

import java.util.Timer;

public class FailureHandler{
	
	// send signal to all other servers in 10s interval 
	Server server;
	Timer timer;
	
	public FailureHandler(Server server,Timer timer)
	{
		this.server = server;
		this.timer = timer;
		timer.schedule(new TimerTaskTest(server), 10000, 5000);
	}
	

}
