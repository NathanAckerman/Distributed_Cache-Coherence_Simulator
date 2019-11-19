import java.lang.Math;
import java.util.*;

public final class L2Arbiter
{
	private static Core[] allCores;

	// all these are length p
	public static Queue<RequestEntry>[] data_lookup;
	public static Queue<RequestEntry>[] mem_lookup;
	public static ArrayList<MsgSentOutMap<Integer, Integer>>[] msg_sent_out;
	public static HashMap<CacheBlock, DirectoryEntry>[] directory;
	
	private static int dc;
	private static int dm;
	private static int num_cores;
	private static long l2_addr_mask = 0;
	private static int cur_cycle = 0;

	public static int num_data_msgs = 0;
	public static int num_control_msgs = 0;

	private L2Arbiter() { }

	@SuppressWarnings("unchecked")
	public static void init(int init_dc, int init_dm, int init_num_cores, int p, Core[] init_all_cores)
	{
		data_lookup = (LinkedList<RequestEntry>[])new LinkedList<?>[init_num_cores];
		mem_lookup = (LinkedList<RequestEntry>[])new LinkedList<?>[init_num_cores];
		msg_sent_out = (ArrayList<MsgSentOutMap<Integer, Integer>>[])new ArrayList<?>[init_num_cores];
		directory = (HashMap<CacheBlock, DirectoryEntry>[])new HashMap<?, ?>[init_num_cores];


		for (int i = 0; i < init_num_cores; i++ ){
			data_lookup[i] = new LinkedList<RequestEntry>();
			mem_lookup[i] = new LinkedList<RequestEntry>();
			msg_sent_out[i] = new ArrayList<MsgSentOutMap<Integer, Integer>>();
			directory[i] = new HashMap<CacheBlock, DirectoryEntry>();
		}

		dc = init_dc;
		dm = init_dm;
		num_cores = init_num_cores;
		allCores = init_all_cores;

		for (int i = 11; i < 11+p; i++) {
			long mask_part = 1 << i;
			l2_addr_mask |= mask_part;
		}
	}

	public static void send_msg(ArrayList<Integer> cores, RequestEntry requestEntry)
	{
		MsgSentOutMap<Integer, Integer> newMap = new MsgSentOutMap<>();
		newMap.requestType = requestEntry.requestType;
		newMap.request = requestEntry;
		
		for (Integer i : cores)
			newMap.put(i, 0);

		for (Integer i : cores)
			allCores[i].addToFulfillQueue(newMap);

		msg_sent_out[getL2CoreID(requestEntry.address)].add(newMap);
	}

	public static void check_msg_sent_out(int core_num)
	{
		ArrayList<MsgSentOutMap<Integer, Integer>> completedRequests = new ArrayList<>();

		// Loop through to check all the finished mappings
		for(MsgSentOutMap<Integer, Integer> msgOut : msg_sent_out[core_num]) {
			boolean isFinished = true;

			for (Integer key : msgOut.keySet()) {
				if (msgOut.get(key) == 0) {
					isFinished = false;
					break;
				}
			}

			if (isFinished)
				completedRequests.add(msgOut);
		}

		// Remove from the MsgSentOutMap
		for (MsgSentOutMap<Integer, Integer> msgOut : completedRequests) {
			msg_sent_out[core_num].remove(msgOut);
			msgOut.request.resolved = true;
			Core mem_req_core = allCores[core_num];
			L2Piece mem_l2_piece = mem_req_core.l2piece;
			CacheBlock cacheBlock = mem_l2_piece.getCacheBlock(msgOut.request.address);

			if (msgOut.requestType == CacheState.EXCLUSIVE) {
				DirectoryEntry newDirectory = new DirectoryEntry(msgOut.request);
				directory[core_num].put(cacheBlock, newDirectory);
			} else if(msgOut.requestType == CacheState.SHARED) {
				directory[core_num].get(cacheBlock).state = CacheState.SHARED;
			}
		}
	}

	public static void do_cycle(int cycle)
	{
		cur_cycle = cycle;
		check_msg_sent_out_loop(cycle);
		data_lookup_func(cycle);
	}

	public static void check_msg_sent_out_loop(int cycle) 
	{
		for (int i = 0; i < num_cores; i++) {
			check_msg_sent_out(i);
		}
	}


	public static void data_lookup_func(int cycle) 
	{
		int len = data_lookup.length;
		for (int core = 0; core < len; core++) {
			Queue<RequestEntry> req_list = data_lookup[core];

			/* get first request for this core */
			RequestEntry req = req_list.peek();
			if (req != null) {

				if (req.cycle_l2_start == null)
					req.cycle_l2_start = cycle;

				/* don't service l2 until time */
				if (cycle < req.cycle_l2_start + dc)
					continue;

				// At this point, we can service the request

				req_list.remove();


				L2Piece l2piece = allCores[core].l2piece;
				CacheBlock cacheBlock = allCores[core].l2piece.getCacheBlock(req);

				// Hit and its valid in L2 cache
				if (cacheBlock != null)
					resolveHit(cacheBlock, req);
				else
					resolveMiss(req);
			}

			Queue<RequestEntry> mem_list = mem_lookup[core];
			RequestEntry mem_req = mem_list.peek();
			if (mem_req != null) {
				/* set when mem was issued */
				if (mem_req.cycle_mem_start == null)
					mem_req.cycle_mem_start = cycle;

				/* don't service mem until time */
				if (cycle < mem_req.cycle_mem_start + dm)
					continue;

				// At this point, mem request can be fulfilled

				mem_list.remove();

				resolveMemAccess(mem_req);
			}
		}
	}

	public static void queueRequest(RequestEntry req)
	{
		Debug.println("data msg received in L2 Arbiter");
		L2Arbiter.num_data_msgs++;
		int core = getL2CoreID(req.address);
		data_lookup[core].add(req);
	}

	public static int getL2CoreID(long address) {
		return (int)((address & l2_addr_mask) >> 11);
	}


	public static void resolveHit(CacheBlock cacheBlock, RequestEntry req) { 
		// Check Directory
		Debug.println("L2 hit on cycle "+cur_cycle+" for request: ");
		Core.print_request(req);
		DirectoryEntry directoryEntry = directory[getL2CoreID(req.address)].get(cacheBlock);

		// DATA LOOKUP PORTION 
		// If its a read
		if (req.rw == 0) {
			// Check the state of Directory Entry
			if (directoryEntry.state == CacheState.EXCLUSIVE) {
				// Send message to all cores to change to share 
				ArrayList<Integer> notifyCores = directoryEntry.coreNumbers;
				send_msg(notifyCores, req);
			} else if (directoryEntry.state == CacheState.SHARED) {
				// We can fulfill the request
				directoryEntry.coreNumbers.add(req.requesterCoreNum);
				req.resolved = true;
			}
		} else if( req.rw == 1) {
			// Do this regardless of what the directory state its currently in
			ArrayList<Integer> notifyCores = directoryEntry.coreNumbers;
			send_msg(notifyCores, req);
		}
	}

	public static void resolveMiss(RequestEntry req) {
		Debug.println("L2 miss on cycle "+cur_cycle+" for request: ");
		Core.print_request(req);
		mem_lookup[getL2CoreID(req.address)].add(req);
	}

	public static void resolveMemAccess(RequestEntry mem_req) {
		Debug.println("Mem access resolving on cycle "+cur_cycle+" for request:");
		Core.print_request(mem_req);
		Core mem_has_core = allCores[getL2CoreID(mem_req.address)];
		L2Piece mem_l2_piece = mem_has_core.l2piece;
		CacheBlock cacheBlock = mem_l2_piece.getCacheBlock(mem_req);
		if (cacheBlock == null) {
			Debug.println("Creating new directory entry for this request");
			mem_l2_piece.add(mem_req.address, mem_req.rw);
			CacheBlock newCacheBlock = mem_l2_piece.getCacheBlock(mem_req);
			DirectoryEntry newEntry = new DirectoryEntry(mem_req);

			directory[getL2CoreID(mem_req.address)].put(newCacheBlock, newEntry);
			mem_req.resolved = true;
		} else {
			// only if two pending requests
			Debug.println("This mem request had a previous one pending for the same block so we need to grab the value from that core if it was exclusive");
			data_lookup[mem_has_core.core_num].add(mem_req);
		}
	}

	public static void removeL1(long address, int core_num)
	{
		L2Arbiter.num_control_msgs++;
		int l2piece_core_id = getL2CoreID(address);

		CacheBlock key = allCores[l2piece_core_id].l2piece.getCacheBlock(address);
		DirectoryEntry dirent = directory[l2piece_core_id].get(key);
		dirent.coreNumbers.remove(Integer.valueOf(core_num));
	}

	public static void removeL2(CacheBlock cache_block, int core_num)
	{
		int l2piece_core_id = getL2CoreID(cache_block.address);
		DirectoryEntry dirent = directory[l2piece_core_id].remove(cache_block);
		if (dirent == null)
			return;

		for (Integer core_id : dirent.coreNumbers)
			allCores[core_id].l1cache.invalidate(cache_block.address);
	}
}
