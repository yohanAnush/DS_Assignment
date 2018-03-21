package fireSensor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

public class FireSensor {
	private static ObjectOutputStream sensorDataOutput;
	private static ObjectInputStream serverDataInput;
	private static PrintWriter sensorTextOutput;
	
	
	// TODO mimic the procedure of the sensor getting data by using a file.
	public static void main(String[] main) {
		String server = "localhost";
		
		try {
			Socket socket = new Socket(server, 9001);
			sensorDataOutput = new ObjectOutputStream(socket.getOutputStream());
			serverDataInput = new ObjectInputStream(socket.getInputStream());
			sensorTextOutput = new PrintWriter(socket.getOutputStream(), true);
			
			// send to the server
			// TODO Send to the server according to the specifications.
			while (true) {
				// add the parameters and their readings to the hashmap first.
				HashMap<String, String> sensorData = new HashMap<>();

				sensorData.put("temperature", "45.0");
				sensorData.put("battery", "70");
				sensorData.put("smoke", "1");
				sensorData.put("co2", "300.0");

				// let the server know data is ready to be read through its ObjectInputStream;
				sensorTextOutput.println("DATA:23-11");
				// send the data to the server
				sensorDataOutput.writeObject(sensorData);
			
				for (int i = 0; i < 50; i++) {
					System.out.print(" " + i);
				}
				
				// TODO Always reset so the next write object wont corrupt the stream!
				sensorDataOutput.reset();
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

}
