package fireAlarmServer;

public interface IMessageQueue {

	public void initMessageQueue();
	public void sendDataToMonitors(FireSensorData fireSensorData);
}
