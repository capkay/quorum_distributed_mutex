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
    	        System.out.println("send cmd 1: setup socket to other server");
                out.println("initial_setup_server");
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
                synchronized (s_list)
                {
                    s_list.put(remote_c_id,this);
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
    public void process_request_message(int their_sn,int j, String filename)
    {
    }

    // method to process received reply message ( part of Ricard-Agrawala algorithm )
    // takes received ID, filename
    public void process_reply_message(int j, String filename)
    {
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
    public synchronized void crit_request(int ts,String filename)
    {
        out.println("REQUEST");
        out.println(ts);
        out.println(my_c_id);
        out.println(filename);
    }

    // method to send the REPLY message with file identifier
    public synchronized void crit_reply(String filename)
    {
        out.println("REPLY");
        out.println(my_c_id);
        out.println(filename);
    }

    // method to enquire file to the remote server and update shared files list on client
    public void enquire_files()
    {
        out.println("ENQUIRY");
        String rd_in = null;
        Matcher m_eom = eom.matcher("start");  // initializing the matcher. "start" does not mean anything
        // get filenames till EOM message is received and update the files list
        try
        {
            while(!m_eom.find())
            {
                rd_in = in.readLine();
                m_eom = eom.matcher(rd_in);
                if(!m_eom.find())
                {
                    String filename = rd_in;
                    snode.files.add(filename);
                } 
                else { break; }  // break out of loop when EOM is received
            }
        }
        catch (IOException e) 
        {
        	System.out.println("Read failed");
        	System.exit(-1);
        }
    }

    // method to send read_file command
    // print the Content on the console
    public void read_file(String filename)
    {
        out.println("READ");
        out.println(filename);
        try
        {
            String content = null;
            content = in.readLine();
            System.out.println("READ content : "+content);
        }
        catch (IOException e) 
        {
        	System.out.println("Read failed");
        	System.exit(-1);
        }
    }

    // method to send write_file command
    public void write_file(String filename)
    {
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
                if ( (my_c_id == 2) | (my_c_id ==3) )
                {
                    s_list.get(1).send_setup_finish();
                }
                //snode.initiate_enquiry();
                //snode.create_RAlgorithm();
            }
            // to terminate the program
            else if(cmd_in.equals("simulation_finish"))
            {
    	        System.out.println("Finish program execution!");
                snode.end_program();
                return 0;
            }
            // got a REQUEST message, process it
            else if(cmd_in.equals("REQUEST"))
            {
                int ts = Integer.valueOf(in.readLine());
                int pid = Integer.valueOf(in.readLine());
                String filename = in.readLine();
    	        System.out.println("REQUEST received from PID "+pid+" with timestamp "+ts+" for file "+filename);
                process_request_message(ts,pid,filename);
            }
            // got a REPLY message, process it
            else if(cmd_in.equals("REPLY"))
            {
                int pid = Integer.valueOf(in.readLine());
                String filename = in.readLine();
    	        System.out.println("REPLY received from PID "+pid+" for file "+filename);
                process_reply_message(pid,filename);
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
