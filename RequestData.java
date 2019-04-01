import java.io.*;
import java.util.*;
// Data structure to store pending request information
public class RequestData implements Comparable<RequestData>
{
    public int timestamp;
    public int id;
    // constructor takes request timestamp and ID of requestor
    RequestData(int timestamp,int id)
    {
        this.timestamp = timestamp;
        this.id = id;
    }

    // custom compare method to maintain proper order in the priority queue
    @Override
    public int compareTo(RequestData b)
    {
        // higher priority to lower timestamp, or lower ID in case timestamps are equal
        if( (this.timestamp < b.timestamp) | ((this.timestamp == b.timestamp) & (this.id < b.id)) )
        {
            return -1;
        }
        else
        {
            return 1;
        }

    }
}
