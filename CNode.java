import java.util.Date;
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
import java.security.MessageDigest;
import java.text.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

// CNode class : handles connections from all client nodes 
class CNode
{
    // server socket variables 
    ServerSocket server = null;
    // variables to obtain IP/port information of where the node is running
    InetAddress myip = null;
    String hostname = null;
    String ip = null;
    String port = null;
    // variable to store ID of server
    int c_id = -1;
    // variables to get server endpoint information
    ServerInfo s_info = null;
    ClientInfo c_info = null;
    // hash table that contains socket connections from clients based on client IDs
    HashMap<Integer, ClientSockHandle> s_list = new HashMap<Integer, ClientSockHandle>();
    // to hold mutual exclusion algorithm instance
    mutexAlgorithm mutex = null;
    // variables to hold current client/server object
    SNode snode = null;
    CNode cnode = null;
    // common file to all clients
    String FILE = "mutex.log";
    // logfile
    String LOGFILE = null;
    // minimum delay in ms
    double crit_a = 5;
    // random delay upto this value that is added to minimum delay
    double crit_b = 10;
    // delay inside critical section
    int rel_delay  = 3;
    // constructor takes ClientID passed from command line from main()
    // listenSocket is called as part of starting up
    CNode(int c_id)
    {
        this.c_info = new ClientInfo();
        this.s_info = new ServerInfo();
        this.c_id = c_id;
    	this.port = c_info.hmap.get(c_id).port;
        this.listenSocket();
        this.cnode = this;
        this.setup_servers();
        this.crit_a = 5;
        this.crit_b = 10;
        this.rel_delay  = 3;
        this.LOGFILE = "C"+c_id + ".log";
        this.clearTheFile(this.FILE);
        this.clearTheFile(this.LOGFILE);
    }

    // CommandParser class is used to parse and execute respective commands that are entered via command line to SETUP/LIST/START/FINISH simulation
    public class CommandParser extends Thread
    {
      	// initialize patters for commands
    	Pattern LIST  = Pattern.compile("^LIST$");
    	Pattern FINISH= Pattern.compile("^FINISH$");
    	Pattern START = Pattern.compile("^START$");
    	Pattern CRIT = Pattern.compile("^CRIT (\\d+) (\\d+)$");
    	Pattern REL  = Pattern.compile("^REL (\\d+)$");
    	
    	// read from inputstream, process and execute tasks accordingly	
    	int rx_cmd(Scanner cmd)
        {
    		String cmd_in = null;
    		if (cmd.hasNext())
    			cmd_in = cmd.nextLine();
    
    		Matcher m_LIST= LIST.matcher(cmd_in);
    		Matcher m_START= START.matcher(cmd_in);
    		Matcher m_FINISH= FINISH.matcher(cmd_in);
    		Matcher m_CRIT = CRIT.matcher(cmd_in);
    		Matcher m_REL  = REL.matcher(cmd_in);
    		
                // print the list of files and
                // check the list of socket connections available on this server
                if(m_LIST.find())
                { 
                    synchronized (s_list)
                    {
                        System.out.println("\n=== Connections to servers ===");
                        s_list.keySet().forEach(key -> {
                        System.out.println("key:"+key + " => ID " + s_list.get(key).remote_c_id);
                        });
                        System.out.println("=== size ="+s_list.size());
                    }
                    synchronized(mutex)
                    {
                        System.out.println("\n=== mutex ===");
                        System.out.println("timestamp ="+mutex.sword.timestamp);
                        System.out.println("target_reply_count="+mutex.sword.target_reply_count);
                        System.out.println("replies_received="+mutex.sword.replies_received);
                        System.out.println("locked ="+mutex.sword.locked);
                    }
                    System.out.println("crit_a="+crit_a);
                    System.out.println("crit_b="+crit_b);
                    System.out.println("rel_delay="+rel_delay);
    		}
                else if(m_START.find())
                { 
                    start_simulation();
                }
                else if(m_FINISH.find())
                { 
    		}
                else if(m_CRIT.find())
                { 
                    crit_a = Double.parseDouble(m_CRIT.group(1));
                    System.out.println("crit_a="+crit_a);
                    crit_b = Double.parseDouble(m_CRIT.group(2));
                    System.out.println("crit_b="+crit_b);
    		}
                else if(m_REL.find())
                { 
                    rel_delay = Integer.valueOf(m_REL.group(1));
                    System.out.println("rel_delay="+rel_delay);
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
    		System.out.println("Enter commands: LIST/FINISH");
    		Scanner input = new Scanner(System.in);
    		while(rx_cmd(input) != 0) { }  // to loop forever
    	}
    }

    // method to clear the contents of the file when starting up
    // adapted from StackOverflow
    public void clearTheFile(String filename) {
        try
        {
            FileWriter fwOb = new FileWriter("./"+filename, false); 
            PrintWriter pwOb = new PrintWriter(fwOb, false);
            pwOb.write("");
            pwOb.flush();
            pwOb.close();
            fwOb.close();
        }
        catch (FileNotFoundException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // logic ot reset simulation
    public void reset_simulation()
    {
        if(c_id == 1)
        {
            clearTheFile(this.FILE);
            for(int j=2;j<8;j++)
            {
                System.out.println("RESET sent to "+ j);
                // send the reset message to servers
                cnode.s_list.get(j).send_reset();
            }
            int t = 0;
            // wait till all other servers have been reset
            System.out.println("wait for reset complete in servers");
            while (t!=6)
            {
                synchronized(mutex)
                {
                    t = mutex.sword.reset_count;
                }
            }
            System.out.println("finished wait for reset complete in servers");
            // send reset done; which if followed by restart_simulation from Server 1
            cnode.s_list.get(1).send_reset_done();
        }
    }

    // restart simulation by exiting current thread and then spawn simulation again
    public void restart_simulation()
    {

    	System.out.println("**************Starting to RESTART");
        boolean waiting = false;
        synchronized(mutex)
        {
            // set restart flag to true to enable current thread to break out
            mutex.sword.restart = true;
            // resume the current thread from its wait
            mutex.notifyAll();
            waiting = mutex.sword.waiting;
        }
        // if deadlocked; waiting would be true
        // wait till the older simulation thread is finished
        if(waiting)
        {
            boolean r = true;
            while (r)
            {
                synchronized(mutex)
                {
                    r = mutex.sword.restart;
                }
            }
        }
        // simulation had successfully finished; in this case just start the simulation again
        else
        {
            synchronized(mutex)
            {
                // reset mutex variables
                mutex.reset_control();
            }
        }
    	System.out.println("**************ENTERING START AGAIN");
        // call start simulation
        start_simulation();
    }

    public void start_simulation()
    {
        clearTheFile(this.LOGFILE);
        Thread x = new Thread()
        {
            public void run()
            {
                // flag to check if need to end the thread
                boolean breaker = false;
    	        System.out.println("**************START Random READ/WRITE simulation");
                for(int i=0;i<20;i++)
                {
                    synchronized(mutex)
                    {
                        // mark current system time to measure latency
                        mutex.sword.start_time = System.currentTimeMillis();
                    }
                        
                    // wait before trying to enter the critical section
                    randomDelay(crit_a,crit_b);
                    System.out.println("**************Iteration : "+(i+1)+" of simulation.");
                    request_crit_section();

                    synchronized(mutex)
                    {
                        if(mutex.sword.waiting)
                        {
                            try
                            {
                                // release lock and wait till all replies are received
                                mutex.wait();
                            }
                            catch (InterruptedException e)  
                            {
                                System.out.println("interrupt");
                                Thread.currentThread().interrupt(); 
                            }
                        }
                        // restart if needed
                        if(mutex.sword.restart)
                        {
                            System.out.println("**************RESTARTING SIMULATION");
                            mutex.reset_control();
                            breaker = true;
                            break;
                        }
                    }
                    // print stats for current iteration of simulation
                    print_crit_stats(i);
                }
    	        System.out.println("**************FINISH Random READ/WRITE simulation");
                if (!breaker)
                {
                    // send trigger message to Server 1 denoting finish of simulation
                    s_list.get(1).send_finish();
                    // print entire simulation stats
                    print_sim_stats();
                }
            }
        };

        x.setDaemon(true); 	// terminate when main ends
        x.setName("Client_"+c_id+"_simulation");
        x.start(); 			// start the thread
    }

    // method that initiates critical section request
    public void request_crit_section()
    {
        System.out.println("\n=== Initiate REQUEST ===");
        // choose a random quorum
        int randQ = (int)( (s_info.quorums.size()) * Math.random() + 0);
        // call request_resource
        mutex.request_resource(randQ);
        // check for restart flag
        synchronized(mutex)
        {
            if(mutex.sword.restart)
            {
                System.out.println("**************RESTARTING SIMULATION --> break out from request crit section");
                return;
            }
        }
    }

    public void enter_crit_release()
    {
        System.out.println("Entering critical section of client "+ c_id);
        // write to file
        do_write_operation(this.FILE);
        System.out.println("Finished critical section of client "+ c_id);
        // call release_resource 
        mutex.release_resource();
    }

    // stat collection; display and log to file; for entire simulation
    public void print_sim_stats()
    {
        String buf = "";
        buf += "\n=== STATS for entire simulation ===";
        synchronized(mutex)
        {
            buf += "\nNumber of messages sent = "+ mutex.sword.total_msgs_tx;
            buf += "\nNumber of messages received = "+ mutex.sword.total_msgs_rx;
        }
        buf += "\n=======================================";
        System.out.print(buf);
        writeToFile(this.LOGFILE,buf);
    }

    // stat collection; display and log to file; for current iteration of simulation
    public void print_crit_stats(int i)
    {
        String buf = "";
        buf += "\n=== STATS for this critical section iteration : "+(i+1);
        synchronized(mutex)
        {
            int total_msgs = mutex.sword.crit_msgs_rx + mutex.sword.crit_msgs_tx;
            buf +="\nNumber of messages exchanged = "+ total_msgs;
            buf +="\nElapsed time (latency in ms) = "+ mutex.sword.crit_elapsed_time;
        }
        buf += "\n=======================================";
        System.out.print(buf);
        writeToFile(this.LOGFILE,buf);
    }

    // send message to all servers to print their stats at the end of all simulations
    // Server 1 sends message to Client 1 indicating computation has terminated
    // thus Client 1 informs all other servers to print their STATS
    public void trigger_stat_collection()
    {
        if(c_id == 1)
        {
            for(int j=1;j<=7;j++)
            {
                System.out.println("STATCOLLECTION sent to "+ j);
                // send the trigger message
                cnode.s_list.get(j).send_stat_collection();
            }
        }
    }

    // delay generating method : time unit is ms
    // min is the least amount of delay guaranteed to happen
    // max is the randomness on top of this min value
    void randomDelay(double min, double max)
    {
        int random =  (int)(max * Math.random() + min) ;
        try 
        {
            System.out.println("sleep for "+random);
            Thread.sleep(random);
        } 
        catch (InterruptedException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // sleep for fixed time
    void sleep_ms(int val)
    {
        try 
        {
            System.out.println("sleep for "+val);
            Thread.sleep(val);
        } 
        catch (InterruptedException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // end program method, calls close on all socket instances and exits program
    public void end_program()
    {
        System.out.println("Received Termination message, Shutting down !");
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
                }
            });
        }
        System.exit(1);
    }

    // helper method for logging purposes
    public void writeToFile(String filename, String content)
    {
        // directory is based on serverID
        File file = new File("./"+filename);
	if (!file.exists()) 
        {
	    System.out.println("File "+filename+" does not exist");
            return;
	}
        try
        {
            // write / append to the file
            FileWriter fw = new FileWriter(file, true);
            fw.write(content+"\n");
            fw.close();
        }
        catch (FileNotFoundException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // write to common file when in critical section
    // stay in critical section for REL_DELAY amount of time
    public void do_write_operation(String filename)
    {
        // directory is based on serverID
        File file = new File("./"+filename);
	if (!file.exists()) 
        {
	    System.out.println("File "+filename+" does not exist");
            return;
	}
        int timestamp = 0;
        synchronized(mutex)
        {
            timestamp = mutex.sword.timestamp;
        }
        try
        {
            // write / append to the file
            FileWriter fw = new FileWriter(file, true);
            fw.write("Client "+c_id+" entering; logical clk="+timestamp+" physical time ="+System.currentTimeMillis()+"\n");
            sleep_ms(this.rel_delay);
            fw.close();
        }
        catch (FileNotFoundException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // method to setup connections to servers
    // called when starting the CLient
    public void setup_servers()
    {
        // all 7 servers
        for(int i=1;i<=7;i++ )
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
                	ClientSockHandle t = new ClientSockHandle(s,ip,port,c_id,s_list,false,cnode);
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
            x.setName("Client_"+c_id+"_ClientSockHandle_to_Server"+i);
            x.start(); 			// start the thread
        }

        // another thread to check until all connections are established ( ie. socket list size =7 )
        // then initialize the mutexAlgorithm instance to hand over all object handles(sockets to servers)
        Thread y = new Thread()
        {
            public void run()
            {
                int size = 0;
                int target = 7;
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
                // create mutex
                create_mutexAlgorithm();
                // send trigger message to enable servers to create instance 
                // of mutex as Client 5 is supposed to be started at the end
                if (c_id == 5)
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

    // create instance of mutex
    public void create_mutexAlgorithm()
    {
        mutex = new mutexAlgorithm(snode,cnode,c_id);
    }


    // method to start server and listen for incoming connections
    public void listenSocket()
    {
        // create server socket on specified port number
        try
        {
            // create server socket and display host/addressing information of this node
            server = new ServerSocket(Integer.valueOf(port)); 
            System.out.println("CNode running on port " + port +"," + " use ctrl-C to end");
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
                        // ServerSockHandle instance with rx_hdl true as this is the socket listener
                	    ClientSockHandle t = new ClientSockHandle(s,ip,port,c_id,s_list,true,cnode);
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
        accept.start();
    }
    
    public static void main(String[] args)
    {
    	// check for valid number of command line arguments
        // get server ID as argument
    	if (args.length != 1)
    	{
    	    System.out.println("Usage: java CNode <client-id>");
    	    System.exit(1);
    	}
    	CNode server = new CNode(Integer.valueOf(args[0]));
    }
}
