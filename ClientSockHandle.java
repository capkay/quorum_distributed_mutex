import java.util.Date;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedList;
import java.lang.management.*;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.text.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

// ClientSockHandle class, to handle each socket connection to all servers
class ClientSockHandle
{
    // to send data out
    PrintWriter out = null;
    // to read incoming data
    BufferedReader in = null;
    // socket instance for the connecting client
    Socket client;
    // variables to obtain IP/port information of where the node is running
    String ip = null; 			// remote ip address
    String port = null; 		// remote port 
    String my_ip = null; 		// ip of this node
    String my_port = null; 		// port of this node
    // variable to store ID of owning client
    int my_c_id = -1;
    // variable to store ID of remote client
    int remote_c_id = -1;
    // hash table handles to hold all client connections
    HashMap<Integer, ClientSockHandle> c_list = null;
    // boolean flag: functionality change if this is created by a listening node
    boolean rx_hdl = false;
    // handle to server object, to trigger some server methods
    CNode cnode = null;
    // generic 'end of message' pattern 
    public static Pattern eom = Pattern.compile("^EOM");  // generic 'end of message' pattern 
    
    // constructor to connect respective variables
    ClientSockHandle(Socket client,String my_ip,String my_port,int my_c_id,HashMap<Integer, ClientSockHandle> c_list, boolean rx_hdl,CNode cnode) 
    {
    	this.client  = client;
    	this.my_ip = my_ip;
    	this.my_port = my_port;
        this.my_c_id = my_c_id;
        this.remote_c_id = remote_c_id;
        this.c_list = c_list;
        this.rx_hdl = rx_hdl;
        this.cnode = cnode;
        // get input and output streams from socket
    	try 
    	{
    	    in = new BufferedReader(new InputStreamReader(client.getInputStream()));
    	    out = new PrintWriter(client.getOutputStream(), true);
    	} 
    	catch (IOException e) 
    	{
    	    System.out.println("in or out failed");
    	    System.exit(-1);
    	}
        try
        {
            // only when this is started from a listening node
            // send a initial_setup_server message to the initiator node (like an acknowledgement message)
            // and get some information from the remote initiator node
            if(rx_hdl == true)
            {
    	        System.out.println("send cmd 1: setup socket to client");
                out.println("initial_setup_server");
                ip = in.readLine();
    	        System.out.println("ip:"+ip);
                port=in.readLine();
    	        System.out.println("port:"+port);
                remote_c_id=Integer.valueOf(in.readLine());
    	        out.println(my_ip);
    	        out.println(my_port);
    	        out.println(my_c_id);
    	        System.out.println("neighbor client connection, PID:"+ Integer.toString(remote_c_id)+ " ip:" + ip + " port = " + port);
                // when this handshake is done
                // add this object to the socket handle list as part of the main server object
                synchronized (c_list)
                {
                    c_list.put(remote_c_id,this);
                }
            }
        }
    	catch (IOException e)
    	{
    	    System.out.println("Read failed");
    	    System.exit(1);
    	}
    	// handle unexpected connection loss during a session
    	catch (NullPointerException e)
    	{
    	    System.out.println("peer connection lost");
    	    System.exit(1);
    	}
        // thread that continuously runs and waits for incoming messages
        // to process it and perform actions accordingly
    	Thread read = new Thread()
        {
    	    public void run()
            {
    	        while(rx_cmd(in,out) != 0) { }
            }
    	};
    	read.setDaemon(true); 	// terminate when main ends
        read.start();		// start the thread	
    }

    // methods to send setup related messages in the output stream
    public void send_setup()
    {
	System.out.println("chain_setup to:"+remote_c_id);
        out.println("chain_setup");
    }
    public void send_restart_deadlock()
    {
	System.out.println("restart_deadlock to:"+remote_c_id);
        out.println("restart_deadlock");
    }
    public void send_restart()
    {
	System.out.println("send_restart to:"+remote_c_id);
        out.println("restart_simulation");
    }
    public void send_reset()
    {
	System.out.println("send_reset to:"+remote_c_id);
        out.println("reset_simulation");
    }
    public void send_finish()
    {
	System.out.println("simulation_finish to:"+remote_c_id);
        out.println("simulation_finish");
    }

    public void send_setup_finish()
    {
	System.out.println("setup_finish to:"+remote_c_id);
        out.println("chain_setup_finish");
    }
    public void send_reset_done()
    {
	System.out.println("send_reset_done to:"+remote_c_id);
        out.println("reset_done");
    }
    public void send_stat_collection()
    {
	System.out.println("send_stat_collection to:"+remote_c_id);
        out.println("stat_collection");
    }
    
    // method to send the REQUEST message with timestamp
    public synchronized void crit_request(int ts)
    {
        out.println("REQUEST");
        out.println(ts);
        out.println(my_c_id);
    }

    // method to send the REPLY message with timestamp
    public synchronized void crit_reply(int ts)
    {
        out.println("GRANT");
        out.println(ts);
        out.println(my_c_id);
    }

    // method to send the RELEASE message with timestamp
    public synchronized void crit_release(int ts)
    {
        out.println("RELEASE");
        out.println(ts);
        out.println(my_c_id);
    }


    // increment the reset count to make sure all servers are reset
    public void process_reset_message()
    {
        synchronized(cnode.mutex)
        {
            ++cnode.mutex.sword.reset_count;
        }
    }

    // method to process received reply message
    // takes received ID and timestamp
    public void process_reply_message(int their_sn,int j)
    {
        int msgs = 0;
        int target= 0;
        synchronized(cnode.mutex)
        {
            // update logical clock 
            cnode.mutex.sword.timestamp = cnode.mutex.sword.timestamp + 1;
            cnode.mutex.sword.timestamp = Math.max(their_sn+1,cnode.mutex.sword.timestamp);
            // update and record the stats
            ++cnode.mutex.sword.replies_received;
            ++cnode.mutex.sword.crit_msgs_rx;
            msgs = cnode.mutex.sword.replies_received;
            target = cnode.mutex.sword.target_reply_count;
            //System.out.println("replies received "+ cnode.mutex.sword.replies_received);
        }

        // enter critical section when required number of replies are received
        if(target == msgs)
        {
            cnode.enter_crit_release();
        }
    }

    // method to process incoming commands and data associated with them
    public int rx_cmd(BufferedReader cmd,PrintWriter out)
    {
    	try
    	{
            // get blocked in readLine until something actually comes on the inputStream
            // then perform actions based on the received command
    	    String cmd_in = cmd.readLine();
            // initial_setup_server sequence to populate the client socket list stored by the server
    	    if(cmd_in.equals("initial_setup_client"))
            {
    	        System.out.println("got cmd 1");
    	        out.println(my_ip);
    	        out.println(my_port);
    	        out.println(my_c_id);
                ip = in.readLine();
    	        System.out.println("ip:"+ip);
                port=in.readLine();
    	        System.out.println("port:"+port);
                remote_c_id=Integer.valueOf(in.readLine());
    	        System.out.println("server connection, PID:"+ Integer.toString(remote_c_id)+ " ip:" + ip + " port = " + port);
                synchronized (c_list)
                {
                    c_list.put(remote_c_id,this);
                }
    	    }
            // to terminate the program
            else if(cmd_in.equals("simulation_finish"))
            {
    	        System.out.println("Finish program execution!");
                cnode.end_program();
                return 0;
            }
            // to start simulation
            else if(cmd_in.equals("start_simulation"))
            {
    	        System.out.println("start_simulation from server!");
                cnode.start_simulation();
            }
            // to restart simulation
            else if(cmd_in.equals("restart_simulation"))
            {
    	        System.out.println("restart simulation from server!");
                cnode.restart_simulation();
            }
            // to process rese_done messages; part of restarting simulation
            else if(cmd_in.equals("reset_done"))
            {
    	        System.out.println("reset done from server!");
                process_reset_message();
            }
            // to initiate the simulation restart feature
            else if(cmd_in.equals("reset_simulation"))
            {
    	        System.out.println("reset simulation from server!");
                cnode.reset_simulation();
            }
            // to enable stat collection at the end of all simulations
            else if(cmd_in.equals("finish_stat_collection"))
            {
    	        System.out.println("trigger stat collection finish server!");
                cnode.trigger_stat_collection();
            }
            // got a GRANT message, process it
            else if(cmd_in.equals("GRANT"))
            {
                int ts = Integer.valueOf(in.readLine());
                int pid = Integer.valueOf(in.readLine());
    	        System.out.println("GRANT received from PID "+pid);
                process_reply_message(ts,pid);
            }
    	}
    	catch (IOException e) 
    	{
    	    System.out.println("Read failed");
    	    System.exit(-1);
    	}
    
    	// default : return 1, to continue processing further commands 
    	return 1;
    }
}
