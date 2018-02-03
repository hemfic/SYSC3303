package Main;
import java.util.concurrent.ExecutionException;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;

public class ClientConnectionThread extends Thread{
	DatagramPacket ACKPacket,dataPacket;
	DatagramSocket sendRecieveSocket;
	
	int threadId;
	
	DatagramPacket clientRequest;
	
	AsynchronousFileChannel fileController;
	ByteBuffer dataBuffer;
	ByteBuffer errorBuffer;
	
	public ClientConnectionThread(DatagramPacket request,int id) {
		threadId = id;
		try {
			sendRecieveSocket = new DatagramSocket();
		}catch(SocketException e) {
			System.out.println("ServerThread("+threadId+"): An error occured while trying to create a socket for client connection.");
			try {
				sendRecieveSocket = new DatagramSocket();
				System.out.println("ServerThread("+threadId+"): New socket "+sendRecieveSocket.getLocalSocketAddress());
			}catch(SocketException se) {
				System.out.println("ServerThread("+threadId+"): Second try to create socket failed. Closing thread");
				System.out.println(se.getCause());
				System.exit(-1);
			}
		}
		clientRequest = request;		
	}
	public void run() {
		parse();
		sendRecieveSocket.close();
	}
	private int parse() {
		//Test for validity of data
		if(clientRequest.getLength()<6) return respondError("Request too short",4);
		byte data[] = clientRequest.getData();
		int naught[] = new int[3];
		int j = 0;
		for(int i=0;i<clientRequest.getLength();i++) {
			if(data[i]==0) {
				if(j>2) return respondError("Too many zeroes",4);
				naught[j++]=i;
			}
		}
		//need exactly three zeros. No more, no less.
		if(j<2) return respondError("Not enough zeroes",4);
		//No empty Strings plz;
		if(naught[0]>0 || naught[1]==3 || naught[2]==naught[1]+1) return respondError("One or more empty strings",4);
		//If strings are non-empty then do the work to make it
		String fName = new String(data,2,naught[1]-2);
		String method = new String(data,naught[1]+1,(naught[2]-naught[1])-1);
		method = method.toLowerCase();
		
		if(!method.equals("netascii") && !method.equals("octet"))return respondError("Invalid method: "+method,4);
		
		//Valid enough; Prepare response
		if(data[1]==1) {
			//readRequest
			System.out.printf("ServerThread("+threadId+"): Valid Read Request: %s :: %s%n",fName,method);
			return respondRead(fName,method);
		}else if(data[1]==2) {
			//writeRequest
			System.out.printf("ServerThread("+threadId+"): Valid Write Request: %s :: %s%n",fName,method);
			return respondWrite(fName,method);
		}else {
			return respondError("Improperly formatted request",4);
		}
	}
	private int respondRead(String fName,String method) {
		//Confirm file
		File file = new File(fName);
		if(!file.canRead()) return respondError("File either does not exist or cannot be read",1);
		if(file.length()>33554432) return respondError("File is too long for transfer",3);

		//Acquire file lock
		try {
			fileController = AsynchronousFileChannel.open(file.toPath(),StandardOpenOption.READ);
			//yes this line is a little hack-y, but its the only way to prevent Execution Exception
			fileController.lock(0,file.length(),true).get();
		    int block = 0;

		    //prebuild expected ack to make for easy comparison;
		    byte[] expectedACK = new byte[4];
		    byte[]data;
		    expectedACK[0]=(byte)0;
		    expectedACK[1]=(byte)4;
		    
		    //DOWHILE read buffer, send, recieve acknowledgment;
		    int rc = 512;
		    while (rc>511) {
		    	//(2) OPCODE | (2) Block | (0-512) bytes
		    	//OPCODE
		    	dataBuffer = ByteBuffer.allocate(516);
		    	//					 (2)OP   | 		      (2) block
		    	dataBuffer.putShort((short)3).putShort((short)(block+1));
		    	rc = fileController.read(dataBuffer,block*512).get();
		    	expectedACK[2] = (byte)((block+1) >> 8);
		    	expectedACK[3] = (byte)(block+1);
		    	dataPacket = new DatagramPacket(dataBuffer.array(),4+rc,clientRequest.getAddress(),clientRequest.getPort());
		    	byte[] ack = new byte[4];
		    	ACKPacket = new DatagramPacket(ack,4);
		    	try {
		    		sendRecieveSocket.send(dataPacket);
		    		sendRecieveSocket.receive(ACKPacket);
		    	}catch(IOException e) {
		    		System.out.println("IOException while trying to send data packet");
		    		e.printStackTrace();
		    	}
		    	//If ACKPacket is as expected then move to the next block, else assume loss and repeat?
		    	data=ACKPacket.getData();
		    	if(data[0]==expectedACK[0]&&data[1]==expectedACK[1]) ++block;
		    	else {
		    		System.out.println("Unexpected packet recieved");
		    		System.out.println("Non-optimal package order not protected for");
		    	}
		    }
		    fileController.close();
		    return 1;
		} catch (IOException e) {
		    System.out.println("An error most likely occured with the buffer");
		    e.printStackTrace();
		    try{
		    	fileController.close();
		    }catch(IOException ie) {
		    	ie.printStackTrace();
		    }
		    return -1;
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			e1.printStackTrace();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	private int respondWrite(String fName,String method) {
		//Check file
		File file =new  File(fName);

		//Acquire file lock
		try {		
			if(!file.createNewFile()) {
				if(!file.canWrite()) return respondError("Cannot write to specified file",6);
			}
			fileController = AsynchronousFileChannel.open(file.toPath(),StandardOpenOption.WRITE);
			while(true) {
				try {
					fileController.lock().get();
					break;
				}catch(NonWritableChannelException e) {
					return respondError("Cannot read from file",1);
				}catch(IllegalArgumentException e) {
					System.out.println("ServerThread("+threadId+"): "+e.getCause());
					return respondError("Illegal Argument",5);
				}catch(Exception e) {
					System.out.println("ServerThread("+threadId+"): "+e.getCause());
					e.printStackTrace();
				}
			}
			dataBuffer = ByteBuffer.allocate(516);
	    	dataBuffer.putShort((short)3);
	    	//block
	    	int block = 0;
	    	//data
		    byte[] data = new byte[516];
	    	
	    	++block;
		    int rc = 512;
		    //prepare ACK code in advance
		    byte[] ack = new byte[4];
		    ack[0]=(byte)0;
		    ack[1]=(byte)4;
		    ack[2]=(byte)0;
		    ack[3]=(byte)0;
		    ACKPacket = new DatagramPacket(ack,4,clientRequest.getSocketAddress());
		    sendRecieveSocket.send(ACKPacket);
		    //DOWHILE read buffer, send, receive acknowledgment;
		    while (rc==512) {
		    	dataPacket = new DatagramPacket(data,516);
			    ACKPacket = new DatagramPacket(ack,4,clientRequest.getAddress(),clientRequest.getPort());
		    	sendRecieveSocket.receive(dataPacket);
		    	dataBuffer.clear();
		    	dataBuffer.put(Arrays.copyOfRange(dataPacket.getData(),4,dataPacket.getLength()));
		    	block = data[2]<<8;
		    	block += data[3];
		    	rc = fileController.write(dataBuffer,block*512).get();
		    	ack[2] = data[2];
		    	ack[3] = data[3];
		    	ACKPacket = new DatagramPacket(ack,4);
		    	try {
		    		sendRecieveSocket.send(dataPacket);
		    		sendRecieveSocket.receive(ACKPacket);
		    	}catch(IOException e) {
		    		System.out.println("ServerThread("+threadId+"): IOException while trying to send data packet");
		    		e.printStackTrace();
		    	}
		    }
		    fileController.close();
		    return 2;
		} catch (IOException | InterruptedException | ExecutionException e) {
		    System.out.println(e.getCause());
		    e.printStackTrace();
		}
		return 0;
	}
	private int respondError(String errorMsg,int errorCode) {
		System.out.println("ServerThread("+threadId+"): AN ERROR OCCURRED: "+errorMsg);
		//(2)05 |  (02)ErrorCode |   ? ErrMsg   |  (1) 0
		byte[] errorArray = errorMsg.getBytes();
		errorBuffer = ByteBuffer.allocate((errorArray.length+5));
		errorBuffer.putShort((short)5).putShort((short)errorCode).put(errorArray).put((byte)0);
		System.out.printf("ServerThread("+threadId+"): Sending error packet to: %s %d%n",clientRequest.getAddress(),clientRequest.getPort());
		dataPacket = new DatagramPacket(errorBuffer.array(),errorBuffer.position(),clientRequest.getAddress(),clientRequest.getPort());
		try {
			sendRecieveSocket.send(dataPacket);
			System.out.println("ServerThread("+threadId+"): error packet sent");
			return 5;
		}catch(IOException e) {
			System.out.println("ServerThread("+threadId+"): An error occurred while tring to send an error report. Ironic");
			e.printStackTrace();
			return -1;
		}
	}
}
