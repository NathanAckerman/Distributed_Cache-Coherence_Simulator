import java.util.*;


public class DirectoryEntry {
    public CacheState state;
    public ArrayList<Integer> coreNumbers;

    public DirectoryEntry(RequestEntry req){
        this.state = req.requestType;
        coreNumbers = new ArrayList<Integer>();
        coreNumbers.add(req.requesterCoreNum);
    }

}