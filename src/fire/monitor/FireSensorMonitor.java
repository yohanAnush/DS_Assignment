package fire.monitor;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import fire.alarm.server.FireAlarmServer;
import fire.alarm.server.FireSensorData;


public class FireSensorMonitor extends UnicastRemoteObject implements IRmiListener {

	// RMI Listner properties.
	private static final String rmiRegistrationTarget = "//localhost/server";	// this is the same url as the server's reg address but without rmi: protocol part.

	
	// Message Queue implementation.
	/*
	 * We need to implement the constructor in a way it can handle the RemoteException.
	 */
	public FireSensorMonitor() throws RemoteException {}
	/*
	 * To execute methods provided by the server, we can use it as a remote service,
	 * and invoke methods via RMI.
	 * 
	 * (non-Javadoc)
	 * @see fire.monitor.IRmiListner#getRemoteServer()
	 */
	public FireAlarmServer getRemoteServer() {
		FireAlarmServer server = null;
		try {
			Remote remoteService = Naming.lookup(rmiRegistrationTarget);
			server = (FireAlarmServer)remoteService;
		}
		catch (MalformedURLException | RemoteException | NotBoundException e) {
			e.printStackTrace();
		}
		
		return server;
	}
	
	/*
	 * Acts as an async method call.
	 * 
	 * (non-Javadoc)
	 * @see fire.monitor.IRmiListener#onData(fire.alarm.server.FireSensorData)
	 */
	public void onData(FireSensorData sensorData) throws RemoteException{
		sensorData.printData();
	}

	/*
	 * (non-Javadoc)
	 * @see fire.monitor.IRmiListener#onError(java.lang.String)
	 */
	public void onError(String error) throws RemoteException {
		System.err.println(error);
	}
	
	public static void main(String[] args) throws RemoteException {
		Registry registry = LocateRegistry.getRegistry("localhost");
		try {
			FireAlarmServer stub = (FireAlarmServer)registry.lookup("FireAlarmServer");
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//FireAlarmServer server = monitor.getRemoteServer();
		
		// subscribing to the server's listeners list.
		//server.addMonitor(monitor);
	}
	
}
