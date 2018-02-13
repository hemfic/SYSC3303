# SYSC3303

Term Project: Iteration 1

Set-Up Instructions:
1. Run the Server.
2. Run the Host.
3. Run the Client. 
Note:(file paths are hardcoded at this point in time. The test text files are in the "main" folder. We also
      weren't able to finish implementing the ability to toggle between using the host, and the Client and Server talking       directly)


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



Team: 
Sarah Elizabeth "Liz" Davies  -- 100828244 -- SarahDavies4@cmail.carleton.ca
  Assignment for iteration 1: Server
  
Eric Reesor - 1000970401 - ericreesor@cmail.carleton.ca
  Assignment for iteration 1: Client
  
Thomas Carriere - 100947281 - thomasdehaancarriere@cmail.carleton.ca
  Assignment for iteration 1: Diagrams
  
Melissa Seaman (MIA) - 100939062 - melissaseaman@cmail.carleton.ca
  Assignment for iteration 1: User Interface.
  
TFTP - RFC1350 documentation
https://tools.ietf.org/html/rfc1350
