import java.io.*;
import java.util.*;
// Data structure to store Shared variables that help in running Ricart-Agrawala algorithm
public class MEcontrol
{
    // ME is my_id
    public int ME;
    //number of nodes
    public int N = 5;
    // which file is this instance being used 
    public String filename;
    // current sequence number
    public int our_sn;
    // highest sequence number
    public int high_sn;
    // using the critical section : boolean flag 
    public boolean using;
    // issued a request/waiting to use the critical section : boolean flag 
    public boolean waiting;
    // authorization flag : boolean array
    // Carvalho-Roucairol optimization
    public Boolean[] A = new Boolean[5];
    // replies deferred flag : boolean array
    public Boolean[] reply_deferred = new Boolean[5];

    // constructor takes ID and filename
    MEcontrol(int ME,String filename)
    {
        this.our_sn = 0;
        this.high_sn= 0;
        this.using = false;
        this.waiting= false;
        this.ME = ME;
        this.filename = filename;
        // none of the nodes have authorized us to enter critical section initially
        Arrays.fill(A, Boolean.FALSE);
        // no replies deferred at the start
        Arrays.fill(reply_deferred, Boolean.FALSE);
        // authorized by self
        A[ME] = true;
    }
}
