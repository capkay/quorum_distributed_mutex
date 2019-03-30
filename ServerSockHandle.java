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
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

// ServerSockHandle class, to handle each socket connection (to/from client/server)
class ServerSockHandle
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
    // hash table handles to hold all client and server socket connections
    HashMap<Integer, ServerSockHandle> c_list = null;
    HashMap<Integer, ServerSockHandle> s_list = null;

    // boolean flag: functionality change if this is created by a listening node
    boolean rx_hdl = false;
    // boolean flag: functionality change if this is a connection to a server node
    boolean svr_hdl = false;
    // handle to client object, to trigger some client methods
    SNode snode = null;
    // generic 'end of message' pattern 
    public static Pattern eom = Pattern.compile("^EOM");
    
    // constructor to connect respective variables
    ServerSockHandle(Socket client,String my_ip,String my_port,int my_c_id,HashMap<Integer, ServerSockHandle> c_list,HashMap<Integer, ServerSockHandle> s_list, boolean rx_hdl,boolean svr_hdl,SNode snode) 
    {
    	this.client  = client;
    	this.my_ip = my_ip;
    	this.my_port = my_port;
        this.my_c_id = my_c_id;
        this.remote_c_id = remote_c_id;
        this.c_list = c_list;
        this.s_list = s_list;
        this.rx_hdl = rx_hdl;
        this.svr_hdl = svr_hdl;
        this.snode = snode;
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
            // send a initial_setup message to the initiator node (like an acknowledgement message)
            // and get some information from the remote initiator node
            if(rx_hdl == true)
            {
    	        System.out.println("send cmd 1: setup socket to other client");
                out.println("initial_setup_client");
                ip = in.readLine();
    	        System.out.println("ip:"+ip);
                port=in.readLine();
    	        System.out.println("port:"+port);
                remote_c_id=Integer.valueOf(in.readLine());
    	        out.println(my_ip);
    	        out.println(my_port);
    	        out.println(my_c_id);
    	        System.out.println("neighbor connection server rx_hdl, PID:"+ Integer.toString(remote_c_id)+ " ip:" + ip + " port = " + port);
                // when this handshake is done
                // add this object to the socket handle list as part of the main Client object
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
        read.setName("rx_cmd_"+my_c_id+"_ServerSockHandle_to_Server"+svr_hdl);
        read.start();		// start the thread	
    }

    // method to process received request message ( part of Ricard-Agrawala algorithm )
    // takes received sequence number, received ID, filename
    public void process_request_message(int their_sn,int j)
    {
        synchronized(snode.mutex)
        {
            if(!snode.mutex.sword.locked & snode.mutex.sword.queue.isEmpty())
            {
	        System.out.println("server already unlocked and queue is empty");
                // first request
                snode.mutex.sword.locked = true;
                snode.mutex.sword.timestamp = snode.mutex.sword.timestamp + 1;
                snode.mutex.sword.timestamp = Math.max(their_sn+1,snode.mutex.sword.timestamp);
                // send GRANT message
                crit_reply(snode.mutex.sword.timestamp);
            }
            else
            {
	        System.out.println("adding request to queue");
                // add to queue
                RequestData t = new RequestData(their_sn,j);
                snode.mutex.sword.queue.add(t);
            }
        }
    }

    // method to process received reply message ( part of Ricard-Agrawala algorithm )
    // takes received ID, filename
    public void process_release_message(int their_sn,int j)
    {
        synchronized(snode.mutex)
        {
            snode.mutex.sword.timestamp = snode.mutex.sword.timestamp + 1;
            snode.mutex.sword.timestamp = Math.max(their_sn+1,snode.mutex.sword.timestamp);
            if(snode.mutex.sword.queue.isEmpty() & snode.mutex.sword.locked)
            {
                snode.mutex.sword.locked = false;
	        System.out.println("server unlocked");
            }
            else
            {
	        System.out.println("server sending reply from pending queue");
                // pop from queue
                RequestData t = snode.mutex.sword.queue.poll();
                // send grant to that PID
                c_list.get(t.id).crit_reply(snode.mutex.sword.timestamp);
            }
        }
    }
    // methods to send setup related messages in the output stream
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
    // methods to send setup related messages in the output stream
    public void send_start()
    {
	System.out.println("send_start to:"+remote_c_id);
        out.println("start_simulation");
    }
    // methods to send setup related messages in the output stream
    public void send_setup()
    {
	System.out.println("chain_setup to:"+remote_c_id);
        out.println("chain_setup");
    }

    public void send_finish()
    {
        out.println("simulation_finish");
    }

    public void send_setup_finish()
    {
	System.out.println("setup_finish to:"+remote_c_id);
        out.println("chain_setup_finish");
    }

    // method to send the REQUEST message with timestamp and file identifier
    public synchronized void crit_request(int ts)
    {
        out.println("REQUEST");
        out.println(ts);
        out.println(my_c_id);
    }

    // method to send the REPLY message with file identifier
    public synchronized void crit_reply(int ts)
    {
	System.out.println("GRANT to "+remote_c_id);
        out.println("GRANT");
        out.println(ts);
        out.println(my_c_id);
    }

    // method to process incoming commands and data associated with them
    public int rx_cmd(BufferedReader cmd,PrintWriter out)
    {
    	try
    	{
            // get blocked in readLine until something actually comes on the inputStream
            // then perform actions based on the received command
    	    String cmd_in = cmd.readLine();
            // initial_setup sequence to populate the client socket list stored by the client
    	    if(cmd_in.equals("initial_setup_server"))
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
    	        System.out.println("neighbor connection server not rx_hdl, PID:"+ Integer.toString(remote_c_id)+ " ip:" + ip + " port = " + port);
                synchronized (s_list)
                {
                    s_list.put(remote_c_id,this);
                }
    	    } 
            // initial_setup_server sequence to populate the server socket list stored by the client
            else if(cmd_in.equals("initial_setup_client"))
            {
                System.out.println("got cmd 1 from server");
                out.println(my_ip);
                out.println(my_port);
                out.println(my_c_id);
                ip = in.readLine();
                System.out.println("ip:"+ip);
                port=in.readLine();
                System.out.println("port:"+port);
                remote_c_id=Integer.valueOf(in.readLine());
                System.out.println("server connection client, PID:"+ Integer.toString(remote_c_id)+ " ip:" + ip + " port = " + port);
                synchronized (c_list)
                {
                    c_list.put(remote_c_id,this);
                }
                // if this is a server handle / connected to the server
                // no need to run this rx_cmd method
                // so return from this method
                if(svr_hdl == true)
                {
                    System.out.println("rx_cmd processing finished");
                    return 0;
                }
            }
            // initiate connection setup once this message is received
            else if(cmd_in.equals("chain_setup"))
            {
    	        System.out.println("chain_setup ");
                snode.setup_connections();
            }
            // perform enquiry and create the Ricart-Agrawala instances after this message
            else if(cmd_in.equals("chain_setup_finish"))
            {
    	        System.out.println("connection setup finished");
                snode.create_mutexAlgorithm();
            }
            // to terminate the program
            else if(cmd_in.equals("simulation_finish"))
            {
    	        //System.out.println("Finish program execution!");
                //snode.end_program();
                //snode.print_stats();
                //return 0;
            }
            else if(cmd_in.equals("reset_simulation"))
            {
    	        System.out.println("reset from "+remote_c_id);
                snode.restart_simulation();
                out.println("reset_done");
            }
            else if(cmd_in.equals("reset_done"))
            {
    	        System.out.println("reset_done from "+remote_c_id);
                snode.send_restart_message();
            }
            // got a REQUEST message, process it
            else if(cmd_in.equals("REQUEST"))
            {
                int ts = Integer.valueOf(in.readLine());
                int pid = Integer.valueOf(in.readLine());
    	        System.out.println("REQUEST received from PID "+pid+" with timestamp "+ts);
                process_request_message(ts,pid);
            }
            // got a REPLY message, process it
            else if(cmd_in.equals("RELEASE"))
            {
                int ts = Integer.valueOf(in.readLine());
                int pid = Integer.valueOf(in.readLine());
    	        System.out.println("RELEASE received from PID "+pid);
                process_release_message(ts,pid);
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
