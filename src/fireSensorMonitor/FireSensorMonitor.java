package fireSensorMonitor;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import fireAlarmServer.FireSensorData;

public class FireSensorMonitor implements MessageListener {

	// Message Queue properties.
	private ConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private Destination queue;
	private MessageConsumer messageConsumer;
	
	
	// Message Queue implementation.
	public FireSensorMonitor() {
		connectionFactory = null;
		connection = null;
		session = null;
		queue = null;
		messageConsumer = null;
	}
	
	/*
	* Initialize the connection and create the queue when the
	* object is created.
	*
	* (non-Javadoc)
	* @see fireSensorMonitor.IMessageQueueConsumer#initMessageQueue()
	*/
	public void registerAsListner() {
		try {
			// connect to ActiveQM server as a sender.
			connectionFactory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_BROKER_URL);
			connection = connectionFactory.createConnection();
			
			connection.start();
			
			// create a session for messaging receiving.
			// messages are not transacted => 1st arg is false.
			// session will auto acknowledge.
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
				
			// FireDataQueue is our messaging queue that's going to handle the delivery,
			// of data sent by the server to the monitors.
			// We will use this class on the server to send messages.
			queue = session.createQueue("FireDataQueue");
			messageConsumer = session.createConsumer(queue);
			
			messageConsumer.setMessageListener(this);
		}
		catch (JMSException jmse) {
			jmse.printStackTrace();
		}
	}
	
	
	/*
	 * Data is sent to the queue by the server as an ObjectMessage, hence the,
	 * receiver will get an ObjectMessage as well.
	 * 
	 * Following method acts as a callback function to be called when the message queue,
	 * gets a message to make the communication async.
	 */
	@Override
	public void onMessage(Message message) {
		ObjectMessage sensorData = (ObjectMessage)message;
		
		// get the parameters separately.
		// params:- sensorId, temperature, battery, smoke, co2.
		try {
			System.out.print(" Sensor: " + sensorData.getStringProperty("sensorId"));
			System.out.print(" Tempeature: " + sensorData.getDoubleProperty("temperature"));
			System.out.print(" Battery: " + sensorData.getIntProperty("battery"));
			System.out.print(" Smoke: " + sensorData.getIntProperty("smoke"));
			System.out.print(" CO2: " + sensorData.getDoubleProperty("co2"));
			System.out.println();
			
		} catch (JMSException jmse) {
			jmse.printStackTrace();
		}
	}
	
	/*
	 * Server sends data of the fire sensor to the message queue as an object message,
	 * hence we receive an object message from the queue as well.
	 * 
	 * (non-Javadoc)
	 * @see fireSensorMonitor.IMessageQueueConsumer#receiveSensorData()
	 */
	public void receiveSensorData( ) {
		if (session != null && queue != null && messageConsumer != null) {
			try {
				ObjectMessage sensorDataMsg = (ObjectMessage) messageConsumer.receive();
				
				System.out.println(sensorDataMsg);
					
			} catch (JMSException jmse) {
				jmse.printStackTrace();
			}
		}
	}
	
	
	public static void main(String[] args) {
		FireSensorMonitor monitor = new FireSensorMonitor();
		
		monitor.registerAsListner();
	}
	
}
