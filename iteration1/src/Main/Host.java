package Main;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Scanner;

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
	public void receive(int mode, int packetNumber,int delayTime) {
		try {
			System.out.println("Host: Waiting for package...");
			sockR.receive(receivePacket);
			System.out.println("Host: Package received");
			Thread thread = new Thread(new HostConnectionManager(receivePacket,mode,packetNumber,delayTime));
	        thread.start();
	        while(thread.isAlive()); // Wait until the request is complete, or until the Host's Socket timesout
		}catch(IOException e) {
			System.out.println("Host: Fatal error");
			sockR.close();
			System.exit(1);
			
		}
		
	}
	public void receiveWithLoss() {
		Scanner s= new Scanner(System.in);
		String input;
		System.out.println("Enter what packet you want to lose. (0=Request packet, 1=1st transfer packet...) \nIt is assumed you choose a packet number knowing how many packets there will be.\nDo not choose packet 8 if there will only be 7 packets.");
		input=s.nextLine();
		receive(1,Integer.parseInt(input),0);
		s.close();
	}
	public void receiveWithDelay() {
		Scanner s= new Scanner(System.in);
		String input;
		int packetNum;
		System.out.println("Enter what packet you want to delay. (0=Request packet, 1=1st transfer packet...) \nIt is assumed you choose a packet number knowing how many packets there will be.\nDo not choose packet 8 if there will only be 7 packets.");
		input=s.nextLine();
		packetNum=Integer.parseInt(input);
		System.out.println("Enter the delay for the chosen packet in milliseconds");
		input=s.nextLine();
		receive(2,packetNum,Integer.parseInt(input));
		s.close();
	}
	public void receiveWithDupe() {
		Scanner s= new Scanner(System.in);
		String input;
		int packetNum;
		System.out.println("Enter what packet you want to duplicate. (0=Request packet, 1=1st transfer packet...) \nIt is assumed you choose a packet number knowing how many packets there will be.\nDo not choose packet 8 if there will only be 7 packets.");
		input=s.nextLine();
		packetNum=Integer.parseInt(input);
		System.out.println("Enter the delay for the duplicated copy of the packet in milliseconds");
		input=s.nextLine();
		receive(3,packetNum,Integer.parseInt(input));
		s.close();
	}
	public static void main(String[] args) {
		Host h=new Host();
		boolean run=true;
		Scanner s= new Scanner(System.in);
		String input;
		while(run) {
			System.out.println("Choose simulation for next transfer request:");
			System.out.println("1) Normal Mode");
			System.out.println("2) Lose Packet");
			System.out.println("3) Delay Packet");
			System.out.println("4) Duplicate Packet");
			System.out.println("5) EXIT");
			input=s.nextLine();
	    	if(Integer.parseInt(input)==1) {
	    		System.out.println("Running Normal Mode");
	    		h.receive(0,0,0);
	    	}else if(Integer.parseInt(input)==2) {
	    		System.out.println("Running Packet Loss Mode");
	    		h.receiveWithLoss();
	    	}else if(Integer.parseInt(input)==3) {
	    		System.out.println("Running Packet Delay Mode");
	    		h.receiveWithDelay();
	    	}else if(Integer.parseInt(input)==4) {
	    		System.out.println("Running Packet Duplication Mode");
	    		h.receiveWithDupe();
	    	}else if(Integer.parseInt(input)==5) {
	    		System.out.println("Exiting");
	    		run=false;
	    	}else {
	    		System.out.println("Invalid input. Please Enter Again");
	    	}
			
		}
		s.close();
	}
}
