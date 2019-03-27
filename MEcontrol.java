import java.io.*;
import java.util.*;
// Data structure to store Shared variables that help in running Ricart-Agrawala algorithm
public class MEcontrol
{
    // ME is my_id
    public int ME;
    public int total_msgs_tx;
    public int total_msgs_rx;
    public int crit_msgs_tx;
    public int crit_msgs_rx;
    public int crit_elapsed_time;

    // current sequence number
    public int timestamp;
    public int target_reply_count;
    public int replies_received;
    // using the critical section : boolean flag 
    public boolean locked;
    public PriorityQueue<RequestData> queue = null;
    // constructor takes ID
    MEcontrol(int ME)
    {
        this.timestamp = 0;
        this.locked = false;
        this.target_reply_count = 0;
        this.replies_received = 0;
        this.queue = new PriorityQueue<>();
        this.ME = ME;
    }
}
