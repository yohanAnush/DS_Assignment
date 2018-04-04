package fireSensorMonitor;

import fireAlarmServer.FireSensorData;

public interface IMessageQueueConsumer {

	public void registerAsListner();
	public void receiveSensorData();
}
