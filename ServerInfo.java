import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
// Data structure initialized to have server endpoint information
public class ServerInfo
{
    // define hashmap to hold server Info <Id, Endpoint Info>
    public HashMap<Integer, IPData> hmap = new HashMap<Integer, IPData>();
    public ArrayList<ArrayList<Integer>> quorums = new ArrayList<ArrayList<Integer>>();
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
        this.quorums.add(new ArrayList<Integer>() {{ add(1); add(2); add(4); }});
        this.quorums.add(new ArrayList<Integer>() {{ add(1); add(2); add(5); }});
        this.quorums.add(new ArrayList<Integer>() {{ add(1); add(4); add(5); }});
        this.quorums.add(new ArrayList<Integer>() {{ add(1); add(3); add(6); }});
        this.quorums.add(new ArrayList<Integer>() {{ add(1); add(3); add(7); }});
        this.quorums.add(new ArrayList<Integer>() {{ add(1); add(6); add(7); }});
        this.quorums.add(new ArrayList<Integer>() {{ add(2); add(4); add(3); add(6); }});
        this.quorums.add(new ArrayList<Integer>() {{ add(2); add(4); add(3); add(7); }});
        this.quorums.add(new ArrayList<Integer>() {{ add(2); add(5); add(3); add(6); }});
        this.quorums.add(new ArrayList<Integer>() {{ add(2); add(5); add(3); add(7); }});
        this.quorums.add(new ArrayList<Integer>() {{ add(2); add(4); add(6); add(7); }});
        this.quorums.add(new ArrayList<Integer>() {{ add(2); add(5); add(6); add(7); }});
        this.quorums.add(new ArrayList<Integer>() {{ add(4); add(5); add(3); add(6); }});
        this.quorums.add(new ArrayList<Integer>() {{ add(4); add(5); add(3); add(7); }});
        this.quorums.add(new ArrayList<Integer>() {{ add(4); add(5); add(6); add(7); }});
        //this.quorums.add(Arrays.asList(1, 2, 4));
        //this.quorums.add(Arrays.asList(1, 2, 5));
        //this.quorums.add(Arrays.asList(1, 4, 5));
        //this.quorums.add(Arrays.asList(1, 3, 6));
        //this.quorums.add(Arrays.asList(1, 3, 7));
        //this.quorums.add(Arrays.asList(1, 6, 7));
        //this.quorums.add(Arrays.asList(2, 4, 3, 6));
        //this.quorums.add(Arrays.asList(2, 4, 3, 7));
        //this.quorums.add(Arrays.asList(2, 5, 3, 6));
        //this.quorums.add(Arrays.asList(2, 5, 3, 7));
        //this.quorums.add(Arrays.asList(2, 4, 6, 7));
        //this.quorums.add(Arrays.asList(2, 5, 6, 7));
        //this.quorums.add(Arrays.asList(4, 5, 3, 6));
        //this.quorums.add(Arrays.asList(4, 5, 3, 7));
        //this.quorums.add(Arrays.asList(4, 5, 6, 7));
    }
}
