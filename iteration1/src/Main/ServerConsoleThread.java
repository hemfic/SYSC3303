package Main;
import java.io.*;
public class ServerConsoleThread extends Thread {
	String shutdownCode = "000";
	BufferedReader consoleResponse;
	public ServerConsoleThread() {
		consoleResponse = new BufferedReader(new InputStreamReader(System.in));
    }
	public void run() {
		while (true) {
	        System.out.println("To shut down the server use the code 000 and press enter");
	        try {
	        	String input = consoleResponse.readLine();
	        	if (shutdownCode.equals(input)) {
	        		System.out.printf("%n%nShutting down the server softly%nSome threads will continue running until complete%n%n");
	        		System.exit(0);
	        	}else {
	        		System.out.println("Server: "+input+" is not a recognized code");
	        	}
	        }catch(IOException e) {
	        	System.out.println("An error occured while trying to read in line. Please try again");
	        }
	    }
	}
}
