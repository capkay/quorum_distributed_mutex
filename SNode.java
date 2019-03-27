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
    // list of files updated after enquiring a random server
    List<String> files = new ArrayList<String>();
    mutexAlgorithm mutex = null;
    // handle to client object, ultimately self 
    CNode cnode = null;
    SNode snode = null;
    // constructor takes clientID passed from command line from main()
    // listenSocket is called as part of starting up
    SNode(int c_id)
    {
        this.c_info = new ClientInfo();
        this.s_info = new ServerInfo();
        this.c_id = c_id;
    	this.port = s_info.hmap.get(c_id).port;
        this.listenSocket();
        this.snode = this;
    }
    
    // CommandParser class is used to parse and execute respective commands that are entered via command line to SETUP/LIST/START/FINISH simulation
    public class CommandParser extends Thread
    {
      	// initialize patters for commands
    	Pattern SETUP = Pattern.compile("^SETUP$");
    	Pattern LIST  = Pattern.compile("^LIST$");
    	Pattern START = Pattern.compile("^START$");
    	Pattern FINISH= Pattern.compile("^FINISH$");
    	Pattern ENQUIRE= Pattern.compile("^ENQUIRE$");
    	
    	// read from inputstream, process and execute tasks accordingly	
    	int rx_cmd(Scanner cmd)
        {
    		String cmd_in = null;
    		if (cmd.hasNext())
    			cmd_in = cmd.nextLine();
    
    		Matcher m_START= START.matcher(cmd_in);
    		Matcher m_LIST= LIST.matcher(cmd_in);
    		Matcher m_SETUP= SETUP.matcher(cmd_in);
    		Matcher m_FINISH= FINISH.matcher(cmd_in);
    		Matcher m_ENQUIRE= ENQUIRE.matcher(cmd_in);
    		
                // perform setup connections, check for clientID 0
    		if(m_SETUP.find())
                { 
                    if( c_id == 1 )
                    {
                        //setup_connections();
                    }
                    else
                    {
                        System.out.println("Enter SETUP command on ServerID 1!");
                    }
    		}
                // do a manual enquiry of files present on server, check if files list is empty, then proceed
                else if(m_ENQUIRE.find())
                { 
                }
                // check the list of socket connections available on this client
                else if(m_LIST.find())
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
                // start the random read/write simulation to access critical section based on Ricart-Agrawala algorithm
                else if(m_START.find())
                { 
    		    System.out.println("**************TRIGGER START Random READ/WRITE simulation");
                    synchronized (c_list)
                    {
                        c_list.keySet().forEach(key -> {
                            c_list.get(key).send_start();
                        });
                    }
    		    System.out.println("**************TRIGGER FINISH Random READ/WRITE simulation");
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
    public synchronized void increment_clock()
    {

    }

    public synchronized void update_clock()
    {

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

    // method to create multiple instances of Ricart-Agrawala algorithm
    // save it to a hash corresponding to filenames
    public void create_mutexAlgorithm()
    {
        mutex = new mutexAlgorithm(snode,cnode,c_id);
    }

    // method to create delay based on inputs in seconds
    // adapted from stackOverflow
    void randomDelay(double min, double max)
    {
        int random = (int)(max * Math.random() + min);
        try 
        {
            Thread.sleep(random * 1000);
        } 
        catch (InterruptedException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void print_enquiry_results()
    {
	System.out.println("Files available:");
	for (int i = 0; i < files.size(); i++) {
		System.out.println(files.get(i));
	}
    }

    // method to setup connections to servers
    public void setup_servers()
    {
        if(c_id<=3)
        {
            // all 3 servers
            for(int i=2*c_id;i<=(2*c_id)+1;i++ )
            {
                // get the server IP and port info
                String t_ip = s_info.hmap.get(i).ip;
                int t_port = Integer.valueOf(s_info.hmap.get(i).port);
                Thread x = new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            Socket s = new Socket(t_ip,t_port);
                            // ServerSockHandle instance with svr_hdl true and rx_hdl false as this is the socket initiator
                            ServerSockHandle t = new ServerSockHandle(s,ip,port,c_id,c_list,s_list,false,true,snode);
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
                };

                x.setDaemon(true); 	// terminate when main ends
                x.setName("Server_"+c_id+"_ServerSockHandle_to_Server"+i);
                x.start(); 			// start the thread
            }
        }
        // another thread to check until all connections are established ( ie. socket list size =4 )
        // then send a message to my_id+1 client to initiate its connection setup phase
        Thread y = new Thread()
        {
            public void run()
            {
                int size = 0;
                int target = 0;
                if(c_id ==1)
                    target = 2;
                else if( (c_id == 2) | (c_id ==3))
                    target = 3;
                else 
                    target = 1;
	        System.out.println("connection setup target:"+target);
                // wait till client connections are setup
                while (size != target)
                {
                    synchronized(s_list)
                    {
                        size = s_list.size();
                    }
                }
	        System.out.println("connection setup target reached");
                // send chain init message to trigger connection setup
                // phase on the next client
                if (c_id <=3)
                {
	            System.out.println("chain setup init");
                    s_list.get((2*c_id)).send_setup();
                    s_list.get((2*c_id)+1).send_setup();
	            System.out.println("chain setup init");
                }
                if (c_id >3)
                {
                    s_list.keySet().forEach(key -> {
                        System.out.println("send setup finish:"+key + " => ID " + s_list.get(key).remote_c_id);
                        s_list.get(key).send_setup_finish();
                    });
                }
            }
        };
            
        y.setDaemon(true); 	// terminate when main ends
        y.start(); 			// start the thread
    }

    // method to setup connections to servers
    public void setup_clients()
    {
        // all 3 servers
        for(int i=1;i<=1;i++ )
        {
            // get the server IP and port info
            String t_ip = c_info.hmap.get(i).ip;
            int t_port = Integer.valueOf(c_info.hmap.get(i).port);
            Thread x = new Thread()
            {
                public void run()
                {
                    try
                    {
                        Socket s = new Socket(t_ip,t_port);
                        // ServerSockHandle instance with svr_hdl true and rx_hdl false as this is the socket initiator
                        ServerSockHandle t = new ServerSockHandle(s,ip,port,c_id,c_list,s_list,false,true,snode);
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
            };

            x.setDaemon(true); 	// terminate when main ends
            x.setName("Server_"+c_id+"_ServerSockHandle_to_Client"+i);
            x.start(); 			// start the thread
        }
        // another thread to check until all connections are established ( ie. socket list size =4 )
        // then send a message to my_id+1 client to initiate its connection setup phase
        Thread y = new Thread()
        {
            public void run()
            {
                int size = 0;
                int target = 1;
	        System.out.println("client connection setup target:"+target);
                // wait till client connections are setup
                while (size != target)
                {
                    synchronized(s_list)
                    {
                        size = s_list.size();
                    }
                }
	        System.out.println("client connection setup target reached");
                // send chain init message to trigger connection setup
                // if this is not the last client node (ID =4)
                // send chain init message to trigger connection setup
                // phase on the next client
                if(c_id != 7)
                {
                    s_list.get(c_id+1).send_setup();
                    System.out.println("chain setup init");
                }
                // send the setup finish, from Client 4
                // indicating connection setup phase is complete
                else
                {
                    s_list.get(1).send_setup_finish();
                }
                // phase on the next client
            }
        };
            
        y.setDaemon(true); 	// terminate when main ends
        y.start(); 			// start the thread
    }

    // method encompasses both server and client connection setup
    public void setup_connections()
    {
        //setup_servers();
        setup_clients();
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
        // get client ID as argument
    	if (args.length != 1)
    	{
    	    System.out.println("Usage: java SNode <server-id>");
    	    System.exit(1);
    	}
    	SNode server = new SNode(Integer.valueOf(args[0]));
    }
}