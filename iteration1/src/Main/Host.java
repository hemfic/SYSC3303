package Main;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
//import java.util.Scanner;

public class Host {
	private DatagramPacket receivePacket;
	private DatagramSocket sockR;
	private byte[] rcvData; 
	public HostSettings settings;
	
	public Host() {
		settings = new HostSettings();
		System.out.println("Creating Host");
		System.out.printf("The Host simulates errors that occur in connections%nMode can be changed at (almost) any time as well as exitting.%nThe host will continue to recieve and start new threads in the selected mode%nDefault mode is '1', error free mode\n");
		Thread thread = new Thread(new HostConsoleThread(settings));
		thread.start();
		rcvData=new byte[516];					//buffer for receiving data
		receivePacket=new DatagramPacket(rcvData, rcvData.length);
		try {
	         sockR = new DatagramSocket(23);
		} catch (SocketException se) {
	         se.printStackTrace();
	         System.exit(1);
		} 
	}
	public void run() {
		while(true) {
			try {
				System.out.println("Host: Waiting for package...");
				sockR.receive(receivePacket);
				if(settings.getMode()==0) {
					break;
				}
				System.out.println("Host: Package received");
				Thread thread = new Thread(new HostConnectionManager(receivePacket,settings.getMode(),settings.getPacketNumber(),settings.getDelay()));
		        thread.start();
		        //while(thread.isAlive()); // Wait until the request is complete, or until the Host's Socket timesout
			}catch(IOException e) {
				System.out.println("Host: Fatal error");
				sockR.close();
				System.exit(1);
				
			}
		}
		System.out.println("Host Connection is shutting down");
		sockR.close();
		System.exit(1);
	}
	public static void main(String[] args) {
		Host h=new Host();
		h.run();
	}
}
