public final class L2Arbiter
{
	private static ArrayList<L2Piece> l2list = new ArrayList<L2Piece>();
	private static Queue<RequestEntry> requests = new LinkedList<RequestEntry>();

	// all these are length p
	public static ArrayList<RequestEntry>[] l2_reqs;
	public static ArrayList<RequestEntry>[] mem_reqs;
	public static int[] cycle_l2_reqs;
	public static int[] cycle_mem_reqs;
	//private static Hashmap<int, int> addrToCycleMadeExclusive;
	
	int dc;
	int dm;

	private static L2Arbiter() { }

	public void init(int dc, int dm)
	{
		// TODO do these
		// init the lists
		this.dc = dc;
		this.dm = dm;
	}

	// TODO implement this
	public static boolean inCache(RequestEntry)
	{
		return false;
	}

	public static void do_cycle(int cycle)
	{
		int len = l2_reqs.length;
		for (int core = 0, core < len; core++) {
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

			/* hit */
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
		requests.add();
	}
}
