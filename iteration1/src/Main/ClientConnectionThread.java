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
	
	String serverFiles = "src/Main/ServerFiles";
	
	DatagramPacket clientRequest;
	
	AsynchronousFileChannel fileController;
	ByteBuffer dataBuffer;
	ByteBuffer errorBuffer;
	
	boolean verbose;
	
	public ClientConnectionThread(DatagramPacket request,int id, boolean v) {
		threadId = id;
		verbose = v;
		try {
			sendRecieveSocket = new DatagramSocket();
		}catch(SocketException e) {
			printMessage("An error occured while trying to create a socket for client connection.");
			try {
				sendRecieveSocket = new DatagramSocket();
				printMessage("New socket "+sendRecieveSocket.getLocalSocketAddress());
			}catch(SocketException se) {
				printMessage("Second try to create socket failed. Closing thread");
				printMessage(se.getCause().toString());
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
		File file = new File(serverFiles,fName);
		printMessage(file.toString());
		if(!file.getAbsoluteFile().exists()) return respondError("File does not exist",1);
		if(file.getAbsoluteFile().length()>33554432) return respondError("File is too long for transfer",3);

		//Acquire file lock
		try {
			fileController = AsynchronousFileChannel.open(file.toPath(),StandardOpenOption.READ);

			
		    int block = 0;

		    //prebuild expected ack to make for easy comparison;
		    byte[] expectedACK = new byte[4];
		    expectedACK[0]=(byte)0;
		    expectedACK[1]=(byte)4;
		    
		    //DOWHILE read buffer, send, recieve acknowledgment;
		    int rc = 512;
		    FileLock lock;
		    while (rc>511) {
		    	//(2) OPCODE | (2) Block | (0-512) bytes
		    	//OPCODE
		    	dataBuffer = ByteBuffer.allocate(516);
		    	//					 (2)OP   | 		      (2) block
		    	dataBuffer.putShort((short)3).putShort((short)(block+1));
		    	
		    	while(true) {
					try {
						lock = fileController.lock(block*512,512,true).get();
						break;
					}catch(OverlappingFileLockException e) {
						
					}catch(NonReadableChannelException e) {
						return respondError("Cannot read from file",1);
					}catch(IllegalArgumentException e) {
						printMessage(e.getCause().toString());
						return respondError("Illegal Argument",5);
					}catch(Exception e) {
						printMessage(e.getCause().toString());
						e.printStackTrace();
					}
				}
		    	rc = fileController.read(dataBuffer,block*512).get();
		    	lock.release();
		    	expectedACK[2] = (byte)((block+1) >> 8);
		    	expectedACK[3] = (byte)(block+1);
		    	dataPacket = new DatagramPacket(Arrays.copyOfRange(dataBuffer.array(), 0, rc+4),4+rc,clientRequest.getAddress(),clientRequest.getPort());
		    	byte[] ack = new byte[4];
		    	ACKPacket = new DatagramPacket(ack,4);
		    	try {
		    		sendRecieveSocket.send(dataPacket);
		    		sendRecieveSocket.receive(ACKPacket);
		    	}catch(IOException e) {
		    		printMessage("IOException while trying to send data packet");
		    		e.printStackTrace();
		    	}
		    	//If ACKPacket is as expected then move to the next block, else assume loss and repeat?
		    	if(Arrays.equals(ack,expectedACK)) {
		    		++block;
		    		printMessage("rc = "+rc);
		    		printMessage("Acknowledgement Recieved");
		    	}else {
		    		rc =512;
		    		printMessage("Unexpected packet recieved");
		    		printMessage("Non-optimal package order not protected for");
		    	}
		    }
		    fileController.close();
		    return 1;
		} catch (AccessDeniedException e) { 
			return respondError("File cannot be read",2);
		} catch (IOException e) {
			printMessage("An error most likely occured with the buffer");
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
		File file = new File(serverFiles,fName);
		printMessage(file.getAbsolutePath());
		//Acquire file lock
		try {		
			if(!file.getAbsoluteFile().createNewFile()) {
				if(!file.getAbsoluteFile().canWrite()) return respondError("Cannot write to specified file",6);
			}
			fileController = AsynchronousFileChannel.open(file.toPath(),StandardOpenOption.WRITE);
			FileLock lock;
			dataBuffer = ByteBuffer.allocate(512);
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
		    	sendRecieveSocket.receive(dataPacket);
		    	dataBuffer.allocate(512);
		    	dataBuffer.put(Arrays.copyOfRange(data,4,dataPacket.getLength()));
		    	block = data[2]<<8;
		    	block += data[3];
		    	ack[2] = data[2];
		    	ack[3] = data[3];
				while(true) {
					try {
						lock = fileController.lock().get();
						break;
					}catch(NonWritableChannelException e) {
						return respondError("Cannot read from file",1);
					}catch(IllegalArgumentException e) {
						printMessage(e.getCause().toString());
						return respondError("Illegal Argument",5);
					}catch(Exception e) {
						printMessage(e.getCause().toString());
						e.printStackTrace();
					}
				}
		    	fileController.write(dataBuffer,block*512).get();
		    	lock.release();
		    	try {
		    		sendRecieveSocket.send(ACKPacket);
		    	}catch(IOException e) {
		    		printMessage("IOException while trying to send data packet");
		    		e.printStackTrace();
		    	}
		    }
		    fileController.close();
		    return 2;
		} catch (IOException | InterruptedException | ExecutionException e) {
			printMessage(e.getCause().toString());
		    e.printStackTrace();
		}
		return 0;
	}
	private int respondError(String errorMsg,int errorCode) {
		printMessage("AN ERROR OCCURRED: "+errorMsg);
		//(2)05 |  (02)ErrorCode |   ? ErrMsg   |  (1) 0
		byte[] errorArray = errorMsg.getBytes();
		errorBuffer = ByteBuffer.allocate((errorArray.length+5));
		errorBuffer.putShort((short)5).putShort((short)errorCode).put(errorArray).put((byte)0);
		printMessage("Sending error packet to: "+ clientRequest.getAddress()+clientRequest.getPort());
		dataPacket = new DatagramPacket(errorBuffer.array(),errorBuffer.position(),clientRequest.getAddress(),clientRequest.getPort());
		try {
			sendRecieveSocket.send(dataPacket);
			printMessage("error packet sent");
			return 5;
		}catch(IOException e) {
			printMessage("An error occurred while tring to send an error report. Ironic");
			e.printStackTrace();
			return -1;
		}
	}
	private void printMessage(String out) {
		if(verbose) {
			System.out.println("ServerThread("+threadId+"): "+out);
		}
	}
}
