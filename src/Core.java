import java.util.*;

public class Core {
	int core_num;
	//needs DQ
	Queue<RequestEntry> dq = new LinkedList<RequestEntry>();
	//needs L1 cache
	//needs L2 cache
	//needs message queues
	public Core(int core_num)
	{
		this.core_num = core_num;
	}

	public void add_request_entry(int delta, long address, int rw)
	{
		RequestEntry new_req = new RequestEntry(delta, address, rw);
		dq.add(new_req);
	}

	public void print_dq()
	{
		for(RequestEntry re : dq) {
			System.out.println("address:"+re.address+" rw:"+re.rw+" delta:"+re.delta);
		}
	}

	public void do_cycle()
	{

	}

	public int get_dq_size()
	{
		return dq.size();
	}

}

class RequestEntry {
	int delta;
	long address;
	int rw;//0 for read, 1 for write
	int resolved = false;
	public RequestEntry(int delta, long address, int rw)
	{
		this.delta = delta;
		this.address = address;
		this.rw = rw;
	}

}
