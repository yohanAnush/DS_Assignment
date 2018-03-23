package fireAlarmServer;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

/*
 * This class will handle everything related to notifying the,
 * monitors when data is relyed from the fire sensors.
 * To keep the communication async we will be using ActiveMQ(Apache),
 * with RMI.
 * Here the class doesn't act as a server itself but as a sender(producer) and,
 * connects to an instance of, ActiveMQ server which we need to run before 
 * running this class.
 */
public class FireDataSender {
	
	private ConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private Destination queue;
	private MessageProducer messageProducer;
	
	
	/*
	 * Initialize the connection and create the queue when the
	 * object is created.
	 */
	public FireDataSender() {
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
	 */
	public void sendDataToMonitors(FireSensorHelper fireSensorData) {
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

}
