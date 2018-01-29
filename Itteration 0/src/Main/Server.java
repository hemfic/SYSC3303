package Main;
import java.io.*;
import java.net.*;
public class Server {
	DatagramSocket receiveSocket;
	DatagramPacket receivePacket;
	
	Thread serverConsoleThread;
	ThreadGroup allResponseThreads;
	
	public Server() {
		//Create console monitor
		serverConsoleThread = new Thread(new ServerConsoleThread());
		serverConsoleThread.start();
		//create thread group for all client response threads
		
		allResponseThreads = new ThreadGroup("Response Threads");
		try {
			receiveSocket = new DatagramSocket(69);
		}catch(SocketException se) {
			System.out.println("Unable to create socket"); 
			se.printStackTrace();
	        System.exit(1);
		}
		try {
			//receive until Server console returns;
			while(true) {
				byte data[] = new byte[100];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("Server: Waiting for Packet.\n");
				receiveSocket.receive(receivePacket);
				System.out.println("Server: data received -- "+ receivePacket.getData().toString());
				System.out.println("Server: rough decription -- "+new String(receivePacket.getData()));
				Thread clientResponseThread = new Thread(allResponseThreads,new ClientConnectionThread(receivePacket));
				clientResponseThread.start();
				if(!serverConsoleThread.isAlive()) {
					System.out.println("Server: Shutdown request confirmed. No new requests will be accepted");
					System.out.println("Server: "+allResponseThreads.activeCount()+" threads remaining");
					receiveSocket.close();
					System.exit(0);
				}
			}
		}catch (IOException e) {
	         System.out.print("IO Exception: likely:");
	         System.out.println("Receive Socket Timed Out.\n" + e);
	         receiveSocket.close();
	         e.printStackTrace();
	         System.exit(1);
		}
	}
	public static void main(String args[]) {
		new Server();
	}
}
