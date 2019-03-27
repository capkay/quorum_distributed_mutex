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

        sword.target_reply_count = s_info.quorums.get(randQ).size();
        sword.replies_received = 0;
        target = sword.target_reply_count;
        ts = sword.timestamp;
        }
        // send requests to all nodes in quorum

        for(int j=0;j<target;j++)
        {
            int rx = s_info.quorums.get(randQ).get(j);
            System.out.println("REQUEST to "+ rx);
            cnode.s_list.get(rx).crit_request(ts);
        }

        System.out.println("Wait for Replies");
        // wait till all replies are received
        waitfor_replies(target);

        System.out.println("Finish Wait for Replies");
    }

    // method that waits till all replies are received to enter critical section
    public void waitfor_replies(int target)
    {
        int current = -1;
        //wait till all replies received
        while ( current != target)
        {
            synchronized(sword)
            {
                current = sword.replies_received;
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
        }
    }
}
