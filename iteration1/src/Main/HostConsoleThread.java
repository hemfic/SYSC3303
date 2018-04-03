package Main;
import java.util.Scanner;

public class HostConsoleThread extends Thread {
	public HostSettings settings;
	public Scanner s;
	public HostConsoleThread(HostSettings set) {
		settings = set;
		s=new Scanner(System.in);
	}
	public void run() {
		while(true) {
			System.out.println("Choose simulation for next transfer request:");
			System.out.println("0) EXIT");
			System.out.println("1) Normal Mode");
			System.out.println("2) Lose Packet");
			System.out.println("3) Delay Packet");
			System.out.println("4) Duplicate Packet");
			System.out.println("5) Flip Bit(error 4)");
			int mode = getInteger();
			if(mode==0) {
	    		System.out.println("Exiting");	    		
	    		settings.setMode(0);	    			
	    		break;
	    	}else if(mode==1) {
	    		System.out.println("Running Normal Mode");
	    		settings.setMode(1);
	    	}else if(mode==2) {
	    		System.out.println("Running Packet Loss Mode");
	    		System.out.println("Enter what packet you want to drop. (0=Request packet, 1=1st transfer packet...) \nIt is assumed you choose a packet number knowing how many packets there will be.\nDo not choose packet 8 if there will only be 7 packets.");
	    		int packetTemp = this.getInteger();
	    		settings.allSettings(2,1000,packetTemp);
	    	}else if(mode==3) {
	    		System.out.println("Running Packet Delay Mode");
	    		System.out.println("Enter what packet you want to delay. (0=Request packet, 1=1st transfer packet...) \nIt is assumed you choose a packet number knowing how many packets there will be.\nDo not choose packet 8 if there will only be 7 packets.");
	    		int packetTemp = this.getInteger();
	    		System.out.println("Enter the desired delay value");
	    		int delayVal = this.getInteger();
	    		settings.allSettings(3,delayVal,packetTemp);
	     	}else if(mode==4) {
	    		System.out.println("Running Packet Duplication Mode");
	    		System.out.println("Enter what packet you want to duplicate. (0=Request packet, 1=1st transfer packet...) \nIt is assumed you choose a packet number knowing how many packets there will be.\nDo not choose packet 8 if there will only be 7 packets.");
	    		int packetTemp = this.getInteger();
	    		System.out.println("Enter the desired delay value");
	    		int delayVal = this.getInteger();
	    		settings.allSettings(4,delayVal,packetTemp);
	    	}else if(mode==5) {
	    		System.out.println("Running Illegal TFTP Operation Mode");
	    		System.out.println("Enter the packet you wish to alter");
	    		int packetNum = this.getInteger();
	    		System.out.println("Enter the bit you want to flip");
	    		int bit = this.getInteger();
	    		settings.allSettings(mode,bit,packetNum);
	    	}else if(mode == 6) {
	    		System.out.println("Running Unknown Transfer ID Mode");
	    		System.out.println("Enter the when to send");
	    		System.out.println("(0 is not a valid value,odd numbers will most likely be directed toward client, even, the server)");
	    		int packetNum = this.getInteger();
	    		while(packetNum == 0) {
	    			System.out.println("I told you the 0 was NOT valid! Please try again");
	    			packetNum = this.getInteger();
	    			settings.allSettings(mode,1000,packetNum);
	    		}
	    		settings.allSettings(mode, 1000, packetNum);;
	    	}else {
	    		System.out.println("Invalid input. Please Enter Again");
	    	}
		}
		s.close();
	}
	private int getInteger() {
		String input;
		int intVal;
		while(true) {
			input=s.nextLine();
			try {
				intVal = Integer.parseInt(input);
			}catch(NumberFormatException e) {
				continue;
			}
			break;			
		}
		return intVal;
	}
}
