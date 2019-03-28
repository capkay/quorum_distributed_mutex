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

// ClientSockHandle class, to handle each socket connection to all clients
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
        out.println("GRANT");
        out.println(ts);
        out.println(my_c_id);
    }

    // method to send the REPLY message with file identifier
    public synchronized void crit_release(int ts)
    {
        out.println("RELEASE");
        out.println(ts);
        out.println(my_c_id);
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
    // method to process received reply message ( part of Ricard-Agrawala algorithm )
    // takes received ID, filename
    public void process_reply_message(int their_sn,int j)
    {
        //System.out.println("process reply message");
        synchronized(cnode.mutex)
        {
            cnode.mutex.sword.timestamp = cnode.mutex.sword.timestamp + 1;
            cnode.mutex.sword.timestamp = Math.max(their_sn+1,cnode.mutex.sword.timestamp);
            ++cnode.mutex.sword.replies_received;
            ++cnode.mutex.sword.crit_msgs_rx;
            //System.out.println("replies received "+ cnode.mutex.sword.replies_received);
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
            else if(cmd_in.equals("start_simulation"))
            {
    	        System.out.println("Trigger start from server!");
                cnode.start_simulation();
            }
            // perform the read operation for the requested file
            else if(cmd_in.equals("READ"))
            {
                String filename = in.readLine();
                String content = cnode.do_read_operation(filename);
                out.println(content);
            }
            // perform the write operation for the requested file
            else if(cmd_in.equals("WRITE"))
            {
                String filename = in.readLine();
                String content = in.readLine();
                cnode.do_write_operation(filename,content);
                out.println("EOM");
            }
            // got a REPLY message, process it
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
