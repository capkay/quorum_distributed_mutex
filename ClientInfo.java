import java.util.HashMap;
// Data structure initialized to have client endpoint information
public class ClientInfo
{
    // define hashmap to hold Client Info <Id, Endpoint Info>
    public HashMap<Integer, IPData> hmap = new HashMap<Integer, IPData>();
    
    ClientInfo()
    {
        // create and populate data
        IPData c1 = new IPData("localhost","5101");
        IPData c2 = new IPData("localhost","5102");
        IPData c3 = new IPData("localhost","5103");
        IPData c4 = new IPData("localhost","5104");
        IPData c5 = new IPData("localhost","5105");
        this.hmap.put(1,c1);
        this.hmap.put(2,c2);
        this.hmap.put(3,c3);
        this.hmap.put(4,c4);
        this.hmap.put(5,c5);
    }
}
