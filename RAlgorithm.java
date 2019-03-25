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
public class RAlgorithm
{
    // shared data structure to run algorithm
    MEcontrol cword = null;
    // handle to the client node instance
    ClientNode cnode = null;

    RAlgorithm(ClientNode cnode, int c_id, String filename)
    {
        this.cnode = cnode;
        this.cword = new MEcontrol(c_id,filename);
    }

    // synchronized method to request resource/ critical section
    public synchronized void request_resource()
    {
        // mark flag as request issued
        cword.waiting = true;
        // update sequence number
        cword.our_sn = cword.high_sn + 1;
        System.out.println("Request resource timestamp :"+cword.our_sn);

        // send requests to all other nodes
        for(int j=0;j<cword.N;j++)
        {
            // send request if not authorized
            if( (j != cword.ME) & (!cword.A[j]) )
            {
                System.out.println("REQUEST to "+ j);
                cnode.c_list.get(j).crit_request(cword.our_sn,cword.filename);
            }
            // print a message for authorized node
            if( (j != cword.ME) & (cword.A[j]) )
            {
                System.out.println("REQUEST not needed to "+ j);
            }
        }

        System.out.println("Wait for Replies");
        // wait till all replies are received
        waitfor_replies();

        System.out.println("Finish Wait for Replies");

        // request issue flag taken down
        cword.waiting = false;
        // going to enter critical section, mark flag
        cword.using = true;
    }

    // method to check if boolean array has all true values
    public static boolean areAllTrue(Boolean[] array)
    {
        for(Boolean b : array) if(!b) return false;
        return true;
    }

    // method that waits till all replies are received to enter critical section
    public void waitfor_replies()
    {
        Boolean[] temp = new Boolean[5];
        Arrays.fill(temp, Boolean.FALSE);
        //wait till all replies received
        while ( !areAllTrue(temp))
        {
            synchronized(cword)
            {
                temp = cword.A;
            }
        }
    }

    // synchronized method to release resource/ critical section
    public synchronized void release_resource()
    {
        int j;
        // stop using critical section, mark flag
        cword.using = false;
        for(j=0;j<cword.N;j++)
        {
            // send all deferred replies
            if( cword.reply_deferred[j] )
            {
                System.out.println("DEFERRED REPLY sent to "+ j);
                // authorization set to false for the node to which deferred reply is just sent
                cword.A[j]=false;
                // unset own flag
                cword.reply_deferred[j]=false;
                // send the reply
                cnode.c_list.get(j).crit_reply(cword.filename);
            }
        }
    }
}
