import java.lang.Math;
import java.util.HashMap;
import java.util.Queue;

public final class L2Arbiter
{
	private static Core[] allCores;

	// all these are length p
	public static Queue<RequestEntry>[] data_lookup;
	public static Queue<RequestEntry>[] mem_lookup;
	public static ArrayList<MsgSentOutMap<Integer, Integer>>[] msg_sent_out;
	private static HashMap<CacheBlock, DirectoryEntry>[] directory;
	
	private static int dc;
	private static int dm;
	private static int num_cores;
	private static int l2_addr_mask = 0;


	private L2Arbiter() { }

	public static void init(int init_dc, int init_dm, int init_num_cores, int p, Core[] init_all_cores)
	{
		data_lookup = new Queue<RequestEntry>[init_num_cores];
		mem_lookup = new Queue<RequestEntry>[init_num_cores];
		msg_sent_out = new ArrayList<MsgSentOutMap<Integer, Integer>>[init_num_cores];
		directory = new HashMap<CacheBlock, DirectoryEntry>[init_num_cores];


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
			int mask_part = 1 << i;
			l2_addr_mask |= mask_part;
		}
		

	}

	public static void send_msg(ArrayList<Integer> cores, RequestEntry requestEntry)
	{
		MsgSentOutMap<Integer, Integer> newMap = new MsgSentOutMap<>();
		newMap.requestType = requestEntry.requestType;
		newMap.request = requestEntry;
		
		for(Integer i : cores) {
			newMap.put(i, 0);
		}

		allCores[i].addToFulfillQueue(newMap);

		msg_sent_out.add(newMap);
	}

	public static void check_msg_sent_out(int core_num)
	{
		ArrayList<MsgSentOutMap<Integer, Integer>> completedRequests;

		// Loop through to check all the finished mappings
		for(MsgSentOutMap<Integer, Integer> msgOut : msg_sent_out[core_num]) {
			boolean isFinished = true;

			for (Integer key : msgOut.keySet()) {
				if (msgOut.get(key) == 0) {
					isFinished = false;
					break;
				}
			}

			if(isFinished) {
				completedRequests.add(msgOut);
			}
		}

		// Remove from the MsgSentOutMap
		for(MsgSentOutMap<Integer, Integer> msgOut : completedRequests){
			msg_sent_out[core_num].remove(msgOut);
			msgOut.request.resolved = true;
			Core mem_req_core = allCores[core_num];
			L2Piece mem_l2_piece = mem_req_core.l2piece;
			CacheBlock cacheBlock = mem_l2_piece.getCacheBlock();

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
			if (req == null) continue;
			
			if (req.cycle_l2_start == null)
				req.cycle_l2_start = cycle;

			/* don't service l2 until time */
			if (cycle < req.cycle_l2_start + dc)
				continue;

			// At this point, we can service the request

			req_list.remove();


			L2Piece l2piece = l2Caches[getL2CoreID(req.address)];
			CacheBlock cacheBlock = allCores[req.requesterCoreNum].l2piece.getCacheBlock(req);

			// Hit and its valid in L2 cache
			if (cacheBlock != null)
				resolveHit(cacheBlock, core, req);	
			else
				resolveMiss(req);

			Queue<RequestEntry> mem_list = mem_lookup[core];
			RequestEntry mem_req = mem_list.peek();

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

	public static void queueRequest(int core_id, RequestEntry req)
	{
		data_lookup[core_id].add(req);
	}

	public static int getL2CoreID(long address) {
		return (address & l2_addr_mask) >> 11;
	}


	public static void resolveHit(CacheBlock cacheBlock, RequestEntry req) { 
		// Check Directory
		DirectoryEntry directoryEntry = directory[req.requesterCoreNum].get(cacheBlock);

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
		mem_lookup[req.requesterCoreNum].add(req);
	}

	public static void resolveMemAccess(RequestEntry mem_req) {
		Core mem_req_core = allCores[mem_req.requesterCoreNum];
		L2Piece mem_l2_piece = mem_req_core.l2piece;
		CacheBlock cacheBlock = mem_l2_piece.getCacheBlock(mem_req);
		if (cacheBlock == null) {
			mem_l2_piece.add(mem_req.address, mem_req.rw);
			CacheBlock newCacheBlock = mem_l2_piece.getCacheBlock(mem_req);
			DirectoryEntry newEntry = new DirectoryEntry(mem_req);

			directory[mem_req.requesterCoreNum].put(newCacheBlock, newEntry);
		} else {
			data_lookup.add(mem_req);
		}
	}

	public static void removeL1(long address, int core_num)
	{
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