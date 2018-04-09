package fire.alarm.server;

import java.rmi.Remote;
import javax.management.monitor.Monitor;
import fire.monitor.FireSensorMonitor;
import fire.alarm.server.FireAlarmServer;

public interface IRmiServer extends Remote{

	//public void bindToRegistry(ServerInstance serverInstance);
	public void addMonitor(FireSensorMonitor monitor);
	public void removeMonitor(FireSensorMonitor monitor);
	public void notifyMonitors(FireSensorData fireSensorData);	// we don't need to pass the sensorId here since the fireSensorData obj already has it inside it.
	public void notifyMonitors(String error);
}
