package fireAlarmServer;

public interface IMessageQueueProducer {

	public void initMessageQueue();
	public void sendDataToMonitors(FireSensorData fireSensorData);
}
