# Distributed Mutual Exclusion based on Quorum-based systems

### 1. Modify ClientInfo.java and ServerInfo.java first to match hostnames where each of the 5 clients and 3 servers are going to be run.
### 2. Create 3 directories named 0,1 & 2 (based on serverIDs) in this directory.
### 3. Create unique set of files and paste them in each of the directories ( all files must be same in each of the directories )

### 4. Compile Java program (on any supported platform : specifically dc machines on UTD network) :
  `javac Server.java ; javac Client.java`

### 5. SSH to each host and start the client/server :

  example for Client 0 : 
  
  // SSH using credentials; need to match hostname in ClientInfo.java
  
  `ssh dc01.utdallas.edu`
  
  // cd into directory where code is present
  
  `cd Distributed_mutex_ricart_agrawala/`
  
  // start the client node passing the ID as argument and logging all messages to a text file 
  
  `java ClientNode 0 | tee c0.txt`

####  For all clients and servers same has to be repeated :
```
  java ClientNode 1 | tee c1.txt
  java ClientNode 2 | tee c2.txt
  java ClientNode 3 | tee c3.txt
  java ClientNode 4 | tee c4.txt


  java ServerNode 0 | tee s0.txt
  java ServerNode 1 | tee s1.txt
  java ServerNode 2 | tee s2.txt
```

### 6. After all clients and servers are started on respective machines, Enter 'SETUP' from the terminal where Client 0 was started, to setup connections for communication.
   This command takes care of enquiring a random server for the list of files and manages this metadata for future use when issuing a READ/WRITE to a server.

### 7. The algorithm simulation can be started by entering the command 'START <iteration-count>' where iteration-count is an integer which is used to generate requested number of random READ/WRITEs.
### 8. Simulation can be ended by entering 'FINISH' from client 0's terminal.
