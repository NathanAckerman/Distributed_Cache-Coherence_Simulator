import java.util.*;

public class Core {
	int core_num;
	int cycle = 0;
	Queue<RequestEntry> dq = new LinkedList<RequestEntry>();
	Queue<MsgSentOutMap> fulfillQueue = new LinkedList<MsgSentOutMap>();
	L1Cache l1cache = new L1Cache();
	L2Piece l2piece = new L2Piece();
	boolean finished_all_requests = false;
	public int cycle_done = 0;
	public int total_requests_missed = 0;
	public int total_miss_penalty = 0;

	public Core(int core_num)
	{
		this.core_num = core_num;
	}

	public void add_request_entry(int delta, long address, int rw)
	{
		RequestEntry new_req = new RequestEntry(delta, address, rw, l1cache, (rw==0 ? CacheState.SHARED : CacheState.EXCLUSIVE), core_num);
		dq.add(new_req);
	}

	public void print_dq()
	{
		for(RequestEntry re : dq) {
			print_request(re);
		}
	}

	public void addToFulfillQueue(MsgSentOutMap map) 
	{
		this.fulfillQueue.add(map);
	}

	public void print_request(RequestEntry re)
	{
		Debug.println("address:"+re.address+" rw:"+re.rw+" delta:"+re.delta);
	}

	public void fulfill_request()
	{
		MsgSentOutMap<Integer, Integer> map = fulfillQueue.remove();

		if (map == null) 
			return;

		RequestEntry req = map.request;

		// If it is a read
		l1cache.updateEntry(req);
		map.put(this.core_num, 1);

		return;
		
	}

	public void do_cycle()
	{

		fulfill_request();

		if (finished_all_requests) {
			return;
		} else if (dq.size() == 0) {
			Debug.println("Core "+core_num+" is completing last request at cycle "+cycle);
			cycle_done = cycle;
			finished_all_requests = true;
		}

		RequestEntry head = dq.peek();
		if (head == null) {
			return;
		}
		if (head.delta > 0) {
			head.delta--;
			if (head.delta == 0) {//ready to be issued
				boolean hit = l1cache.access(head);
				if (hit) {
					Debug.println("Cache hit for core "+core_num+" at cycle "+cycle+" for request:");
					print_request(head);
					dq.remove();
				} else {
					Debug.println("Cache miss for core "+core_num+" at cycle "+cycle+" for request:");
					print_request(head);
					head.cycle_issued = cycle;
				}
			}
		} else {
			if (cycle == 0 && head.delta <= 0) {//edge case where first cycle has req
				boolean hit = l1cache.access(head);
				if (hit) {
					Debug.println("Cache hit for core "+core_num+" at cycle "+cycle+" for request:");
					print_request(head);
					dq.remove();
				} else {
					Debug.println("Cache miss for core "+core_num+" at cycle "+cycle+" for request:");
					print_request(head);
					head.cycle_issued = cycle;
				}
			} else if (head.resolved) {
				//this req is now done
				process_resolved_request();
			}
		}

		cycle++;
	}

	public void process_resolved_request() {
		RequestEntry head = dq.remove();
		total_requests_missed++;
		int miss_penalty = cycle - head.cycle_issued;
		total_miss_penalty += miss_penalty;

		l1cache.cache_access(head.address, head.rw);
		CacheBlock cacheBlock = l1cache.getCacheBlock(head);
		cacheBlock.state = head.requestType;

		//debug printing
		Debug.println("\n");
		Debug.println("Request completed for core "+core_num+":");
		print_request(head);
		Debug.println("Cycle Completed: "+cycle+" Miss Penalty: "+miss_penalty);
	}

	public int get_dq_size()
	{
		return dq.size();
	}

}
