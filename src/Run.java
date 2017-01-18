
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;

import com.cogniteam.HamsterAPIClient.Hamster;
import com.cogniteam.HamsterAPICommon.Common.HamsterError;
import com.cogniteam.HamsterAPICommon.Common.Log;
import com.cogniteam.HamsterAPICommon.Messages.GPS;
import com.cogniteam.HamsterAPICommon.Messages.IMU;
import com.cogniteam.HamsterAPICommon.Messages.Image;
import com.cogniteam.HamsterAPICommon.Messages.LidarScan;
import com.cogniteam.HamsterAPICommon.Messages.Pose;


public class Run 
{
	Hamster hamster;

	public void getScansBetween(Double min, Double max, List<Double> distances) throws HamsterError {
		
		LidarScan scan = hamster.getLidarScan();
		
		for (int i = 0; i < scan.getScanSize(); i++) {
			Double degree = (double) (scan.getScanAngleIncrement() * i);
			if (degree >= min && degree <= max)
				distances.add(scan.getDistance(i).doubleValue());
		}
		
	}
	
	public Boolean willCollide(List<Double> distances, int angle_from_center) throws HamsterError {
		
		LidarScan scan = hamster.getLidarScan();
		
		int collisions = 0;
		
		for (int i = distances.size() / 2 - angle_from_center / 2; i < distances.size() / 2 + angle_from_center / 2; i++)
			if (distances.get(i) < scan.getMaxRange() / 4.0)
				collisions++;
		return collisions >= angle_from_center / 4.0;		
	}
	
	public Boolean isFrontFree() throws HamsterError {
		
		List<Double> distances = new ArrayList<Double>();
		
		getScansBetween(90.0, 270.0, distances);
		return !willCollide(distances, 40);
	}
	
	public Boolean isLeftFree() throws HamsterError {

		List<Double> distances = new ArrayList<Double>();
		
		getScansBetween(180.0, 360.0, distances);
		return !willCollide(distances, 40);
	}

	public Boolean isRightFree() throws HamsterError {

		List<Double> distances = new ArrayList<Double>();
		
		getScansBetween(0.0, 180.0, distances);
		return !willCollide(distances, 40);
	}	

	public Boolean isBackFree() throws HamsterError {

		List<Double> distances = new ArrayList<Double>();
		
		getScansBetween(270.0, 360.0, distances);
		getScansBetween(0.0, 90.0, distances);
		
		return !willCollide(distances, 40);
	}
	
	public void moveForward() throws HamsterError {
		Log.i("Client", "Moving Forward");
		hamster.sendSpeed(5f, 0.0f);
	}
	
	public void turnLeft() throws HamsterError {
		Log.i("Client", "Turning Left");
		while (!isFrontFree())
			hamster.sendSpeed(1f, 45.0f);
	}
	
	public void turnRight() throws HamsterError {
		Log.i("Client", "Turning Right");
		while (!isFrontFree())
			hamster.sendSpeed(1f, -45.0f);
	}
	
	public void moveBackwards()  throws HamsterError {
		Log.i("Cliente", "Moving Backwards");
		
		while (!isLeftFree() && isRightFree() && isBackFree())
			hamster.sendSpeed(-5f, 0.0f);
	
		if (isLeftFree())
			turnLeft();
		else
			turnRight();
	}
	
	public void stopMoving() throws HamsterError {
		hamster.sendSpeed(0.0f, 0.0f);
	}
	
	public void remoteControlMode() throws HamsterError {
		Scanner rd = new Scanner(System.in);
		rd.useDelimiter("");
		
		char c;
		while(hamster.isConnected())
		{
			do {
				c = rd.next().charAt(0);
			
				switch (c) {
					case 'a':
						turnLeft();
						break;
					case 'w':
						moveForward();
						break;
						
					case 's':
						moveBackwards();
						break;
					case 'd':
						turnRight();
						break;
					default:
						Log.i("Cliente","Not an option.");
						break;
				}
			}
			while (c == 'a' || c == 'w' || c == 's' || c == 'd');	
		}
	}
	public void autonomusMode() throws HamsterError {
		
		while(hamster.isConnected())
		{
			if (isFrontFree())
		
				moveForward();
			else {
				stopMoving();
				if (isLeftFree())
					turnLeft();
				else if (isRightFree())
					turnRight();
				else if (isBackFree())
					moveBackwards();
				else
					Log.i("Client", "I am stuck!");
			}
		}
	}
	
	public void informationMode() throws HamsterError {
		while (hamster.isConnected()) {
			GPS gps = hamster.getGPS();
			IMU imu = hamster.getIMU();
			LidarScan lidar = hamster.getLidarScan();
			Pose pose = hamster.getPose();
			Image image = hamster.getCameraImage();				

			Log.i("Client",gps.toString());
			Log.i("Client",imu.toString());
			Log.i("Client",lidar.toString());
			Log.i("Client",pose.toString());
			Log.i("Client",image.toString());
		}
	}
	
	public static void main(String[] args) 
	{
		try
		{
			Run run = new Run();
			run.hamster = new Hamster(1);
			
			while(run.hamster.isConnected())
			{
				try
				{			
					System.out.println("Select mode:");
					System.out.println("[r]emote control");
					System.out.println("[a]utonomus");
					System.out.println("[i]nformation");
					
					Scanner rd = new Scanner(System.in);
					char option = rd.next().charAt(0);
					
					if (option == 'r')
						run.remoteControlMode();
					else if (option == 'a')
						run.autonomusMode();
					else if (option == 'i')
						run.informationMode();
					else 
						System.out.println("Not an option. Try again.");
				}
				catch(HamsterError message_error)
				{
					Log.i("Client",message_error.what());
				}
			}
		}
		catch(HamsterError connection_error)
		{
			Log.i("Client",connection_error.what());
		}
	}
}
