package fireAlarmServer;

import java.io.IOException;
import java.io.Serializable;

/*
 * This class will act as a helper class to the server to manage sensor data.
 * A sensor will send 4 parameters as follows;
 *  		1) Temperature(celcius)
 *  		2) Battery level (percentage)
 *  		3) Smoke level (scale of 1 - 10)
 *  		4) CO2 level (parts per million)
 *  
 *  The above parameters should be stored, and validated for their correctness and checked for,
 *  dangerous levels/values and notify the server right away as well.
 */
public class FireSensorHelper {
	
	private String sensorId;
	private double temperature;
	private int batteryPercentage;
	private int smokeLevel;
	private double co2Level;
	
	
	// Getters.
	public String getSensorId() {
		return sensorId;
	}
	
	public double getTemperature() {
		return temperature;
	}
	
	public double getBatteryPercentage() {
		return batteryPercentage;
	}
	
	public int getSmokeLevel() {
		return smokeLevel;
	}
	
	public double getCo2Level() {
		return co2Level;
	}
	
	
	
	/* 
	 * Setters.
	 * A separate method should be used to determine such value is deemed dangerous as specified, or invalid.
	 */
	
	public void setSensorId(String sensorId) {
		this.sensorId = sensorId;
	}
	
	public void setTemperature(double temperature) {
		this.temperature = temperature;	
	}
	
	public void setBatteryPercentage(int batteryPercentage) {
		this.batteryPercentage = batteryPercentage;
	}
	
	public void setSmokeLevel(int smokeLevel) {
		this.smokeLevel = smokeLevel;
	}
	
	public void setCo2Level(double co2Level) {
		this.co2Level = co2Level;
	}
	
	
	/* 
	 * Validators.
	 * The 4 parameters will be checked for their validity and whether they indicate any sort of danger.
	 * Always check for invalidity first and then for dangerous values.
	 */
	
	// Minimum possible temperature is -273.15 degrees celcius.
	// Anything above 50 degrees is considered dangerous.
	public boolean isTemperatureInLevel() {
		boolean validity = true;
		
		if (this.temperature < -273.15) {
			validity = false;
			System.out.println(this.sensorId + " : Sensor is malfunctioning; A temperature of " + this.temperature + " celcius is below absolute zero.");
		}
		else if (this.temperature > 50.0) {
			validity = false;
			System.out.println(this.sensorId + " : Temperature is reaching a dangerous level at " + this.temperature + " celcius.");
		}
		
		return validity;
	}
	
	// Battery level over 100% indicates a malfunction in the battery.
	// Anything from 30% to 0% indicates low battery level.
	public boolean isBatteryInLevel() {
		boolean validity = true;
		
		if (this.batteryPercentage > 100 || this.batteryPercentage < 0) {
			validity = false;
			System.out.println(this.sensorId + " : Battery malfunction.");
		}
		
		else if (this.batteryPercentage <= 30) {
			validity = false;
			System.out.println(this.sensorId + " : Battery low!");
		}

		return validity;
	}
	
	// Smoke level above 7 is dangerous.
	// From 1 to 6 is considered okay.
	public boolean isSmokeInLevel() {
		boolean validity = true;
		
		if (this.smokeLevel < 1 || this.smokeLevel > 10) {
			System.out.println(this.sensorId + " : Smoke sensor malfunction.");
			validity = false;
		}
		
		else if (this.smokeLevel > 7) {
			System.out.println(this.sensorId + " : Smoke level is at a dangerous level of " + this.smokeLevel);
			validity = false;
		}
		
		return validity;
	}
	
	// A CO2 level of above or belowe 300.0 is considered dangerous.
	// At 300.0, CO2 level is considered okay.
	public boolean isCo2InLevel() {
		boolean validity = true; 
		
		if (this.co2Level != 300.0) {
			System.out.println(this.sensorId + " : CO2 level is at a dangerous level of " + this.co2Level);
			validity = false;
		}
		
		return validity;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
