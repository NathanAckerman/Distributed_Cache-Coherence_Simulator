import java.lang.Math;
import java.util.HashMap;
import java.util.Queue;

public final class L2Arbiter
{
	private static Core[] allCores;

	// all these are length p
	public static Queue<RequestEntry>[] data_lookup;
	public static Queue<RequestEntry>[] mem_lookup;
	public static Integer[] cycle_l2_reqs;
	public static Integer[] cycle_mem_reqs;

	public static ArrayList<MsgSentOutMap<Integer, Integer>>[] msg_sent_out;

	//private static Hashmap<int, int> addrToCycleMadeExclusive;
	
	private static int dc;
	private static int dm;
	private static int num_cores;
	private static int l2_addr_mask = 0;

	private static HashMap<CacheBlock, DirectoryEntry>[] directory; 


	private L2Arbiter() { }

	public static void init(int init_dc, int init_dm, int num_cores, int p)
	{
		// TODO initialize list

		dc = init_dc;
		dm = init_dm;

		for (int i = 11; i < 11+p; i++) {
			int mask_part = 1 << i;
			l2_addr_mask |= mask_part;
		}
		

	}

	public static void send_msg(ArrayList<Integer> cores, RequestEntry requestEntry)
	{
		MsgSentOutMap<Integer, Integer> newMap = new MsgSentOutMap<>();
		newMap.requestType = requestEntry.requestType;
		
		for(Integer i : cores) {
			newMap.put(i, 0);
		}

		// TODO: Implement this inside Core
		allCores[i].addToFulfillQueue(newMap, requestEntry);

		msg_sent_out.add(newMap);
	}



	// TODO implement this
	public static boolean inCache(RequestEntry requestEntry)
	{
		return false;
	}

	public static void do_cycle(int cycle)
	{
		int len = data_lookup.length;
		for (int core = 0; core < len; core++) {
			Queue<RequestEntry> req_list = data_lookup[core];

			/* get first request for this core */
			RequestEntry req = req_list.peek();
			if (req == null) continue;

			/* set when l2 was issued */
			if (cycle_l2_reqs[core] == null)
				cycle_l2_reqs[core] = cycle;

			/* don't service l2 until time */
			if (cycle < cycle_l2_reqs[core] + dc)
				continue;

			// At this point, we can service the request
			cycle_l2_reqs[core] = cycle;


			L2Piece l2piece = l2Caches[getL2CoreID(req.address)];
			CacheBlock cacheBlock = l2piece.inCache(req);

			// Hit and its valid in L2 cache
			if (cacheBlock != null) {
				resolveHit(cacheBlock, core, req);	
			}else{
				resolveMiss();
			}

			

			if (inCache(req)) {
				// TODO cache coherence
				req.resolved = true;
				continue;
			}

			/* miss */
			req_list.remove(0);
			mem_reqs[core].add(req);

			/* set when mem was issued */
			if (cycle_mem_reqs[core] == 0)
				cycle_mem_reqs[core] = cycle;

			/* don't service mem until time */
			if (cycle < cycle_mem_reqs[core] + dm)
				continue;

			// TODO cache coherence
			req.resolved = true;
		}
	}

	public static void queueRequest(int core_id, RequestEntry req)
	{
		data_lookup[core_id].add(req);
	}

	public static int getL2CoreID(long address) {
		return (address & l2_addr_mask) >> 11;
	}


	public static void resolveHit(CacheBlock cacheBlock, int core, RequestEntry req) { 
		// Check Directory
		DirectoryEntry directoryEntry = directory[core].get(cacheBlock);

		// DATA LOOKUP PORTION 
		// If its a read
		if (req.rw == 0) {
			// Check the state of Directory Entry
			if (directoryEntry.state == CacheState.EXCLUSIVE) {
				// Send message to all cores to change to share 
				ArrayList<Integer> notifyCores = directoryEntry.coreNumbers;
				send_msg(notifyCores, req);
			}else if (directoryEntry.state == CacheState.SHARED) {
				// We can fulfill the request
				directoryEntry.coreNumbers.add(req.requesterCoreNum);
				req.resolved = true;
			}
		}else if(req.rw == 1) {
			// Do this regardless of what the directory state its currently in
			ArrayList<Integer> notifyCores = directoryEntry.coreNumbers;
			send_msg(notifyCores, req);
		}
	}

	public static void resolveMiss() {

	}
}
