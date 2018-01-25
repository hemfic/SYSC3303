package Main;


import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Server {

   DatagramPacket sendPacket, receivePacket;
   DatagramSocket sock;

   public Server()
   {
      try {
         
         sock = new DatagramSocket(69);
        
      } catch (SocketException se) {
         se.printStackTrace();
         System.exit(1);
      } 
   }
   
   //parses incoming data using predefined structure
   public String[] parse(byte[] x) {
	   String[] ret= {"","",""};
	   int count=0;
	   int index=0; //offset for parsing
	   byte[] data=new byte[x.length-2]; 	//holds original message minus the first two bytes
	   byte[] file=new byte[x.length-2]; 	//holds the filename from the message
	   byte[] typet=new byte[x.length-2];	//holds the type from the message
	   byte[] temp;
	   boolean filename=false,type=false;
	   		if(x[0]==0&&x[1]==1) {			//check for read request
	   			ret[0]="r";
	   			for(int i =2;i<x.length;i+=1) {
	   				data[i-2]=x[i];			//copy x to data without the request type
	   			}
	   			for(int i = 0;i<data.length;i+=1) {
	   				if(!filename) {			//if still copying name
	   					if(data[i]>0) { 	//if is a valid character
	   						file[i]=data[i];//copy x portion containing the name to filename 
	   					}else {				//when the '0' byte is reached in the message, stop copying the name
	   						
	   						filename=true;
	   						index=i;		//save position in data
	   					}
	   				}
	   				
	   			}
	   			for (int i = index+1;i<data.length;i+=1) { //starting from position after the '0'
	   				if(!type) {				//if still copying type
	   					if(data[i]>0) {		//if is a valid character
	   						typet[i]=data[i];//copy x portion containing the type to typet 
	   					}else {				//when the '0' byte is reached in the message, stop copying the type
	   						
	   						type=true;		//stop copying type
	   					}
	   				}
	   				
	   			}
	   		}else if(x[0]==0&&x[1]==2) {	//check for write request
	   			ret[0]="w";
	   			for(int i =2;i<x.length;i+=1) {
	   				data[i-2]=x[i];
	   			}
	   			for(int i = 0;i<data.length;i+=1) {
	   				if(!filename) {			//if still copying name
	   					if(data[i]>0) {		//if is a valid character
	   						file[i]=data[i];//copy x portion containing the name to filename 
	   					}else {				//when the '0' byte is reached in the message, stop copying the name
	   						
	   						filename=true;
	   						index=i;		//save position in data
	   					}
	   				}
	   				
	   			}
	   			for (int i = index+1;i<data.length;i+=1) {	//starting from position after the '0'
	   				if(!type) {				//if still copying type
	   					if(data[i]>0) {		//if is a valid character
	   						typet[i]=data[i];//copy x portion containing the type to typet
	   					}else {				//when the '0' byte is reached in the message, stop copying the type
	   						
	   						type=true;		//stop copying type
	   					}
	   				}
	   				
	   			}
	   			
	   		}
	   		else {
	   			sock.close();
	   			System.err.println("ILLEGAL PACKET");
	   			System.exit(1);
	   		}
	   while(typet[0]==0) {
		   for(int k=0;k<typet.length-1;k+=1) {
			   typet[k]=typet[k+1];
		   }
	   }
	   
	   for (int i=0;i<typet.length;i+=1) {
		   if(typet[i]>64&&typet[i]<123) {
			   count+=1;
		   }
	   }
	   temp=new byte[count];
	   for (int i=0;i<count;i+=1) {
		   temp[i]=typet[i];
	   }
	   
	   ret[1]=new String(file);
	   ret[2]=new String(temp);
	   if(!(ret[2].equalsIgnoreCase("netascii")||ret[2].equalsIgnoreCase("octet"))) { //check for valid type
		   sock.close();
		   System.err.println("ILLEGAL TYPE");
		   System.exit(1);
	   }
	   return ret;
   }
   public void receive()
   {

      byte recieveData[] = new byte[30];//buffer for receiving data
      byte sendData[] = new byte[4];
      receivePacket = new DatagramPacket(recieveData, recieveData.length);

      try {
         sock.receive(receivePacket);
      } catch (IOException e) {
         System.out.print("IO Exception: likely:");
         System.out.println("Receive Socket Timed Out.\n" + e);
         e.printStackTrace();
         System.exit(1);
      }

      
      String[] received = (parse(recieveData)); 
      received[1]=received[1].trim();	//remove excess spaces
      received[2]=received[2].trim();	
      if(received[0]=="r") {			//check for read request
    	  System.out.println("Read request Received(string): "+received[1]+":"+received[2].trim()+" from port: "+receivePacket.getPort());
    	  System.out.println("Read request Data Received(byte[]): "+Arrays.toString(received[1].getBytes())+":"+Arrays.toString(received[2].getBytes()));
    	  sendData= new byte[] {0,3,0,1};
      }
      else if(received[0]=="w") {		//check for write request
    	  System.out.println("Write request Received(string): "+received[1]+":"+received[2]+" from port: "+receivePacket.getPort());
    	  System.out.println("Write request Data Received(byte[]): "+Arrays.toString(received[1].getBytes())+":"+Arrays.toString(received[2].getBytes()));
    	  sendData= new byte[] {0,4,0,0};
      }
      try {								//create socket, create datagram, send datagram and close socket
    	  DatagramSocket d = new DatagramSocket();
    	  sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), 23);
    	  System.out.println("Sending(string): "+new String(sendData)+" to port: "+sendPacket.getPort());
    	  System.out.println("Sending(byte): "+Arrays.toString(sendData));
    	  d.send(sendPacket);
    	  d.close();
      }catch (Exception e) {
    	  sock.close();
    	  e.printStackTrace();
    	  System.exit(1);
      }
   }

   public static void main( String args[] ) {
	  Server c = new Server();
	  while(true) {
      
      c.receive();
	  }
   }
}