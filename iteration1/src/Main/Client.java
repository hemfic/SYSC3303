package Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
/*
Type   Opcode     Format without header

	   2 bytes    string    1 byte     string   1 byte
	   -----------------------------------------------
RRQ/  | 01/02 |  Filename  |   0  |    Mode    |   0  |
WRQ    -----------------------------------------------
		2 bytes    2 bytes       n bytes
		---------------------------------
DATA   | 03    |   Block #  |    Data    |
		---------------------------------
		2 bytes    2 bytes
		-------------------
ACK    | 04    |   Block #  |
		--------------------
*/

public class Client {

   private DatagramPacket sendPacket, receivePacket;
   private DatagramSocket sockRS;
   private byte[] rcvData; 						//buffer for receiving data
   private final int HOSTPORT = 23, SERVERPORT = 69;
   private boolean verbose=false;

   public Client() {
	   rcvData=new byte[516]; 			
	   receivePacket=new DatagramPacket(rcvData, rcvData.length); //initialize receiving packet - receiving will load data into rcvData directly
	   try {
		   sockRS = new DatagramSocket();
		   sockRS.setSoTimeout(10000);
	   } catch (Exception e) {   // Can't create the socket.
		   e.printStackTrace();
		   System.exit(1);
	   }
   }
   
   public void send(int type, String source,String dest, int operMode) {
	   int index = 2;	//offset for packet creation
	   int sendPort = SERVERPORT; 
	   String t = "netascii";
	   
	   byte[] filename = source.getBytes(), typeB=t.getBytes();
	   if(type==2) {
		   filename = dest.getBytes();
	   }
	   byte[] sendData=new byte[4 + filename.length + typeB.length]; 	//buffer for sending data
	   
	   //set sendingPort depending on Operating Mode(Normal/Test)
	   if(operMode==1) {
		   sendPort = SERVERPORT;
	   }else if(operMode==2) {
		   sendPort = HOSTPORT;
	   }
	
	   if(type==1) {				// Read request
		   sendData[0] = 0;
		   sendData[1] = 1;
	   }else if(type==2) { 			// Write request
		   sendData[0] = 0;
		   sendData[1] = 2;
	   }
	   for(int i=0;i<filename.length;i++) {
		   index++;
		   sendData[i+2]=filename[i];	//load byte converted char from filenameString
	   }
	   sendData[index]=0;
	   index++;
	   for(int i=index;i<index + typeB.length;i++) {
		   sendData[i] = typeB[i-index];	//load byte converted char from typeB string
	   }
	   sendData[index + typeB.length]=0;
	   
	   try {
		   sendPacket=new DatagramPacket(sendData, sendData.length,InetAddress.getLocalHost() , sendPort);
		   sockRS.send(sendPacket);		//load packet and send
		   
	   } catch (Exception e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   if(verbose) {
		   System.out.print("Sent: ");
	   
		   if(type==1) {
			   System.out.print("RRQ to: ");
		   }else {
			   System.out.print("WRQ to: ");
		   }
		   try {
			   System.out.print(" IP:"+InetAddress.getLocalHost().getHostAddress()+" Port:"+sendPort);
		   } catch (UnknownHostException e1) {
			   e1.printStackTrace();
		   }
		   System.out.println(" Source filename: "+source+" Mode: ASCII");
	   }
	   try {							
		   sockRS.receive(receivePacket);
	   }catch(SocketTimeoutException e) {
		   //No server response, trying one more time
		   try{
			   sockRS.send(sendPacket);
			   sockRS.receive(receivePacket);
		   }catch(SocketTimeoutException ex) {
			   System.out.println("Second attempt to contact server has failed. I quit.");
			   return;
		   }catch(IOException ex) {
			   System.out.println("An error occurred when trying to send the request");
			   ex.printStackTrace();
		   }catch(Exception ex){
			   ex.printStackTrace();
		   }
	   }catch(Exception e){
		   e.printStackTrace();
		   System.exit(1);
	   }
	   
	   //check if we received an error packet. All packets, for now, are non-recoverable
	   if(rcvData[0]==0 && rcvData[1]==5) {
		   handleError();
		}
	   
	   if(rcvData[0]== 0 &&rcvData[1]==3) {
		   if(verbose) {
			   System.out.print("Received: DATA from: ");
			   try {
				   System.out.print(" IP:"+InetAddress.getLocalHost().getHostAddress()+" Port:"+sendPort);
			   } catch (UnknownHostException e1) {
				   e1.printStackTrace();
			   }
			   System.out.println(" Block:"+(rcvData[3]+rcvData[2]*16)+" Data Size: "+receivePacket.getLength());
		   }
		   handleRead(dest);
	   }
	   if(rcvData[0]==0 && rcvData[1]==4) {
		   if(verbose) {
			   System.out.print("Received: ACK from: ");
			   try {
				   System.out.print(" IP:"+InetAddress.getLocalHost().getHostAddress()+" Port:"+sendPort);
			   } catch (UnknownHostException e1) {
				   e1.printStackTrace();
			   }
			   System.out.println(" Block: 0");
		   }
		   handleWrite(source);
	   }
	   
   }
   
   public DatagramPacket buildAck(int block,InetAddress address,int port) {
	   DatagramPacket ret=null;
	   byte[] data= {0,4,0,0};
	   byte[] b=ByteBuffer.allocate(4).putInt(block).array();
	   data[2]=b[2];
	   data[3]=b[3];
	   try {
		   ret= new DatagramPacket(data,data.length,address,port);
	   } catch (Exception e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   return ret;
   }
   
   
   public int handleError() {
	   if(verbose) {
		   System.out.print("ERROR TYPE " + rcvData[3] + " received from");
		   try {
			   System.out.println(" IP:"+InetAddress.getLocalHost().getHostAddress()+" Port:"+receivePacket.getPort());
		   } catch (UnknownHostException e1) {
			   e1.printStackTrace();
		   }		
	   }
	   byte[] errorMessage = Arrays.copyOfRange(rcvData, 4,receivePacket.getLength());
	   
	   System.out.println("Error Message: " + new String(errorMessage));
	   System.out.println("Terminating Request");
	   //System.exit(1);
	   return -1;
   }
   
   //assumes the first block of received data is already in rcvData
   public void handleRead(String filename) {
	   FileOutputStream fout=null;
	   String folderStructure = "src/Main/ClientFiles/";
	   InetAddress serverAddress= receivePacket.getAddress();
	   int serverPort=receivePacket.getPort();
	   int i=0; 
	   boolean done=false;
	   byte[] data= {};
	   try {
		   fout=new FileOutputStream(folderStructure+filename);
	   } catch (FileNotFoundException e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   while(!done) {
		   byte[] raw=Arrays.copyOfRange(rcvData, 4,receivePacket.getLength());
		   byte[] rawB=Arrays.copyOfRange(rcvData, 2, 4);
		   int block= unsignedToBytes(rawB[1]) + unsignedToBytes(rawB[0])*256;
		   
		   if(raw.length<512) {
			   if(data.length<(block-1) * 512 + raw.length) {
				   data=Arrays.copyOf(data,(block-1) * 512 + raw.length);
				   for(int j=(block-1)*512;j<(block-1) * 512 + raw.length;j++){
					   data[j] = raw[j-(block-1)*512];
				   }
			   }
			   done = true;
		   }else {
			   if(data.length<block*512) {
				   data=Arrays.copyOf(data,block*512);
				   for(int j = (block-1)*512;j<block*512;j++){
					   data[j]=raw[j-(block-1)*512];
				   }
			   }
		   }
		   try {
			   sockRS.send(buildAck(block,serverAddress,serverPort));
		   } catch (IOException e1) {
			   e1.printStackTrace();
			   System.exit(1);
		   }
		   if(verbose) {
			   System.out.print("Sent: ACK to: ");
			   try {
				   System.out.print(" IP:"+InetAddress.getLocalHost().getHostAddress()+" Port:"+sendPacket.getPort());
			   } catch (UnknownHostException e1) {
				   e1.printStackTrace();
		   		}
		   		System.out.println(" Block: "+block);
		   }
		   if(!done) {
			   try {
				   rcvData= new byte[516];
				   receivePacket=new DatagramPacket(rcvData,rcvData.length,serverAddress,serverPort);
				   sockRS.receive(receivePacket);
				   
				   if(rcvData[0]==0 && rcvData[1]==5) {
					  handleError();
					  return;
				   }
				   
			   } catch (IOException e) {
				   e.printStackTrace();
				   System.exit(1);
			   }
			   if(verbose) {
				   System.out.print("Received: DATA from: ");
				   try {
					   System.out.print(" IP:"+InetAddress.getLocalHost().getHostAddress()+" Port:"+sendPacket.getPort());
				   } catch (UnknownHostException e1) {
					   e1.printStackTrace();
				   }
				   System.out.println(" Block:"+(rcvData[3]+rcvData[2]*16)+" Data Size: "+receivePacket.getLength());
			   }
			   
		   }
	   }
	   try {
		   if(verbose) {
			   System.out.println("Writing: "+ new String(data));
		   }
		   fout.write(data);
		   fout.close();
	   } catch (IOException e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   System.out.println("Done Read Request");
   }
   
   public void handleWrite(String filename) {
	   FileInputStream fin=null;
	   Set<Integer> acksReceived = new HashSet<Integer>();
	   
	   String folderStructure = "src/Main/ClientFiles/";
	   File f= new File(folderStructure+filename);
	   InetAddress serverAddress = receivePacket.getAddress();
	   int serverPort = receivePacket.getPort();
	   byte[] data = new byte[(int)f.length()];
	   byte[] sendData = new byte[516];
	   sendData[0] = 0;
	   sendData[1] = 3;
	   int size = 0;
	   int blocks;
	   try {
		   fin = new FileInputStream(f);
	   } catch (FileNotFoundException e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   try {
		   size = fin.read(data);
	   } catch (IOException e) {
		   e.printStackTrace();
	   }
	   
	   blocks = (data.length / 512);
	   if(data.length%512!=0) {
		   blocks += 1;
	   }
	   for(int i=0;i<blocks;i++) {
		   int ackNumber = ByteBuffer.wrap(receivePacket.getData()).getShort(2);
		   if(acksReceived.contains(ackNumber)) {
			   System.out.println("Duplicate ACKS received(Sorcerers Apprentice Bug)");
			   System.out.println("Terminating Request.");
			   return;
		   }
		   acksReceived.add(ackNumber);
		   
		   if(i==blocks-1) {
			   sendData=new byte[data.length-(i*512)+4];
		   }else {
			   sendData=new byte[516];
		   }
		   sendData[0] = 0;
		   sendData[1] = 3;
		   sendData[2]=(byte) ((i+1) / Math.pow(2,8));
		   sendData[3]=(byte) ((i+1) % Math.pow(2,8));
		   for(int j=4;j<sendData.length;j++) {
			   sendData[j]=data[j+(i*512)-4];
		   }
		   sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
		   sendData(i);
		   
	   }
	   System.out.println("Done Write Request");
   }
   
   //attempt to send DATA and receive ACK. If ACK isn't received, re-send the DATA.
   public void sendData(int index) {
	   try {
		   sockRS.send(sendPacket);
	  
		   if(verbose) {
			   System.out.print("Sent: DATA to: ");
			   try {
				   System.out.print(" IP:"+InetAddress.getLocalHost().getHostAddress()+" Port:"+sendPacket.getPort());
			   } catch (UnknownHostException e1) {
				   e1.printStackTrace();
			   }
			   System.out.println(" Block: "+(index+1)+" Data Size: "+(sendPacket.getLength()-4));
		   }
	  
		   sockRS.setSoTimeout(5000);
		   sockRS.receive(receivePacket);
		   sockRS.setSoTimeout(0); //remove timeout?

		   if(rcvData[0]==0 && rcvData[1]==5) {
			  handleError();
	       }
		   
	   } catch (SocketTimeoutException e) {
		   if(verbose) {
			   System.out.println("ACK was not received. Attempting to re-send DATA. ");
		   }
		   sendData(index);
		   return;
	   } catch(IOException e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   if(verbose) {
		   System.out.print("Received: ACK from: ");
		   try {
			   System.out.print(" IP:"+InetAddress.getLocalHost().getHostAddress()+" Port:"+sendPacket.getPort());
		   } catch (UnknownHostException e1) {
			   e1.printStackTrace();
		   }
		   System.out.println(" Block: "+(index+1));
	   }
   }
   
   public void toggleVerbose() {
	   verbose=!verbose;
   }
   
   public String getVerboseState() {
	   String ret="";
	   if(verbose) {
		   ret="true";
	   }else {ret="false";}
	   return ret;
   }
   public boolean testSuite() {
	   System.out.printf("%n%n%n%n");
	   System.out.println("\nTesting a proper WRQ");
	   send(2,"WriteTo.txt","WriteTo.txt",1);
	   System.out.println("\nTesting a proper RRQ");
	   send(1,"MarkedFile.txt","bigFileLocal.txt",1);
	   System.out.println("\nTesting a WRQ to a file that already exists");
	   send(2,"WriteTo.txt","WriteTo.txt",1);
	   System.out.println("\nTesting RRQ for non-existent file");
	   send(1,"test.txt","ThisDoesntExist.txt",1);
	   System.out.println("\nTesting sending a poorly formated/illegal request");
	   send(2,"","",1);
	   System.out.println("\nTesting RRQ on forbidden file");
	   send(1,"ReadWriteForbidden.txt","test.txt",1);
	   return true;
   }
   private static int unsignedToBytes(byte a) {
		int b = a & 0xFF;
		return b;
	}

   public static void main(String args[])
   {
	  String source="";
	  String destination="1";
	  int requestType=1, operMode=2;
	  String input;
	  Scanner s= new Scanner(System.in);
	  Client c = new Client();
	  
      while(true) {
    	  System.out.println("Toggle Verbose Mode? y/n Currently: "+c.getVerboseState());
    	  if(s.nextLine().equalsIgnoreCase("y")) {
    		  c.toggleVerbose();
    		  System.out.println("Verbose toggled. Now: "+c.getVerboseState());
    	  }
    	  System.out.println("1) Read Request \n2) Write Request\n3) Run test suite\nType Exit at any point to exit");
    	  input=s.nextLine();
    	  requestType =0;
    	  requestType = Integer.parseInt(input);
    	  if(requestType!=1 && requestType!=2 && requestType!=3) break;
    	  if(requestType==1) {
    		  System.out.println("Enter filename of source for read");
        	  source = s.nextLine();
        	  if(source.equalsIgnoreCase("exit")) {break;}
        	  System.out.println("Enter filename of destination for read");
          	  destination = s.nextLine();
          	  if(destination.equalsIgnoreCase("exit")) {break;}
    	  }else if(requestType==2){
    		  System.out.println("Enter filename of source for write");
        	  source = s.nextLine();
        	  if(source.equalsIgnoreCase("exit")) {break;}
        	  System.out.println("Enter filename of destination for write");
          	  destination = s.nextLine();
          	  if(destination.equalsIgnoreCase("exit")) {break;}
    	  }else {
    		  c.testSuite();
    		  s.close();
    	      System.exit(1);
    	  }
      	  System.out.println("1) Normal Mode \n2) Test Mode");
   	  	  input = s.nextLine();
   	  	  if(input.equalsIgnoreCase("exit")) {break;}
   	  	  operMode = Integer.parseInt(input);
   	  	  while(operMode!= 1 && operMode!=2){
   	  		  System.out.println("1) Normal Mode \n2) Test Mode");
   	  		  input = s.nextLine();
   	  		  if(input.equalsIgnoreCase("exit")) {break;}
   	  		  operMode = Integer.parseInt(input);
   	  	  }
   	  	  System.out.println("Settings: Type(1-read,2-write):"+requestType+" Source: "+source+" Destination: "+destination + " OperatingMode(1-Normal, 2-Test): " + operMode);
   	  	  System.out.println("Settings ok? y/n");
   	  	  input = s.nextLine();
    	  if(input.equalsIgnoreCase("y")) {
    		  c.send(requestType,source,destination,operMode);
    	  }
    	}
      s.close();
      System.exit(1);
   }
}
