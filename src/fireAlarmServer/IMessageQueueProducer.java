package fireAlarmServer;

public interface IMessageQueueProducer {

	public void initMessageQueue();
	public void sendToMonitors(FireSensorData fireSensorData);	// we don't need to pass the sensorId here since the fireSensorData obj already has it inside it.
	public void sendToMonitors(String messageStr);
}
