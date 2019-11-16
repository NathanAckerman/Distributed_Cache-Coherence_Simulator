import java.util.*;

public class Core {
	int core_num;
	Queue<RequestEntry> dq = new LinkedList<RequestEntry>();
	Cache l1cache = new L1Cache();
	Cache l2piece = new L2Piece();

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
