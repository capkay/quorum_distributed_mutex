import java.io.*;
// Data structure to store endpoint <ip/host,port> information 
public class IPData implements Serializable 
{
    public String ip;
    public String port;
    // constructor takes ip and port info to create object
    IPData(String ip,String port)
    {
        this.ip = ip;
        this.port = port;
    }
}
