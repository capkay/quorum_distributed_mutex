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
    ClientInfo c_info = null;
    // hash table that contains socket connections from clients based on client IDs
    HashMap<Integer, ClientSockHandle> s_list = new HashMap<Integer, ClientSockHandle>();
    // list of files updated during start
    List<String> files = new ArrayList<String>();
    // handle to server object, ultimately self 
    CNode snode = null;
    // constructor takes ServerID passed from command line from main()
    // populate_files & listenSocket is called as part of starting up
    CNode(int c_id)
    {
        this.c_info = new ClientInfo();
        this.c_id = c_id;
    	this.port = c_info.hmap.get(c_id).port;
        this.listenSocket();
        this.snode = this;
    }

    // CommandParser class is used to parse and execute respective commands that are entered via command line to SETUP/LIST/START/FINISH simulation
    public class CommandParser extends Thread
    {
      	// initialize patters for commands
    	Pattern LIST  = Pattern.compile("^LIST$");
    	Pattern FINISH= Pattern.compile("^FINISH$");
    	
    	// read from inputstream, process and execute tasks accordingly	
    	int rx_cmd(Scanner cmd)
        {
    		String cmd_in = null;
    		if (cmd.hasNext())
    			cmd_in = cmd.nextLine();
    
    		Matcher m_LIST= LIST.matcher(cmd_in);
    		Matcher m_FINISH= FINISH.matcher(cmd_in);
    		
                // print the list of files and
                // check the list of socket connections available on this server
                if(m_LIST.find())
                { 
		    System.out.println("Files:");
		    for (int i = 0; i < files.size(); i++) {
		    	System.out.println(files.get(i));
		    }
                    synchronized (s_list)
                    {
                        System.out.println("\n=== Connections to servers ===");
                        s_list.keySet().forEach(key -> {
                        System.out.println("key:"+key + " => ID " + s_list.get(key).remote_c_id);
                        });
                        System.out.println("=== size ="+s_list.size());
                    }
    		}
                else if(m_FINISH.find())
                { 
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
                	//System.exit(1);
                    e.printStackTrace(); 
                }
            });
        }
        System.exit(1);
    }

    // read last line and return it, for the requested file
    public String do_read_operation(String filename)
    {
        // directory is based on serverID
        File file = new File("./"+c_id+"/"+filename);
	if (!file.exists()) 
        {
	    System.out.println("File "+filename+" does not exist");
            return "NULL";
	}
        // return line
        return readFromLast(file,filename);
    }

    // method to read last line from a file
    // adapted from StackOverflow
    public String readFromLast(File file, String filename)
    {
        int lines = 0;
        StringBuilder builder = new StringBuilder();
        RandomAccessFile randomAccessFile = null;
        try 
        {
            randomAccessFile = new RandomAccessFile(file, "r");
            long fileLength = file.length() - 1;
            // Set the pointer at the last of the file
            randomAccessFile.seek(fileLength);
            for(long pointer = fileLength; pointer >= 0; pointer--)
            {
                randomAccessFile.seek(pointer);
                char c;
                // read from the last one char at the time
                c = (char)randomAccessFile.read(); 
                // break when end of the line
                if(c == '\n')
                {
                    break;
                }
                builder.append(c);
            }
            // Since line is read from the last so it 
            // is in reverse so use reverse method to make it right
            builder.reverse();
            System.out.println("Line - " + builder.toString()+" - File : "+filename);
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
        finally
        {
            if(randomAccessFile != null)
            {
                try 
                {
                    randomAccessFile.close();
                } 
                catch (IOException e) 
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return builder.toString();
    }

    // write to file
    public void do_write_operation(String filename,String content)
    {
        // directory is based on serverID
        File file = new File("./"+c_id+"/"+filename);
	if (!file.exists()) 
        {
	    System.out.println("File "+filename+" does not exist");
            return;
	}
        try
        {
            // write / append to the file
            FileWriter fw = new FileWriter(file, true);
            fw.write("\n"+content);
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
                	    ClientSockHandle t = new ClientSockHandle(s,ip,port,c_id,s_list,true,snode);
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
