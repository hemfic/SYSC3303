package Main;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Host {
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sockR,sockS;
	byte[] rcvData; 
	boolean client;
	int clientport;
	
	public Host() {
		System.out.println("Creating Host");
		rcvData=new byte[516];					//buffer for receiving data
		receivePacket=new DatagramPacket(rcvData, rcvData.length);
		try {
	         sockR = new DatagramSocket(23);
		} catch (SocketException se) {
	         se.printStackTrace();
	         System.exit(1);
		} 
	}
	public void receive() {
		try {
			System.out.println("Host: Waiting for package...");
			sockR.receive(receivePacket);
			System.out.println("Host: Package received");
	        (new Thread(new HostConnectionManager(receivePacket))).start();
		}catch(IOException e) {
			System.out.println("Host: Fatal error");
			sockR.close();
			System.exit(1);
			
		}
		
	}
	public static void main(String[] args) {
		Host h=new Host();
		while(true) {
			h.receive();
		}
	}
}
