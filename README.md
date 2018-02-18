# SYSC3303

Term Project: Iteration 1

Creating a request
      1. Run Server.java. Enter "true" in the console for Verbose Mode, and "false" for Quiet Mode.
      2. If running in Test mode, run Host.java.
      3. Run Client.java. Input request settings based on console prompts.
      4. Once request is complete, you will be promted to input another request.
      Note: For a RRQ, the File to Read from should be placed in src/Main/ServerFiles
            For a WRQ, the File to Read from should be placed in src/Main/ClientFiles

      Testing
      A test suite has been created to test most of the errors asked for in this assignment. In the console it is option 3.

      Testing TFTP Error 3 (Disk Full or Allocation Exceeded)
      The best way to test this is on a small thumb drive. Running this package on it, repeatedly request MarkedFile.txt to new files.

Files:

Client.java: sends and receives files to/from the Server, using the Host as an intermidiate

Host.java: receives packet from Client. Creates a HostConnectionManager thread to the retransmit the packet. Note: this class and HostConnectionManager were named as such, since they aren't simulating errors yet, but rather acting as a intermidiate host for the Client and Server.

HostConnectionManager.java: forwards packet to it's destination (server/client)

Server.java: receives user request from host. Creates a ServerConsoleThread to poll for user input. Creates (as needed) clientConnectionThreads to handle requests.

ClientConnectionThread.java: handles an indivul Client's request. Validates the request, and processes any data. Sends either data or acknowledment (readRequest/WriteRequest) to Host for forwarding.

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

Eric Reesor - 1000970401 - ericreesor@cmail.carleton.ca
  Assignment for iteration 1: Client
  Assignment for iteration 2: Server

Thomas Carriere - 100947281 - thomasdehaancarriere@cmail.carleton.ca
  Assignment for iteration 1: Diagrams
  Assignment for iteration 2: Client

Melissa Seaman (MIA) - 100939062 - melissaseaman@cmail.carleton.ca
  Assignment for iteration 1: User Interface -- Not completed.
  Assignment for iteration 2: Nothing. Assumed to have dropped out.

TFTP - RFC1350 documentation
https://tools.ietf.org/html/rfc1350
