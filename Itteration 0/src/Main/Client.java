package Main;

import java.net.*;
import java.util.Arrays;

public class Client {

   DatagramPacket sendPacket, receivePacket;
   DatagramSocket sockRS;
   byte[] rcvData; 						//buffer for receiving data

   public Client() {
	   rcvData=new byte[4]; 			//server only sends 4 bytes
	   receivePacket=new DatagramPacket(rcvData, rcvData.length); //initialize receiving packet - receiving will load data into rcvData directly
	   try {
		   sockRS = new DatagramSocket();
		   sockRS.setSoTimeout(50000);
	   } catch (Exception e) {   // Can't create the socket.
		   e.printStackTrace();
		   System.exit(1);
	   }
   }
   
   public void send(int type) {
	   int index=2;						//offset for packet creation
	   byte[] sendData=new byte[100]; 	//buffer for sending data
	   
	   String f="test.txt",t="netascii";
	   byte[] filename = f.getBytes(), typeB=t.getBytes();
	   if(type==10) {					// Invalid request
		   sendData[0]=4;
		   sendData[1]=2;
	   }else if(type%2==0) { 			// Read request
		   sendData[0]=0;
		   sendData[1]=1;
	   }else { 							// Write request
		   sendData[0]=0;
		   sendData[1]=2;
	   }
	   for(int i=0;i<filename.length;i+=1) {
		   index+=1;
		   sendData[i+2]=filename[i];	//load byte converted char from filename string
	   }
	   sendData[index]=0;
	   index+=1;
	   for(int i=index;i<index+typeB.length;i+=1) {
		   sendData[i]=typeB[i-index];	//load byte converted char from typeB string
	   }
	   sendData[index+typeB.length]=0;
	   
	   System.out.println("Sending(string): "+new String(sendData).trim()+" on port:"+sockRS.getLocalPort());
	   System.out.println("Sending(byte): "+Arrays.toString(sendData));
	   
	   try {
		   sendPacket=new DatagramPacket(sendData, 4+typeB.length+filename.length,InetAddress.getLocalHost() , 23);
		   sockRS.send(sendPacket);		//load packet and send
	   } catch (Exception e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   
	   try {							
		   sockRS.receive(receivePacket);
	   }catch(Exception e){
		   e.printStackTrace();
		   System.exit(1);
	   }
	   System.out.println("Received (string): "+new String(rcvData));
	   System.out.println("Received (byte): "+Arrays.toString(rcvData));
   }

   public static void main(String args[])
   {	
      Client c = new Client();
      for(int k = 0; k<=10;k+=1) {
    	  c.send(k);
    	  
      }

      System.exit(1);
   }
}
