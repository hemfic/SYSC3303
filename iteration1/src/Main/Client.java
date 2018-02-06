package Main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;
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
	   rcvData=new byte[516]; 			
	   receivePacket=new DatagramPacket(rcvData, rcvData.length); //initialize receiving packet - receiving will load data into rcvData directly
	   try {
		   sockRS = new DatagramSocket();
		   sockRS.setSoTimeout(50000);
	   } catch (Exception e) {   // Can't create the socket.
		   e.printStackTrace();
		   System.exit(1);
	   }
   }
   
   public void send(int type, String source,String dest,int mode) {
	   int index=2;	//offset for packet creation
	   String t="netascii";
	   if(mode==1) {
	   		t="netascii";
	   }else if(mode==2) {
		   t="octet";
	   }
	   byte[] filename = source.getBytes(), typeB=t.getBytes();
	   if(type==2) {
		   filename = dest.getBytes();
	   }
	   byte[] sendData=new byte[4+filename.length+typeB.length]; 	//buffer for sending data
	   
	
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
		   handleRead(dest);
	   }
	   if(rcvData[0]==0&&rcvData[1]==4) {
		   handleWrite(source);
	   }
   }
   
   public DatagramPacket buildAck(int block,InetAddress address,int port) {
	   DatagramPacket ret=null;
	   byte[] data= {0,4,0,0};
	   byte[] b=ByteBuffer.allocate(4).putInt(block).array();
	   data[2]=b[2];
	   data[3]=b[3];
	   System.out.println("ACK: "+Arrays.toString(data));
	   try {
		   ret= new DatagramPacket(data,data.length,address,port);
	   } catch (Exception e) {
		   e.printStackTrace();
		   System.exit(1);
	   }
	   return ret;
   }
   
   //assumes the first block of received data is already in rcvData
   public void handleRead(String filename) {
	   FileOutputStream fout=null;
	   String folderStructure = "src/Main/";
	   InetAddress serverAddress= receivePacket.getAddress();
	   int serverPort=receivePacket.getPort();
	   int i=0; 
	   boolean done=false, shortened=false;
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
		   int block= rawB[1]+(rawB[0]*16);
		   
		   if(raw.length<512) {
			   System.out.println("done");
			   if(data.length<(block-1)*512+raw.length) {
				   data=Arrays.copyOf(data,(block-1)*512+raw.length);
				   for(int j=(block-1)*512;j<(block-1)*512+raw.length;j+=1){
					   data[j]=raw[j-(block-1)*512];
				   }
			   }
			   done=true;
		   }else {
			   System.out.println("not done");
			   if(data.length<block*512) {
				   data=Arrays.copyOf(data,block*512);
				   for(int j=(block-1)*512;j<block*512;j+=1){
					   data[j]=raw[j-(block-1)*512];
				   }
			   }
		   }
		   System.out.println("Sending ACK for block: "+(block));
		   try {
			   sockRS.send(buildAck(block,serverAddress,serverPort));
		   } catch (IOException e1) {
			   e1.printStackTrace();
			   System.exit(1);
		   }
		   if(!done) {
			   try {
				   rcvData= new byte[516];
				   receivePacket=new DatagramPacket(rcvData,rcvData.length,serverAddress,serverPort);
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
		   System.out.println("Writing: "+new String(data));
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
	   String folderStructure = "src/Main/";
	   InetAddress serverAddress= receivePacket.getAddress();
	   int serverPort=receivePacket.getPort();
	   byte[] data=new byte[(int) Math.pow(2,25)];
	   byte[] sendData=new byte[516];
	   sendData[0]=0;
	   sendData[1]=3;
	   int size=0;
	   int blocks;
	   try {
		   fin=new FileInputStream(folderStructure+filename);
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
	   if(blocks==0) {
		   blocks=1;
	   }
	   for(int i=0;i<blocks;i+=1) {
		   for(int j=4;j<512;j+=1) {
			   sendData[2]=(byte) ((blocks+1)/Math.pow(2,8));
			   sendData[3]=(byte) ((blocks+1)%Math.pow(2,8));
			   sendData[j]=data[j+i*512-4];
		   }
		   System.out.println("Sending(string): "+new String(sendData).trim()+" on port:"+sockRS.getLocalPort());
		   System.out.println("Sending(byte): "+Arrays.toString(sendData));
		   try {
			   sendPacket=new DatagramPacket(sendData,sendData.length,serverAddress,serverPort);
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
	  boolean run=true;
	  String source="text1.txt";
	  String destination="text2.txt";
	  int mode=1,type=1;
	  String input;
	  Scanner s= new Scanner(System.in);
	  
      Client c = new Client();
      c.send(type,source,destination,mode);
      //type =2;
      //c.send(type,source,destination,mode);
      /*
      while(run) {
    	  System.out.println("1) Read Request \n2) Write Request\n3) Exit");
    	  input=s.nextLine();
    	  if(Integer.parseInt(input)==1){
    		  type=1;
    		  System.out.println("Enter filename of source for read");
    		  source=s.nextLine();
    		  System.out.println("Enter filename of destination for read");
    		  destination=s.nextLine();
    		  System.out.println("1) ASCII encoding \n2)Octet encoding");
    		  input=s.nextLine();
    		  if(Integer.parseInt(input)==1) {
    			  mode=1;
    		  }else if(Integer.parseInt(input)==2) {
    			  mode=2;
    		  }
    		  System.out.println("Settings: Type(1-read,2-write):"+type+" Source: "+source+" Destination: "+destination+" Encoding(1-ASCII, 2-Octet): "+mode);
    		  System.out.println("Settings ok? y/n");
    		  input=s.nextLine();
    		  System.out.println(
    				  );
    		  if(input=="y") {
    			  c.send(type,source,destination,mode);
    		  }
    	  }else if(Integer.parseInt(input)==2) {
    		  type=2;
    		  System.out.println("Enter filename of source for write");
    		  source=s.nextLine();
    		  System.out.println("Enter filename of destination for write");
    		  destination=s.nextLine();
    		  System.out.println("1) ASCII encoding \n2)Octet encoding");
    		  input=s.nextLine();
    		  if(Integer.parseInt(input)==1) {
    			  mode=1;
    		  }else if(Integer.parseInt(input)==2) {
    			  mode=2;
    		  }
    		  System.out.println("Settings: Type(1-read,2-write):"+type+" Source: "+source+" Destination: "+destination+" Encoding(1-ASCII, 2-Octet): "+mode);
    		  System.out.println("Settings ok? y/n");
    		  input=s.nextLine();
    		  System.out.println(input);
    		  if(input=="y") {
    			  c.send(type,source,destination,mode);
    		  }else {
    			  System.out.println("Re-enter settings");
    		  }
    	  }else if(Integer.parseInt(input)==3) {
    		  run=false;
    	  }
    	  
      }
      */
      System.exit(1);
   }
}
