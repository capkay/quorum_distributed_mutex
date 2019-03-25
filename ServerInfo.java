import java.util.HashMap;
// Data structure initialized to have server endpoint information
public class ServerInfo
{
    // define hashmap to hold server Info <Id, Endpoint Info>
    public HashMap<Integer, IPData> hmap = new HashMap<Integer, IPData>();
    ServerInfo()
    {
        // create and populate data
        IPData s1 = new IPData("localhost","9101");
        IPData s2 = new IPData("localhost","9102");
        IPData s3 = new IPData("localhost","9103");
        IPData s4 = new IPData("localhost","9104");
        IPData s5 = new IPData("localhost","9105");
        IPData s6 = new IPData("localhost","9106");
        IPData s7 = new IPData("localhost","9107");
        this.hmap.put(1,s1);
        this.hmap.put(2,s2);
        this.hmap.put(3,s3);
        this.hmap.put(4,s4);
        this.hmap.put(5,s5);
        this.hmap.put(6,s6);
        this.hmap.put(7,s7);
    }
}
