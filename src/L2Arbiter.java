import java.lang.Math;
import java.util.HashMap;

public final class L2Arbiter
{
	private static L2Piece[] l2Caches;

	// all these are length p
	public static ArrayList<RequestEntry>[] l2_reqs;
	public static ArrayList<RequestEntry>[] mem_reqs;
	public static int[] cycle_l2_reqs;
	public static int[] cycle_mem_reqs;
	//private static Hashmap<int, int> addrToCycleMadeExclusive;
	
	private static int dc;
	private static int dm;
	private static int num_cores;
	private static int l2_addr_mask = 0;

	private static HashMap<CacheBlock, DirectoryEntry>[] directory = new HashMap<>(); 


	private L2Arbiter() { }

	public static void init(int init_dc, int init_dm, int num_cores, int p)
	{
		// TODO do these
		// init the lists
		dc = init_dc;
		dm = init_dm;

		for (int i = 11; i < 11+p; i++) {
			int mask_part = 1 << i;
			l2_addr_mask |= mask_part;
		}
		

	}

	// TODO implement this
	public static boolean inCache(RequestEntry requestEntry)
	{
		return false;
	}

	public static void do_cycle(int cycle)
	{
		int len = l2_reqs.length;
		for (int core = 0; core < len; core++) {
			ArrayList<RequestEntry> req_list = l2_reqs[core];

			/* get first request for this core */
			RequestEntry req;
			try  {
				req = req_list.get(0);
			} catch(Exception e) {
				continue;
			}

			/* set when l2 was issued */
			if (cycle_l2_reqs[core] == 0)
				cycle_l2_reqs[core] = cycle;

			/* don't service l2 until time */
			if (cycle < cycle_l2_reqs[core] + dc)
				continue;

			// At this point, we can service the request

			/* hit */

			L2Piece l2piece = l2Caches[getL2CoreID(req.address)];

			CacheBlock cacheBlock = l2piece.inCache(req);

			// Hit and its valid in L2 cache
			if (cacheBlock != null) {
				// If its a read request
				if (req.rw == 0) {
					// call function to change states to shared in directory
				}
				if (req.rw == 0 || (req.rw == 1 && cacheBlock.state == CacheState.EXCLUSIVE)){
					// Read Request
					return true;
				}
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
		l2_reqs[core_id].add(req);
	}

	public static int getL2CoreID(long address) {
		return (address & l2_addr_mask) >> 11;
	}
}
