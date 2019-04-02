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

// class that implements distributed mutual exclusion algorithm loosely based on Maekawa's Quorum based algorithm
// without YIELD and ENQUIRE/RELINQUISH messages; thus having a possibility of deadlock
// processing request and reply messages are handled in ClientSockHandle & ServerSockHandle Classes, but is part of this algorithm
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

    // method to request resource/ critical section
    public void request_resource(int randQ)
    {
        int target = 0;
        int ts = 0;
        synchronized(sword)
        {
            // update the quorum index that the request is being issued for
            sword.quorum_index = randQ;
            sword.waiting = true;
            // update logical clock
            sword.timestamp = sword.timestamp + 1;
            System.out.println("Request resource timestamp :"+sword.timestamp);
            // mark current system time to measure latency
            sword.start_time = System.currentTimeMillis();

            // set variables; self-explainable
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
            // send request messages to respective servers in this randomly chosen quorum
            cnode.s_list.get(rx).crit_request(ts);
            synchronized(sword)
            {
                // update sent msg count
                ++sword.crit_msgs_tx;
            }
        }
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

    // synchronized method to release resource/ critical section
    public void release_resource()
    {
        int randQ;
        synchronized(sword)
        {
            // get the quorum for which request was issued
            randQ = sword.quorum_index;
            // update logical clock
            sword.timestamp = sword.timestamp + 1;
        }
        System.out.println("release resource timestamp :"+sword.timestamp);
        for(int j=0;j<s_info.quorums.get(randQ).size();j++)
        {
            int rx = s_info.quorums.get(randQ).get(j);
            System.out.println("RELEASE sent to "+ rx);
            // send corresponding release messages to respective servers in this randomly chosen quorum
            cnode.s_list.get(rx).crit_release(sword.timestamp);
            synchronized(sword)
            {
                // update sent msg count
                ++sword.crit_msgs_tx;
            }
        }

        // update stats since this is the end of this iteration
        update_stats();
    }

    public void update_stats()
    {
        synchronized(sword)
        {
            // calculate latency
            sword.end_time = System.currentTimeMillis();
            sword.crit_elapsed_time = sword.end_time - sword.start_time;
            // update totals
            sword.total_msgs_tx += sword.crit_msgs_tx;
            sword.total_msgs_rx += sword.crit_msgs_rx;
            // current request finished
            sword.waiting = false;
        }
        synchronized(this)
        {
            // notify to wake up request thread to issue the next request
            this.notifyAll();
        }
    }

    // method to reset to default values; for the restart feature
    public synchronized void reset_control()
    {
        sword.timestamp = 0;
        sword.locked = false;
        sword.restart = false;
        sword.waiting = false;
        sword.target_reply_count = 0;
        sword.replies_received = 0;
        sword.reset_count = 0;
        sword.finish_sim_count= 0;
        sword.total_msgs_tx = 0;
        sword.total_msgs_rx = 0;
        sword.crit_msgs_tx = 0;
        sword.crit_msgs_rx = 0;
        sword.crit_elapsed_time = 0;
        sword.start_time = 0;
        sword.end_time = 0;
        sword.quorum_index = 0;
        while(!sword.queue.isEmpty())
        {
            sword.queue.remove();
        }
    }
}
