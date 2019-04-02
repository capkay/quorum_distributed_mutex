import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedList;
import java.util.*;
import java.lang.management.*;
import java.lang.*;
import java.net.InetAddress;
import java.text.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

// Node class : handles connections to server nodes and other client nodes pulled from ClientInfo and ServerInfo
class SNode 
{
    // socket variables 
    ServerSocket server = null;
    // variables to obtain IP/port information of where the node is running
    InetAddress myip = null;
    String hostname = null;
    String ip = null;
    String port = null;
    // variable to store ID of client
    int c_id = -1;
    // variables to get server and client endpoint information
    ClientInfo c_info = null;
    ServerInfo s_info = null;
    // hash table that contains socket connections to other clients based on ClientIDs
    HashMap<Integer, ServerSockHandle> c_list = new HashMap<Integer, ServerSockHandle>();
    // hash table that contains socket connections to servers based on ServerIDs
    HashMap<Integer, ServerSockHandle> s_list = new HashMap<Integer, ServerSockHandle>();
    // to hold mutual exclusion algorithm instance
    mutexAlgorithm mutex = null;
    // variables to hold current client/server object
    CNode cnode = null;
    SNode snode = null;
    // simulation start issued flag ; for restart logic
    boolean issued_start = false;
    // constructor takes ID passed from command line from main()
    // listenSocket is called as part of starting up
    SNode(int c_id)
    {
        this.c_info = new ClientInfo();
        this.s_info = new ServerInfo();
        this.c_id = c_id;
        this.issued_start = false;
    	this.port = s_info.hmap.get(c_id).port;
        this.listenSocket();
        this.snode = this;
    }
    
    // CommandParser class is used to parse and execute respective commands that are entered via command line to SETUP/LIST/START/FINISH simulation
    public class CommandParser extends Thread
    {
      	// initialize patters for commands
    	Pattern LIST  = Pattern.compile("^LIST$");
    	Pattern START = Pattern.compile("^START$");
    	Pattern RESTART = Pattern.compile("^R$");
    	Pattern FINISH= Pattern.compile("^FINISH$");
    	
    	// read from inputstream, process and execute tasks accordingly	
    	int rx_cmd(Scanner cmd)
        {
    		String cmd_in = null;
    		if (cmd.hasNext())
    			cmd_in = cmd.nextLine();
    
    		Matcher m_START= START.matcher(cmd_in);
    		Matcher m_RESTART = RESTART.matcher(cmd_in);
    		Matcher m_LIST= LIST.matcher(cmd_in);
    		Matcher m_FINISH= FINISH.matcher(cmd_in);
    		
                // check the list of socket connections available on this client
                if(m_LIST.find())
                { 
                    synchronized (c_list)
                    {
                        System.out.println("\n=== Clients ===");
                        c_list.keySet().forEach(key -> {
                        System.out.println("key:"+key + " => ID " + c_list.get(key).remote_c_id);
                        });
                        System.out.println("=== size ="+c_list.size());
                    }
                    synchronized (s_list)
                    {
                        System.out.println("\n=== Servers ===");
                        s_list.keySet().forEach(key -> {
                        System.out.println("key:"+key + " => ID " + s_list.get(key).remote_c_id);
                        });
                        System.out.println("=== size ="+s_list.size());
                    }
    		}
                // start the random read/write simulation; sends trigger message to all clients
                else if(m_START.find())
                { 
                    start_or_restart();
    		}
                else if(m_RESTART.find())
                { 
                    start_or_restart();
    		}
                // command to close PROGRAM
                else if(m_FINISH.find())
                { 
    		    System.out.println("Closing connections and exiting program!");
                    synchronized (c_list)
                    {
                        c_list.keySet().forEach(key -> {
                            c_list.get(key).send_finish();
                        });
                    }
                    synchronized (s_list)
                    {
                        s_list.keySet().forEach(key -> {
                            s_list.get(key).send_finish();
                        });
                    }
                    randomDelay(0.7,0.9);
                    return 0;
    		}
    		// default message
    		else 
                {
    		    System.out.println("Unknown command entered : please enter a valid command");
    		}
    		
    		// to loop forever	
    		return 1;
    	}
    
    	public void run() {
    		System.out.println("Enter commands: SETUP / START to begin with");
    		Scanner input = new Scanner(System.in);
    		while(rx_cmd(input) != 0) { }  // to loop forever
    	}
    }

    // method to sent trigger message to all clients
    public void start_or_restart()
    {
        // running for first time
        if(!issued_start)
        {
            System.out.println("**************TRIGGER START Random READ/WRITE simulation");
            synchronized (c_list)
            {
                c_list.keySet().forEach(key -> {
                    c_list.get(key).send_start();
                });
            }
            System.out.println("**************TRIGGER FINISH Random READ/WRITE simulation");
            issued_start = true;
        }
        // consequent starts
        else
        {
            System.out.println("**************TRIGGER RESTART Random READ/WRITE simulation");
            mutex.reset_control();
            synchronized (c_list)
            {
                    c_list.get(1).send_reset();
            }
            System.out.println("**************TRIGGER RESTART FINISH Random READ/WRITE simulation");
        }
    }

    // method to initiate restart mechanish
    public void restart_simulation()
    {
        synchronized(mutex)
        {
            // reset the mutex algorithm
            mutex.reset_control();
        }

    }

    // send restart message to clients
    public void send_restart_message()
    {
        System.out.println("restart from server");
        synchronized (c_list)
        {
            c_list.keySet().forEach(key -> {
                c_list.get(key).send_restart();
            });
        }
    }

    // end program method, calls close on all socket instances and exits program
    public void end_program()
    {
        System.out.println("Received Termination message, Shutting down !");
        synchronized (c_list)
        {
            c_list.keySet().forEach(key -> {
                try
                {
                    c_list.get(key).client.close();
                }
                catch (IOException e) 
                {
                    System.out.println("No I/O");
                    e.printStackTrace(); 
                    System.exit(1);
                }
            });
        }
        synchronized (s_list)
        {
            s_list.keySet().forEach(key -> {
                try
                {
                    s_list.get(key).client.close();
                }
                catch (IOException e) 
                {
                    System.out.println("No I/O");
                    e.printStackTrace(); 
                    System.exit(1);
                }
            });
        }
        System.exit(1);
    }

    // create instance of mutex
    public void create_mutexAlgorithm()
    {
        mutex = new mutexAlgorithm(snode,cnode,c_id);
    }

    // method to keep track of client simulation finish counts
    public void increment_sim_finish_count()
    {
        int count = 0;
        synchronized(mutex)
        {
            ++mutex.sword.finish_sim_count;
            count = mutex.sword.finish_sim_count;
        }

        // all clients have finished running the simulation
        // initiate a message to Client 1 that relays to
        // all servers to print server stats
        if(count == 5)
        {
            c_list.get(1).send_finish_stat_collection();
        }
    }

    // print server stats
    public void print_stats()
    {
        String buf = "";
        buf += "\n=== STATS for entire simulation : ";
        synchronized(mutex)
        {
            int total_msgs = mutex.sword.total_msgs_tx+ mutex.sword.total_msgs_rx;
            buf +="\nNumber of messages sent = "+ mutex.sword.total_msgs_tx;
            buf +="\nNumber of messages received = "+ mutex.sword.total_msgs_rx;
            buf +="\nNumber of messages exchanged = "+ total_msgs;
        }
        buf += "\n=======================================";
        System.out.print(buf);
    }

    // delay generating method : time unit is ms
    // min is the least amount of delay guaranteed to happen
    // max is the randomness on top of this min value
    void randomDelay(double min, double max)
    {
        int random = (int)(max * Math.random() + min);
        try 
        {
            System.out.println("sleep for "+ random);
            Thread.sleep(random);
        } 
        catch (InterruptedException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // method to start server and listen for incoming connections
    public void listenSocket()
    {
        // create server socket on specified port number
        try
        {
            // create server socket and display host/addressing information of this node
            server = new ServerSocket(Integer.valueOf(port)); 
            System.out.println("ClientNode running on port " + port +"," + " use ctrl-C to end");
            myip = InetAddress.getLocalHost();
            ip = myip.getHostAddress();
            hostname = myip.getHostName();
            System.out.println("Your current IP address : " + ip);
            System.out.println("Your current Hostname : " + hostname);
        } 
        catch (IOException e) 
        {
            System.out.println("Error creating socket");
            System.exit(-1);
        }

	// create instance of commandparser thread and start it	
        // to get command line inputs from user
	CommandParser cmdpsr = new CommandParser();
	cmdpsr.start();
        

        // create thread to handle incoming connections
        Thread accept = new Thread() 
        {
            public void run()
            {
                while(true)
                {
                    try
                    {
                        Socket s = server.accept();
                        // ServerSockHandle instance with svr_hdl false and rx_hdl true as this is the socket listener
                	ServerSockHandle t = new ServerSockHandle(s,ip,port,c_id,c_list,s_list,true,false,snode);
                    }
                    catch (UnknownHostException e) 
		    {
		    	System.out.println("Unknown host");
		    	System.exit(1);
		    } 
		    catch (IOException e) 
		    {
		    	System.out.println("No I/O");
                        e.printStackTrace(); 
		    	System.exit(1);
		    }

                }
            }
        };
        accept.setDaemon(true);
        accept.setName("AcceptServer_"+c_id+"_ServerSockHandle_to_Server");
        accept.start();
    }
    
    public static void main(String[] args)
    {
    	// check for valid number of command line arguments
        // get server ID as argument
    	if (args.length != 1)
    	{
    	    System.out.println("Usage: java SNode <server-id>");
    	    System.exit(1);
    	}
    	SNode server = new SNode(Integer.valueOf(args[0]));
    }
}
