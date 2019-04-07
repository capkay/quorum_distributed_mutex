# Distributed Mutual Exclusion based in Quorum-based systems

### 1. Modify ClientInfo.java and ServerInfo.java first to match hostnames where each of the 5 clients and 7 servers are going to be run.
### 2. Compile Java program (on any supported platform : specifically dc machines on UTD network) :
  `javac SNode.java ; javac CNode.java`

### 3. SSH to corresponding host machine and start the respective server first:

  example for Server 1 : 
  
  // SSH using credentials; need to match hostname in ServerInfo.java
  
  `ssh dc01.utdallas.edu`
  
  // cd into directory where code is present
  
  `cd quorum_based_systems/`
  
  // start the client node passing the ID as argument and logging all messages to a text file 
  
  `java SNode 1 | tee S1.txt`

####  For all other servers same has to be repeated :
```
  java SNode 2 | tee S2.txt
  java SNode 3 | tee S3.txt
  java SNode 4 | tee S4.txt
  java SNode 5 | tee S5.txt
  java SNode 6 | tee S6.txt
  java SNode 7 | tee S7.txt

```
### 4. SSH to corresponding host machine and start the client in increasing order of client IDs:

```
  java CNode 1 | tee C1.txt
  java CNode 2 | tee C2.txt
  java CNode 3 | tee C3.txt
  java CNode 4 | tee C4.txt
  java CNode 5 | tee C5.txt
```
    After client 5 is started last, we can begin the simulation.

### 5. After all clients and servers are started on respective machines, Enter 'START' from the terminal where Server 1 was started, to start the simulation.

### 6. There are deadlocks bound to happen in this simulation. In that case 'START' or 'R' can be entered in Server 1's terminal to start the simulation again.
### 7. To adjust delays in a Client `CRIT MIN VAR` and `REL DELAY` can be used. Here MIN is the minimum delay that the client will wait till issuing the next request to enter critical section. VAR is a the random amount by which waiting time is affected . WAIT time = MIN + VAR where VAR is random[0,VAR) . DELAY is the amount of time a client stays in the critical section. All time units in ms.
### 8. Simulation can be ended by entering 'FINISH' from Server 1's terminal.
### Sample delays:
#### Client 1 :
    `CRIT 1200 500`
#### Client 2 :
    `CRIT 800 100`
#### Client 3 :
    `CRIT 600 600`
#### Client 4 :
    `CRIT 900 92`
#### Client 5 :
    `CRIT 1000 200`
