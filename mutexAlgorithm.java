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

// class that implements Ricart-Agrawala distributed mutual exclusion algorithm with Carvalho-Roucairol optimization
// processing request and reply messages are handled in SockHandle Class, but is part of this algorithm
public class mutexAlgorithm
{
    // shared data structure to run algorithm
    MEcontrol sword = null;
    // handle to the client node instance
    SNode snode = null;
    CNode cnode = null;
    ClientInfo c_info = null;
    ServerInfo s_info = null;

    mutexAlgorithm(SNode snode, CNode cnode,int c_id)
    {
        this.snode = snode;
        this.cnode = cnode;
        this.sword = new MEcontrol(c_id);
        this.c_info = new ClientInfo();
        this.s_info = new ServerInfo();
    }

    // synchronized method to request resource/ critical section
    public void request_resource(int randQ)
    {
        // update sequence number
        int target = 0;
        int ts = 0;
        synchronized(sword)
        {
            sword.timestamp = sword.timestamp + 1;
            System.out.println("Request resource timestamp :"+sword.timestamp);
            sword.start_time = System.currentTimeMillis();

            sword.target_reply_count = s_info.quorums.get(randQ).size();
            sword.replies_received = 0;
            target = sword.target_reply_count;
            ts = sword.timestamp;
            sword.crit_msgs_tx = 0;
            sword.crit_msgs_rx = 0;
        }
        // send requests to all nodes in quorum

        for(int j=0;j<target;j++)
        {
            int rx = s_info.quorums.get(randQ).get(j);
            System.out.println("REQUEST to "+ rx);
            cnode.s_list.get(rx).crit_request(ts);
            synchronized(sword)
            {
                ++sword.crit_msgs_tx;
            }
        }

        System.out.println("Wait for Replies");
        // wait till all replies are received
        waitfor_replies(target);

        System.out.println("Finish Wait for Replies");
    }

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

    // method that waits till all replies are received to enter critical section
    public void waitfor_replies(int target)
    {
        int current = -1;
        boolean breaker = false;
        //wait till all replies received
        while ( (current != target ) & !breaker)
        {
            synchronized(sword)
            {
                current = sword.replies_received;
                breaker = sword.restart;
            }
        }
    }

    // synchronized method to release resource/ critical section
    public void release_resource(int randQ)
    {
        synchronized(sword)
        {
            sword.timestamp = sword.timestamp + 1;
        }
        System.out.println("release resource timestamp :"+sword.timestamp);
        for(int j=0;j<s_info.quorums.get(randQ).size();j++)
        {
            int rx = s_info.quorums.get(randQ).get(j);
            System.out.println("RELEASE sent to "+ rx);
            // authorization set to false for the node to which deferred reply is just sent
            // send the reply
            cnode.s_list.get(rx).crit_release(sword.timestamp);
            synchronized(sword)
            {
                ++sword.crit_msgs_tx;
            }
        }

        update_stats();
    }

    public void update_stats()
    {
        synchronized(sword)
        {
            sword.end_time = System.currentTimeMillis();
            sword.crit_elapsed_time = sword.end_time - sword.start_time;
            sword.total_msgs_tx += sword.crit_msgs_tx;
            sword.total_msgs_rx += sword.crit_msgs_rx;
        }
    }

    public synchronized void reset_control()
    {
        sword.timestamp = 0;
        sword.locked = false;
        sword.restart = false;
        sword.target_reply_count = 0;
        sword.replies_received = 0;
        sword.reset_count = 0;
        sword.total_msgs_tx = 0;
        sword.total_msgs_rx = 0;
        sword.crit_msgs_tx = 0;
        sword.crit_msgs_rx = 0;
        sword.crit_elapsed_time = 0;
        sword.start_time = 0;
        sword.end_time = 0;
        while(!sword.queue.isEmpty())
        {
            sword.queue.remove();
        }
    }
}
