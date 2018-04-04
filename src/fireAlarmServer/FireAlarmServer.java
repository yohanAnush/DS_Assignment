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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

/*
 *  this will handle two responsibilities.
 *  		1) communicate between the fire sensors and gain information.
 *  		2) inform the monitors about the current state of information gained in 1).
 *  
 *  A socket is used to communicate between the fire sensors.
 *  RMI is used to let the monitors know about the current state of information.
 *  
 */
public class FireAlarmServer implements ISocketConnection, IMessageQueueProducer {
	
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
	
	// Message Queue properties.
	private ConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private Destination queue;
	private MessageProducer messageProducer;
	
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
	
	// Message Queue implementation.
	/*
	 * Initialize the connection and create the queue when the
	 * object is created.
	 *
	 * (non-Javadoc)
	 * @see fireAlarmServer.IMessageQueue#initMessageQueue()
	 */
	public void initMessageQueue() {
		try {
			// connect to ActiveQM server as a sender.
			connectionFactory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_BROKER_URL);
			connection = connectionFactory.createConnection();
			
			// without casting we won't be able to access the setUseAsyncSend method.
			((ActiveMQConnection)connection).setUseAsyncSend(true);
			connection.start();
			
			// create a session for messaging sending.
			// messages are not transacted => 1st arg is false.
			// session will auto acknowledge.
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			
			// FireDataQueue is our messaging queue that's going to handle the delivery,
			// of data sent by the server to the monitors.
			// We will use this class on the server to send messages.
			queue = session.createQueue("FireDataQueue");
			messageProducer = session.createProducer(queue);
		}
		catch (JMSException jmse) {
			jmse.printStackTrace();
		}
	}
	
	/*
	 * To send data from the server to monitors, use the following method.
	 * We have 4 parameters to send which are ints and doubles.
	 * TODO Check if you can send ints and doubles instead of sending a string that,
	 * 		has all the values.
	 * 		=> Use ObjectMessage instead of TextMessage.
	 *
	 * (non-Javadoc)
	 * @see fireAlarmServer.IMessageQueue#sendDataToMonitors(fireAlarmServer.FireSensorData)
	 */
	public void sendDataToMonitors(FireSensorData fireSensorData) {
		// make sure a message queue exists in order to receive our data.
		if (session != null && queue != null && messageProducer != null) {
			try {
				ObjectMessage dataToSend = session.createObjectMessage();
				
				// write the fire sensor's data.
				// make sure sensor id is sent as well to identify which sensor is sending what.
				dataToSend.setStringProperty("sensorId", fireSensorData.getSensorId());
				dataToSend.setDoubleProperty("temperature", fireSensorData.getTemperature());
				dataToSend.setIntProperty("battery", fireSensorData.getBatteryPercentage());
				dataToSend.setIntProperty("smoke", fireSensorData.getSmokeLevel());
				dataToSend.setDoubleProperty("co2", fireSensorData.getCo2Level());
		
				// send the data to the queue.
				messageProducer.send(dataToSend);
				
				// indicate that the data were sent.
				System.out.println("Data from " + fireSensorData.getSensorId() + " sensor to the queue.");
				
			} catch (JMSException jmse) {
				jmse.printStackTrace();
			}
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
				new ServerInstance(portListner.accept()).start();
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
	
	private static class ServerInstance extends Thread {
		private FireAlarmServer server;
		private String sensorId;
		private FireSensorData fireSensorData;
		
		/*
		 * Constructor takes the socket of the sensor so the thread can communicate.
		 */
		public ServerInstance(Socket sensorSocket) {
			server = new FireAlarmServer();
			server.socket = sensorSocket;
		}
		
		
		
		/*
		 * run method of the Thread class.
		 * Listens to the sensor and accepts the hashmap sent.
		 * Parse the content of the hashmap as needed by the FireSensorHelper class,
		 * TODO And determine if the monitors should be notified or not.
		 * TODO Send alert to the sensors to turn the alarm on.
		 */
		@SuppressWarnings("unchecked")
		public void run() {
			try {
				server.initMessageQueue();
				server.initSocketConnection(server.socket);
				/* 
				 * read if there's any data.
				 * when data is present, client will notify with the following notation.
				 * 		DATA:<sensor_id>
				 * Example: DATA:22-13
				 */
				HashMap<String, String> sensorDataAsHashMap;
				FireSensorData fsd;
				String sensorId;
				FireDataSender sender = new FireDataSender();
				
				while (true) {
					// TODO Always get the text input and data input of the sensor into a,
					// 		local variable to avoid null pointers.
					if ( (sensorDataAsHashMap = (HashMap<String, String>) server.readSocketData()) != null) {
						fsd = new FireSensorData().getFireSensorDataFromHashMap(sensorDataAsHashMap);
						sensorId = fsd.getSensorId();
							
						server.insertDataToServerHashMap(sensorId, fsd);
							
						// we need to notify the listners about the new data.
						// always get the data from the hashmap instead of transmitting the local variable.
						server.sendDataToMonitors(sensorAndData.get(sensorId));
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
				server.closeSocket();
			}
		}
	}
	
}
