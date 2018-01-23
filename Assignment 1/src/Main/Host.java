package Main;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

public class Host {
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sockR,sockS;
	byte[] rcvData; 
	boolean client;
	int clientport;
	
	public Host() {
		client=true;
		clientport=0;
		rcvData=new byte[100];					//buffer for receiving data
		receivePacket=new DatagramPacket(rcvData, rcvData.length);
		try {
	         
	         sockR = new DatagramSocket(23);
	        
	      } catch (SocketException se) {
	         se.printStackTrace();
	         System.exit(1);
	      } 
		
	}
	
	public void run() {
		rcvData=new byte[100];					//flush buffer
		receivePacket=new DatagramPacket(rcvData, rcvData.length);
		
		try {
			sockR.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		if(client) {
			clientport=receivePacket.getPort();	//save client port for sending server's response back
			client=false;
		}
		System.out.println("Received (string): " + new String(rcvData).trim()+" from port: "+receivePacket.getPort());
		System.out.println("Received (byte): " + Arrays.toString(rcvData));
		if((receivePacket.getData()[1]==1||receivePacket.getData()[1]==2)) {//check for message from client
			sendPacket= new DatagramPacket(receivePacket.getData(),receivePacket.getLength(),receivePacket.getAddress(),69);
		}else {
			sendPacket= new DatagramPacket(receivePacket.getData(),receivePacket.getLength(),receivePacket.getAddress(),clientport);
		}
		
		try {									//create socket, create datagram, send datagram and close socket
			System.out.println("Sending (string): " + new String(sendPacket.getData()).trim()+" on port: "+sendPacket.getPort());
			System.out.println("Sending (byte): " + Arrays.toString(sendPacket.getData()));
			sockS=new DatagramSocket(); 
			sockS.send(sendPacket);
			sockS.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void main(String[] args) {
		Host h=new Host();
		while(true) {
			h.run();
		}
	}
}
