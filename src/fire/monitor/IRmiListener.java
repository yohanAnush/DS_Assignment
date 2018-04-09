package fire.monitor;

import java.rmi.Remote;
import java.rmi.RemoteException;

import fire.alarm.server.FireAlarmServer;
import fire.alarm.server.FireSensorData;

public interface IRmiListener extends Remote {

	/* * IMPORTANT * */
	// Each method should throw RemoteException, otherwise "...imlements illgeal remote interface",
	// will be thrown.
	
	public FireAlarmServer getRemoteServer() throws RemoteException;
	public void onData(FireSensorData sensorData) throws RemoteException;
	public void onError(String error) throws RemoteException;
	
}
