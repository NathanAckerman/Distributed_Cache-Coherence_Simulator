import java.util.*;

public class Core {
	int core_num;
	int cycle = 0;
	Queue<RequestEntry> dq = new LinkedList<RequestEntry>();
	Queue<MsgSentOutMap<Integer, Integer>> fulfillQueue = new LinkedList<MsgSentOutMap<Integer, Integer>>();
	public L1Cache l1cache;
	public L2Piece l2piece;
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

	public void addToFulfillQueue(MsgSentOutMap<Integer, Integer> map) 
	{
		this.fulfillQueue.add(map);
	}

	public static void print_request(RequestEntry re)
	{
		Debug.println("address:"+String.format("0x%08X", re.address)+" rw:"+re.rw+" orig_core:"+re.requesterCoreNum+" delta:"+re.delta);
	}

	public void fulfill_request()
	{
		MsgSentOutMap<Integer, Integer> map = fulfillQueue.poll();
		if (map == null) 
			return;

		L2Arbiter.num_control_msgs++;
		RequestEntry req = map.request;
		CacheBlock cache_block = l1cache.getCacheBlock(req.address);
		if (cache_block.state == CacheState.EXCLUSIVE) {
			Debug.println("data msg for fulfill request");
			L2Arbiter.num_data_msgs++;
		} else {
			Debug.println("control msg for fulfill request");
			L2Arbiter.num_control_msgs++;
		}

		// If it is a read
		l1cache.updateEntry(req);
		map.put(this.core_num, 1);

		return;
		
	}

	public void do_cycle()
	{
		Debug.println("Core "+core_num+" is starting its do_cycle for cycle "+cycle);
		fulfill_request();

		if (finished_all_requests) {
			Debug.println("This core has no more requests");
			return;
		}

		RequestEntry head = dq.peek();
		if (head == null) {
			return;
		}
		if (head.delta > 0) {
			Debug.println("This core's next request is not until a later cycle");
			head.delta--;
		} else if (head.delta == 0) {
			boolean hit = l1cache.access(head);
			if (hit) {
				if(dq.size() == 1) {
					Debug.println("Core "+core_num+" is completing last request at cycle "+cycle);
					cycle_done = cycle;
					finished_all_requests = true;
				}
				Debug.println("Request Completed");
				Debug.println("Cache hit for core "+core_num+" at cycle "+cycle+" for request:");
				print_request(head);
				dq.remove();
			} else {
				Debug.println("Cache miss for core "+core_num+" at cycle "+cycle+" for request:");
				print_request(head);
				head.cycle_issued = cycle;
			}
			head.delta--;
		} else {
			if (head.resolved) {
				//this req is now done
				process_resolved_request();
			}
		}

		cycle++;
	}

	public void process_resolved_request() {
		Debug.println("data msg receieved at core");
		L2Arbiter.num_data_msgs++;
		RequestEntry head = dq.remove();
		if(dq.size() == 0) {
			Debug.println("Core "+core_num+" is completing last request at cycle "+cycle);
			cycle_done = cycle;
			finished_all_requests = true;
		}
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
