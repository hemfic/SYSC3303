package Main;

import java.io.IOException;
import java.net.*;

public class HostConnectionManager extends Thread{
	DatagramSocket socket;
	DatagramPacket datagramIn,datagramOut,originalRequest;
	SocketAddress clientAddress, serverAddress;
	byte[] data,originalData;
	int originalLength,mode,packetNumber,delayTime;
	
	public HostConnectionManager(DatagramPacket request, int modeNum, int packetNum, int delayT) {
		System.out.println("HostThread: created");
		this.mode = modeNum;
		this.packetNumber=packetNum;
		this.delayTime=delayT;
		originalRequest = request;
		originalData = request.getData();
		originalLength = request.getLength();
		clientAddress = request.getSocketAddress();		
		try {
			socket = new DatagramSocket();
			System.out.println("HostThread: New port on: "+socket.getLocalPort());
			socket.setSoTimeout(100000);
		}catch(SocketException e) {
			System.out.println("HostThread: An error occured while trying to create a socket for client connection.");
			try {
				socket = new DatagramSocket();
				socket.setSoTimeout(100000);
				System.out.println("Problem?");
			}catch(SocketException se) {
				System.out.println("HostThread: Second try to create socket failed. Closing thread");
				System.out.println(se.getCause());
				System.exit(-1);
			}
		}
	}
	public void run() {
		if(mode==0) {
			runWithoutError();
		}else if(mode==1) {
			runWithPacketDropError(packetNumber);
		}else if(mode==2) {
			runWithPacketDelayError(packetNumber,delayTime);
		}else if(mode==3) {
			runWithPacketDuplicationError(packetNumber,delayTime);
		}
		
	}	
	public void runWithoutError() {
		try {
			datagramOut = new DatagramPacket(originalData,originalLength,InetAddress.getLocalHost(),69);
			System.out.println("HostThread: Client -> Server");
			socket.send(datagramOut);
			data = new byte[516];
			datagramIn = new DatagramPacket(data,516);
			socket.receive(datagramIn);
			serverAddress = datagramIn.getSocketAddress();
			while(true) {
				if(datagramIn.getSocketAddress().equals(clientAddress)) {
					datagramOut = new DatagramPacket(datagramIn.getData(),datagramIn.getLength(),serverAddress);
					System.out.println("HostThread: Client -> Server");
				}else if(datagramIn.getSocketAddress().equals(serverAddress)){
					datagramOut = new DatagramPacket(datagramIn.getData(),datagramIn.getLength(),clientAddress);
					System.out.println("HostThread: Server -> Client");
				}else {
					System.out.println("HostThread: Interlopping connection");
					System.out.println("HostThread: Server address: "+serverAddress);
					System.out.println("HostThread: Client address: "+clientAddress);
					System.out.println("HostThread: New address: "+datagramIn.getSocketAddress());
					break;
				}
				socket.send(datagramOut);
				data = new byte[516];
				datagramIn = new DatagramPacket(data,516);
				socket.receive(datagramIn);
			}			
		}catch(SocketTimeoutException e) {
			System.out.println("HostThread: Socket timed out. Closing socket");
			socket.close();
			System.exit(0);
		}
		catch(SocketException e) {
			System.out.println("HostThread: "+e.getCause());
			socket.close();
			System.exit(0);
		} catch (IOException e) {
			System.out.print("HostThread: ");
			e.printStackTrace();
		}
	}
	public void runWithPacketDropError(int packetNum) {
		try {
			datagramOut = new DatagramPacket(originalData,originalLength,InetAddress.getLocalHost(),69);
			System.out.println("HostThread: Client -> Server");
			//In case of drop zeroth packet request
			if(packetNum>0) {
				socket.send(datagramOut);
			}
			data = new byte[516];
			datagramIn = new DatagramPacket(data,516);
			socket.receive(datagramIn);
			serverAddress = datagramIn.getSocketAddress();
			int currentPacket = 1;
			while(true) {
				if(packetNum != currentPacket) {
					//ONLY SEND PACKET IF IT DOES NOT EQUAL THE LOSSY PACKET
					if(datagramIn.getSocketAddress().equals(clientAddress)) {
						datagramOut = new DatagramPacket(datagramIn.getData(),datagramIn.getLength(),serverAddress);
						System.out.println("HostThread: Client -> Server");
					}else if(datagramIn.getSocketAddress().equals(serverAddress)){
						datagramOut = new DatagramPacket(datagramIn.getData(),datagramIn.getLength(),clientAddress);
						System.out.println("HostThread: Server -> Client");
					}else {
						System.out.println("HostThread: Interlopping connection");
						System.out.println("HostThread: Server address: "+serverAddress);
						System.out.println("HostThread: Client address: "+clientAddress);
						System.out.println("HostThread: New address: "+datagramIn.getSocketAddress());
						break;
					}
					socket.send(datagramOut);
					data = new byte[516];
					datagramIn = new DatagramPacket(data,516);
				}
				socket.receive(datagramIn);
				++currentPacket;
			}			
		}catch(SocketTimeoutException e) {
			System.out.println("HostThread: Socket timed out. Closing socket");
			socket.close();
			System.exit(0);
		}
		catch(SocketException e) {
			System.out.println("HostThread: "+e.getCause());
			socket.close();
			System.exit(0);
		} catch (IOException e) {
			System.out.print("HostThread: ");
			e.printStackTrace();
		}
	}		
	public void runWithPacketDuplicationError(int packetNum, int DelayMS) {
		try {
			datagramOut = new DatagramPacket(originalData,originalLength,InetAddress.getLocalHost(),69);
			System.out.println("HostThread: Client -> Server");
			socket.send(datagramOut);
			if(packetNum == 0) delayPacket(datagramOut,DelayMS,datagramOut.getSocketAddress());
			data = new byte[516];
			datagramIn = new DatagramPacket(data,516);
			socket.receive(datagramIn);
			serverAddress = datagramIn.getSocketAddress();
			int currentPacket = 1;
			while(true) {
				
				if(datagramIn.getSocketAddress().equals(clientAddress)) {
					datagramOut = new DatagramPacket(datagramIn.getData(),datagramIn.getLength(),serverAddress);
					System.out.println("HostThread: Client -> Server");
					if(currentPacket == packetNum) delayPacket(datagramIn,DelayMS,serverAddress);
				}else if(datagramIn.getSocketAddress().equals(serverAddress)){
					datagramOut = new DatagramPacket(datagramIn.getData(),datagramIn.getLength(),clientAddress);
					System.out.println("HostThread: Server -> Client");
					//setDelay function
					if(currentPacket == packetNum) delayPacket(datagramIn,DelayMS,clientAddress);
				}else {
					System.out.println("HostThread: Interlooping connection");
					System.out.println("HostThread: Server address: "+serverAddress);
					System.out.println("HostThread: Client address: "+clientAddress);
					System.out.println("HostThread: New address: "+datagramIn.getSocketAddress());
					break;
				}
				socket.send(datagramOut);
				data = new byte[516];
				datagramIn = new DatagramPacket(data,516);
				socket.receive(datagramIn);
				++currentPacket;
			}			
		}catch(SocketTimeoutException e) {
			System.out.println("HostThread: Socket timed out. Closing socket");
			socket.close();
			System.exit(0);
		}
		catch(SocketException e) {
			System.out.println("HostThread: "+e.getCause());
			socket.close();
			System.exit(0);
		} catch (IOException e) {
			System.out.print("HostThread: ");
			e.printStackTrace();
		}
	}
	public void delayPacket(DatagramPacket pack,int delay,SocketAddress addr) {
		//setDelay function
		DatagramPacket tempDatagram = new DatagramPacket(pack.getData(),pack.getLength(),addr);
		new java.util.Timer().schedule( 
		        new java.util.TimerTask() {
		            @Override
		            public void run() {
		            	try {
							socket.send(tempDatagram);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		            }
		        }, 
		        delay 
		);
	}
	
	public void runWithPacketDelayError(int packetNum, int delayMS) {
		int packetCount = 0;
		try {
			datagramOut = new DatagramPacket(originalData,originalLength,InetAddress.getLocalHost(),69);
			System.out.println("HostThread: Client -> Server");
			if(packetNum == 0) delayPacket(datagramOut,delayMS,datagramOut.getSocketAddress());
			else socket.send(datagramOut);
			data = new byte[516];
			datagramIn = new DatagramPacket(data,516);
			socket.receive(datagramIn);
			packetCount+=1;
			serverAddress = datagramIn.getSocketAddress();
			while(true) {
				if(datagramIn.getSocketAddress().equals(clientAddress)) {
					datagramOut = new DatagramPacket(datagramIn.getData(),datagramIn.getLength(),serverAddress);
					System.out.println("HostThread: Client -> Server");
				}else if(datagramIn.getSocketAddress().equals(serverAddress)){
					datagramOut = new DatagramPacket(datagramIn.getData(),datagramIn.getLength(),clientAddress);
					System.out.println("HostThread: Server -> Client");
				}else {
					System.out.println("HostThread: Interlopping connection");
					System.out.println("HostThread: Server address: "+serverAddress);
					System.out.println("HostThread: Client address: "+clientAddress);
					System.out.println("HostThread: New address: "+datagramIn.getSocketAddress());
					break;
				}
				if(packetCount==packetNum)delayPacket(datagramIn,delayMS,clientAddress);
				else {socket.send(datagramOut);}
				data = new byte[516];
				datagramIn = new DatagramPacket(data,516);
				socket.receive(datagramIn);
				packetCount+=1;
			}			
		}catch(SocketTimeoutException e) {
			System.out.println("HostThread: Socket timed out. Closing socket");
			socket.close();
			System.exit(0);
		}
		catch(SocketException e) {
			System.out.println("HostThread: "+e.getCause());
			socket.close();
			System.exit(0);
		} catch (IOException e) {
			System.out.print("HostThread: ");
			e.printStackTrace();
		}
	}
}
