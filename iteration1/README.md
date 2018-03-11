# SYSC3303

Term Project: Iteration 3

Creating a request
      1. Run Server.java. Enter "true" in the console for Verbose Mode, and "false" for Quiet Mode.
      2. If running in Test mode, run Host.java
      	* Menu system for each request. If host shuts down unexpectedly between requests then rerun.
      3. Run Client.java. Input request settings based on console prompts.
      4. Once request is complete, you will be promted to input another request.
      Note: For a RRQ, the File to Read from should be placed in src/Main/ServerFiles
            For a WRQ, the File to Read from should be placed in src/Main/ClientFiles

Testing for iteration 3
      	The mode for the host will have to be set before ANY client request is made. If unexpectedly the host stops running, you may have to rerun it. 
      	
      
Testing for iteration 2
      Using the file explorer if desired, alter the permissions of the file ReadWriteForbidden.txt (It would seem it is impossible to send with the permissions as they are). Deny all.
      A test suite has been created to test most of the errors asked for in this Iteration2. In the console it is option 3.

Testing TFTP Error 3 (Disk Full or Allocation Exceeded)
      The best way to test this is on a small thumb drive. Running this package on it, repeatedly request MarkedFile.txt to new files.

Files:

Client.java: sends and receives files to/from the Server, using the Host as an intermediate

Host.java: receives packet from Client. Creates a HostConnectionManager thread to the retransmit the packet. Note: this class and HostConnectionManager were named as such, since they aren't simulating errors yet, but rather acting as a intermediate host for the Client and Server.

HostConnectionManager.java: forwards packet to it's destination (server/client)

Server.java: receives user request from host. Creates a ServerConsoleThread to poll for user input. Creates (as needed) clientConnectionThreads to handle requests.

ClientConnectionThread.java: handles an individual Client's request. Validates the request, and processes any data. Sends either data or acknowledgment (readRequest/WriteRequest) to Host for forwarding.

ServerConsoleThread.java: polls for user input. When exit code is confirmed, system will exit.



Diagrams:
* UML
* UCM_ReadRequest
* UCM_WriteRequest
* Timing Diagrams Various



Team:
Sarah Elizabeth "Liz" Davies  -- 100828244 -- SarahDavies4@cmail.carleton.ca
  Assignment for iteration 1: Server
  Assignment for iteration 2: Diagrams/ReadMe/Assisting with errors
  Assignment for iteration 3: packet loss, duplication

Eric Reesor - 1000970401 - ericreesor@cmail.carleton.ca
  Assignment for iteration 1: Client
  Assignment for iteration 2: Server
  Assignment for iteration 3: console, delay

Thomas Carriere - 100947281 - thomasdehaancarriere@cmail.carleton.ca
  Assignment for iteration 1: Diagrams
  Assignment for iteration 2: Client
  ASsignment for iteration 3: Client/Server adaptation

Melissa Seaman (MIA) - 100939062 - melissaseaman@cmail.carleton.ca
  Assignment for iteration 1: User Interface -- Not completed.
  Assignment for iteration 2: Nothing. Assumed to have dropped out.

TFTP - RFC1350 documentation
https://tools.ietf.org/html/rfc1350
