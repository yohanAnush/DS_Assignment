//package fire.alarm.server;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.management.monitor.Monitor;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import fire.alarm.server.FireSensorData;
import fire.monitor.FireSensorMonitor;

/*
 *  this will handle two responsibilities.
 *  		1) communicate between the fire sensors and gain information.
 *  		2) inform the monitors about the current state of information gained in 1).
 *  
 *  A socket is used to communicate between the fire sensors.
 *  RMI is used to let the monitors know about the current state of information.
 *  
 */
public class FireAlarmServer implements ISocketConnection, IRmiServer, Runnable {
	
	// server config.
	private static final int PORT_TO_LISTEN = 9001;
	
	/*
	 *  Recording data given by each sensor.
	 *  
	 *  A sensor will send 4 parameters as follows;
	 *  		1) Temperature(celcius)
	 *  		2) Battery level (percentage)
	 *  		3) Smoke level (scale of 1 - 10)
	 *  		4) CO2 level (parts per million)
	 *  
	 *  Use a helper class to validate those parameters and check for dangerous values/levels.
	 */
	private static HashMap<String, FireSensorData> sensorAndData = new HashMap<>();
	
	// RMI properties.
	private static ArrayList<FireSensorMonitor> monitors = new ArrayList<>();
	private static final String rmiRegistrationAddress = "rmi://localhost/server";
	
	
	// Socket Connection properties.
	private Socket socket;
	private BufferedReader sensorTextInput;	// gives sort of a heads-up before sending the actual data via object output stream.
	private ObjectInputStream sensorDataInput;	// this will delivery a hash map where a key can be 1 of the 4 parameters.
											// and the value relevent to the parameter is the object assigned to the key.
											// both the key and the object/value are Strings (Parse as needed).
	
	// while we are not sending any data to the client,
	// we need this object initialized before the input stream,
	// in order for everything to work.
	// TODO initialize the ObjectOutputStream object before ObjectInpuStream.
	@SuppressWarnings("unused")
	private ObjectOutputStream serverDataOutput;
	
	// RMI implementation.
	/*
	 * We need to bind each server instance to the RMI registry so that,
	 * each instance has access to the methods provided by monitors and vice-versa.
	 * 
	 * (non-Javadoc)
	 * @see fire.alarm.server.IRmiService#bindToRegistry()
	 */
	public void bindToRegistry(FireAlarmServer serverInstance) {
		try {
			// generate stub.
			IRmiServer stub = (IRmiServer)UnicastRemoteObject.exportObject(serverInstance);
			
			// bind
			Registry registry = LocateRegistry.getRegistry();
			registry.bind("FireAlarmServer", stub);
		} catch (RemoteException | AlreadyBoundException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Here listeners are the monitors that observes the sensors' data.
	 * 
	 * (non-Javadoc)
	 * @see fire.alarm.server.IRmiService#addListner(javax.management.monitor.Monitor)
	 */
	public void addMonitor(FireSensorMonitor monitor) {
		monitors.add(monitor);
	}
	
	/*
	 * (non-Javadoc)
	 * @see fire.alarm.server.IRmiService#removeListner(javax.management.monitor.Monitor)
	 */
	public void removeMonitor(FireSensorMonitor monitor) {
		monitors.remove(monitor);
	}
	
	/*
	 * This method is responsible for notifying all the listening monitors if there's new data.
	 * 
	 * (non-Javadoc)
	 * @see fire.alarm.server.IRmiService#notifyMonitors(fire.alarm.server.FireSensorData)
	 */
	public void notifyMonitors(FireSensorData sensorData) {
		for(FireSensorMonitor monitor: monitors) {
			// TODO Call the async method on each monitor.
		}
	}
	
	/*
	 * Same as the above method but conveys error strings instead of data.
	 * Thereofore, the monitor's error handling method should be called!
	 * 
	 * (non-Javadoc)
	 * @see fire.alarm.server.IRmiService#notifyMonitors(java.lang.String)
	 */
	public void notifyMonitors(String error) {
		for(FireSensorMonitor monitor: monitors) {
			// TODO Call the async method on each monitor.
		}
	}
	
	
	// Socket Connection implementations.
	/*
	 * (non-Javadoc)
	 * @see fireAlarmServer.ISocketConnection#initSocketConnection(java.net.Socket)
	 */
	public void initSocketConnection(Socket serverSocket) {
		this.socket = serverSocket;
		try {
			this.serverDataOutput = new ObjectOutputStream(this.socket.getOutputStream());
			this.sensorTextInput =  new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			this.sensorDataInput = new ObjectInputStream(this.socket.getInputStream());
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	// must be initialized before input stream.
	}
	
	/*
	 * Reads the ObjectOutputStream of the connected client socket and returns if any data is read.
	 * TODO Above stream will always be read to check for data, thus EOFException will be thrown most of the time,
	 * 		(since data will be available every couple of minutes). Therefore we have to ignore the above exception.
	 * 
	 * (non-Javadoc)
	 * @see fireAlarmServer.ISocketConnection#readSocketData()
	 */
	public Object readSocketData() {
		Object data = null;
		try {
			data = this.sensorDataInput.readObject();
		} 
		catch (IOException  ioe) {
			// do not do a stack trace since most of the time the exception will be,
			// EOFException, since data will be available in fixed intervals of times.
		}
		catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
		
		return data;
	}

	/*
	 * Returns the server socket that is passed to the ServerInstance at the time of the creation of a,
	 * ServerInstance object. We need this server socket to initialize other parameters such as i/o streams,
	 * of the socket.
	 * 
	 * (non-Javadoc)
	 * @see fireAlarmServer.ISocketConnection#getServerSocket()
	 */
	public Socket getServerSocket() {
		return this.socket;
	}
	
	/*
	 * When a client exits, call this method and close its socket.
	 * 
	 * (non-Javadoc)
	 * @see fireAlarmServer.ISocketConnection#closeSocket()
	 */
	public void closeSocket() {
		try {
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
 	
	/*
	 * Execution:-
	 * Main method will listen to the port specified in PORT_TO_LISTEN and will
	 * assign a new thread to each unique sensor.
	 * Implementation of the thread aspects are below the main method.
	 */
	
	public static void main(String[] args) throws IOException {
		System.out.println("Fire Alarm Sensor is up and running");
		
		ServerSocket portListner = new ServerSocket(PORT_TO_LISTEN);
		
		try {
			// accept as requests come.
			while (true) {
				FireAlarmServer server = new FireAlarmServer(portListner.accept());
				Thread t = new Thread(server);
				t.start();
			}
		}
		finally {
			// server is shutting down.
			portListner.close();
		}
	}
	
	/*
	 * Add FireSensor's data to the hashmap that we maintain.
	 * Hashmap is keyed by the sensor's id and the data is paired with that key.
	 * sensor id is of type String and data is of type FireSensorData.
	 * 
	 * TODO Always synchronize and avoid duplicates.
	 */
	public void insertDataToServerHashMap(String sensorId, FireSensorData fireSensorData) {
		synchronized (sensorAndData) {
			// HashMap will automatically replace the value if the sensorId already exists.
			sensorAndData.put(sensorId, fireSensorData);
		}
		
	}
	
	
	/*
	 * Thread for each sensor.
	 * This way server can deal with all the sensors without mixing up or blocking communication.
	 * Each thread should be given the socket of each sensor.	
	 */
	
	/* * * Each ServerInstance is simple an unique instance of FireAlarmServer with a couple of data handling parameters. * * */
	private String sensorId;
	private FireSensorData fireSensorData;
	private long lastUpdate;	// using Time() we can get the difference easily.
	
	
	public FireAlarmServer(Socket sensorSocket) throws RemoteException {
		this.socket = sensorSocket;
	}
		
		
		
		/*
		 * run method of the Thread class.
		 * Listens to the sensor and accepts the hashmap sent.
		 * Parse the content of the hashmap as needed by the FireSensorHelper class,
		 * TODO And determine if the monitors should be notified or not.
		 * TODO Send alert to the sensors to turn the alarm on.
		 * 
		 * Monitors should be notified if the sensor does not report back after an hour.
		 */
		@SuppressWarnings("unchecked")
		public void run() {
			try {
				bindToRegistry(this);
				initSocketConnection(socket);
				
				HashMap<String, String> sensorDataAsHashMap;
				FireSensorData fsd;
				String sensorId = "Unassigned Sensor Id";
				FireDataSender sender = new FireDataSender();
				lastUpdate = System.currentTimeMillis();
				
				while (true) {
					// TODO Always get the text input and data input of the sensor into a,
					// 		local variable to avoid null pointers.
					
					// Monitors should be notified if the sensor's last update exceeds one hour.
					// 1 hour = 3.6e+6 millis. 
					if ((System.currentTimeMillis() - lastUpdate) > 	600) {
						notifyMonitors(sensorId + " has not reported in 1 hour.");
						
						// Don't remove the following code as it will result in a non-stop loop until data arrives.
						// Sending the warning once and then waiting another 1 hour will suffice.
						lastUpdate = System.currentTimeMillis();
					}
					
					if ( (sensorDataAsHashMap = (HashMap<String, String>) readSocketData()) != null) {
						fsd = new FireSensorData().getFireSensorDataFromHashMap(sensorDataAsHashMap);
						sensorId = fsd.getSensorId();
						fsd.printData();
							
						insertDataToServerHashMap(sensorId, fsd);
							
						// we need to notify the listeners about the new data.
						// always get the data from the hashmap instead of transmitting the local variable.
						notifyMonitors(sensorAndData.get(sensorId));
						
						// check for errors and send error messages.
						if (!fsd.isTemperatureInLevel()) {
							notifyMonitors(fsd.getTempErr());
						}
						if (!fsd.isBatteryInLevel()) {
							notifyMonitors(fsd.getBatteryErr());
						}
						if (!fsd.isSmokeInLevel()) {
							notifyMonitors(fsd.getSmokeErr());
						}
						if (!fsd.isCo2InLevel()) {
							notifyMonitors(fsd.getCo2Err());
						}
						
						// coming upto this points indicates that the sensor sent data,
						// hence we can set the last update to the current time.
						lastUpdate = System.currentTimeMillis();
					}
					
					
				}
			}	
			finally {
				// sensor disconnecting from the server.
				// therefore remove the sensor and its data.
				if (sensorId != null) {
					sensorAndData.remove(sensorId);
				}
				
				// close the connection.
				closeSocket();
			}
		}
	
}
