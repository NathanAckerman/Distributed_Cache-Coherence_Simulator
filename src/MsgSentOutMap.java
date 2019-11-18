import java.util.HashMap;

public class MsgSentOutMap<K, V> extends HashMap<K, V>
{
    public CacheState requestType;
    public RequestEntry request;
}
