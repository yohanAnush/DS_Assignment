package fireAlarmServer;

// TODO comment the package name when compiling in a terminal/command prompt using RMIC compiler
// 		since it will give a class not found error while searching for the package when compiling.


import fireAlarmServer.FireSensorData;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/*
 *  this will handle two responsibilities.
 *  		1) communicate between the fire sensors and gain information.
 *  		2) inform the monitors about the current state of information gained in 1).
 *  
 *  A socket is used to communicate between the fire sensors.
 *  RMI is used to let the monitors know about the current state of information.
 *  
 */
public class FireAlarmServer {
	
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
				new SensorHandler(portListner.accept()).start();
			}
		}
		finally {
			// server is shutting down.
			portListner.close();
		}
	}
	
	
	/*
	 * Thread for each sensor.
	 * This way server can deal with all the sensors without mixing up or blocking communication.
	 * Each thread should be given the socket of each sensor.	
	 */
	
	private static class SensorHandler extends Thread {
		private String sensorId;
		private FireSensorData fireSensorData;
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
									
		
		/*
		 * Constructor takes the socket of the sensor so the thread can communicate.
		 */
		public SensorHandler(Socket sensorSocket) {
			this.socket = sensorSocket;
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
		 * run method of the Thread class.
		 * Listens to the sensor and accepts the hashmap sent.
		 * Parse the content of the hashmap as needed by the FireSensorHelper class,
		 * TODO And determine if the monitors should be notified or not.
		 * TODO Send alert to the sensors to turn the alarm on.
		 */
		public void run() {
			try {
				serverDataOutput = new ObjectOutputStream(socket.getOutputStream());	// must be initialized before input stream.
				sensorTextInput =  new BufferedReader(new InputStreamReader(socket.getInputStream()));
				sensorDataInput = new ObjectInputStream(socket.getInputStream());
				
				
				/* 
				 * read if there's any data.
				 * when data is present, client will notify with the following notation.
				 * 		DATA:<sensor_id>
				 * Example: DATA:22-13
				 */
				HashMap<String, String> sensorDataAsHashMap;
				FireSensorData fsd;
				String sensorId;
				
				while (true) {
					// TODO Always get the text input and data input of the sensor into a,
					// 		local variable to avoid null pointers.
					try {
						if ( (sensorDataAsHashMap = (HashMap<String, String>) sensorDataInput.readObject()) != null) {
							fsd = new FireSensorData().getFireSensorDataFromHashMap(sensorDataAsHashMap);
							sensorId = fsd.getSensorId();
							
							insertDataToServerHashMap(sensorId, fsd);
							
							// we need to notify the listners about the new data.
							// always get the data from the hashmap instead of transmitting the local variable.
							FireDataSender sender = new FireDataSender();
							sender.sendDataToMonitors(sensorAndData.get(sensorId));
						}
					} catch (ClassNotFoundException | EOFException e) {
						// when data is not available, EOFException will be thrown but we, 
						// simply ignore it since there won't be data all the time and we want,
						// the loop to be running so it can read data whenever it's available.
						continue;
					}
				}
			}
			catch (IOException ioe){
				ioe.printStackTrace();
			}
			finally {
				// sensor disconnecting from the server.
				// therefore remove the sensor and its data.
				if (sensorId != null) {
					sensorAndData.remove(sensorId);
				}
				
				// close the connection.
				try {
					socket.close();
				}
				catch (IOException e){
					e.printStackTrace();
				}
			}
		}
	}
	
}
