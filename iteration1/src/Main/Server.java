package Main;
import java.io.*;
import java.net.*;
public class Server {
	
	DatagramSocket receiveSocket;
	DatagramPacket receivePacket;
	
	BufferedReader consoleResponse;
	Thread serverConsoleThread;
	ThreadGroup allResponseThreads;
	int num;
	public boolean verbose;
	
	public Server(boolean v) {
		//Create console monitor
		verbose = v;
		num = 1;
		//create thread group for all client response threads
		allResponseThreads = new ThreadGroup("Response Threads");
	}
	
	public void createClientThread() {
		try {
			receiveSocket = new DatagramSocket(69);
		}catch(SocketException se) {
			System.out.println("Server: Unable to create socket"); 
			se.printStackTrace();
	        System.exit(1);
		}
		try {
			//receive until Server console returns;
			while(true) {
				byte data[] = new byte[100];
				receivePacket = new DatagramPacket(data, data.length);
				System.out.println("\nServer: Waiting for Packet...");
				receiveSocket.receive(receivePacket);
				System.out.println("Server: data received -- "+ receivePacket.getData().toString());
				System.out.println("Server: rough decription -- "+new String(receivePacket.getData()));
				Thread clientResponseThread = new Thread(allResponseThreads,new ClientConnectionThread(receivePacket,num,verbose));
				++num;
				clientResponseThread.start();
				if(!serverConsoleThread.isAlive()) {
					System.out.println("Server: Shutdown request confirmed. No new requests will be accepted");
					System.out.println("Server: "+allResponseThreads.activeCount()+" threads remaining");
					receiveSocket.close();
					System.exit(0);
				}
			}
		}catch (IOException e) {
	         System.out.print("Server: IO Exception: likely:");
	         System.out.println("Server: Receive Socket Timed Out.\n" + e);
	         receiveSocket.close();
	         e.printStackTrace();
	         System.exit(1);
		}
	}
	public void consoleThreadUI() {
		serverConsoleThread = new Thread(new ServerConsoleThread());
		serverConsoleThread.start();
		
	}
	
	public void verboseUI() {
		consoleResponse = new BufferedReader(new InputStreamReader(System.in));
		while(true) {
			System.out.printf("Launching server. Verbose mode is: (true/false) => ");
			try {
				String input = consoleResponse.readLine();
			
			input.toLowerCase();
			if(input.equals("true")||input.equals("t")) {
				verbose = true;
				break;
			}else if(input.equals("false")||input.equals("f")) {
				verbose = false;
				break;
			}
			}catch(Exception e) {}
		}
	}
	
	public static void main(String args[]) {
		Server server = new Server(true);
		server.verboseUI();
		server.consoleThreadUI();
		server.createClientThread();
	}
	
}
