package Main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
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

   public Client() {
	   rcvData=new byte[4]; 			//server only sends 4 bytes
	   receivePacket=new DatagramPacket(rcvData, rcvData.length); //initialize receiving packet - receiving will load data into rcvData directly
	   try {
		   sockRS = new DatagramSocket();
		   sockRS.setSoTimeout(500);
	   } catch (Exception e) {   // Can't create the socket.
		   e.printStackTrace();
		   System.exit(1);
	   }
   }
   
   public void send(int type, String filenameString) {
	   int index=2;						//offset for packet creation
	   byte[] sendData=new byte[2^25]; 	//buffer for sending data
	   
	   String t="netascii";
	   byte[] filename = filenameString.getBytes(), typeB=t.getBytes();
	   if(type==1) {				// Read request
		   sendData[0]=0;
		   sendData[1]=1;
	   }else if(type==2) { 			// Write request
		   sendData[0]=0;
		   sendData[1]=2;
	   }
	   for(int i=0;i<filename.length;i+=1) {
		   index+=1;
		   sendData[i+2]=filename[i];	//load byte converted char from filenameString
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
		   sendPacket=new DatagramPacket(sendData, sendData.length,InetAddress.getLocalHost() , 23);
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
	   if(rcvData[0]==0&&rcvData[1]==3) {
		   handleRead(filenameString);
	   }
	   if(rcvData[0]==0&&rcvData[1]==4) {
		   handleWrite(filenameString);
	   }
   }
   
   public DatagramPacket buildAck(int block) {
	   DatagramPacket ret;
	   byte[] data= {0,4,0,0}
	   byte[] b=ByteBuffer.allocate(2).putInt(block).array();
	   data[2]=b[0];
	   data[3]=b[1];
	   ret= new DatagramPacket(data,data.length,InetAddress.getLocalHost(),23);
	   return ret;
   }
   
   //assumes the first block of received data is already in rcvData
   public void handleRead(String filename) {
	   FileOutputStream fout=null;
	   boolean done=false;
	   byte[] data= {};
	   try {
		   fout=new FileOutputStream(filename);
	   } catch (FileNotFoundException e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   while(!done) {
		   byte[] raw=Arrays.copyOfRange(rcvData, 4,rcvData.length-1);
		   byte[] rawB=Arrays.copyOfRange(rcvData, 2, 3);
		   int block= rawB[1]+(rawB[0]*16);
		   if(raw.length<512) {
			   if(data.length<(block-1)*512+raw.length) {
				   data=Arrays.copyOf(data,(block-1)*512+raw.length);
				   for(int i=(block-1)*512;i<(block-1)*512+raw.length;i+=1){
					   data[i]=raw[i-(block-1)*512];
				   }
			   }
			   done=true;
		   }else {
			   if(data.length<block*512) {
				   data=Arrays.copyOf(data,block*512);
				   for(int i=(block-1)*512;i<block*512;i+=1){
					   data[i]=raw[i-(block-1)*512];
				   }
			   }
		   }
		   System.out.println("Sending ACK for block: "+block);
		   try {
			   sockRS.send(buildAck(block));
		   } catch (IOException e1) {
			   e1.printStackTrace();
			   System.exit(1);
		   }
		   if(!done) {
			   try {
				   sockRS.receive(receivePacket);
			   } catch (IOException e) {
				   e.printStackTrace();
				   System.exit(1);
			   }

			   System.out.println("Received (string): "+new String(rcvData));
			   System.out.println("Received (byte): "+Arrays.toString(rcvData));
		   }
	   }
	   try {
		   fout.write(data);
	   } catch (IOException e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   try {
		   fout.close();
	   } catch (IOException e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   System.out.println("Done Read Request");
   }
   
   public void handleWrite(String filename) {
	   FileInputStream fin=null;
	   byte[] data=new byte[2*25];
	   byte[] sendData=new byte[512];
	   int size=0;
	   int blocks;
	   try {
		   fin=new FileInputStream(filename);
	   } catch (FileNotFoundException e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   try {
		   size=fin.read(data);
	   } catch (IOException e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   if(size==-1) {
		   System.err.println("File Specified Is Empty");
	   }
	   blocks=(data.length/512);
	   if(data.length%512!=0) {
		   blocks+=1;
	   }
	   for(int i=0;i<blocks;i+=1) {
		   for(int j=0;j<512;j+=1) {
			   sendData[j]=data[j+i*512];
		   }
		   System.out.println("Sending(string): "+new String(sendData).trim()+" on port:"+sockRS.getLocalPort());
		   System.out.println("Sending(byte): "+Arrays.toString(sendData));
		   try {
			   sendPacket=new DatagramPacket(sendData,sendData.length,InetAddress.getLocalHost(),23);
			   sockRS.send(sendPacket);
		   } catch (IOException e) {
			   e.printStackTrace();
			   System.exit(1);
		   }
		   try {
			   sockRS.receive(receivePacket);
		   } catch (IOException e) {
			   e.printStackTrace();
			   System.exit(1);
		   }

		   System.out.println("Received (string): "+new String(rcvData));
		   System.out.println("Received (byte): "+Arrays.toString(rcvData));
	   }
	   System.out.println("Done Write Request");
   }

   public static void main(String args[])
   {	
	  String f="C:\\Users\\OWNER\\Desktop\\text.txt";
      Client c = new Client();
      c.send(1,f);
      //c.send(2,f);
      System.exit(1);
   }
}
