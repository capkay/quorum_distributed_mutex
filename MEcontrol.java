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
    public long crit_elapsed_time;
    public long start_time;
    public long end_time;
    public int reset_count;

    // current sequence number
    public int timestamp;
    public int target_reply_count;
    public int replies_received;
    // using the critical section : boolean flag 
    public boolean locked;
    public boolean restart;
    public PriorityQueue<RequestData> queue = null;
    // constructor takes ID
    MEcontrol(int ME)
    {
        this.timestamp = 0;
        this.reset_count = 0;
        this.locked = false;
        this.restart= false;
        this.target_reply_count = 0;
        this.replies_received = 0;
        this.queue = new PriorityQueue<>();
        this.ME = ME;
        this.total_msgs_tx = 0;
        this.total_msgs_rx = 0;
        this.crit_msgs_tx = 0;
        this.crit_msgs_rx = 0;
        this.crit_elapsed_time = 0;
        this.start_time = 0;
        this.end_time = 0;
        this.reset_count = 0;
    }
}
