import java.io.*;
import java.util.*;
// Data structure to store endpoint <ip/host,port> information 
public class RequestData implements Comparable<RequestData>
{
    public int timestamp;
    public int id;
    // constructor takes ip and port info to create object
    RequestData(int timestamp,int id)
    {
        this.timestamp = timestamp;
        this.id = id;
    }

    @Override
    public int compareTo(RequestData b)
    {
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
//class SortRequests implements Comparator<RequestData> 
//{
//    public int compare(RequestData a, RequestData b)
//    {
//        return ( (a.timestamp - b.timestamp) + (a.id - b.id) )
//    }
//}
