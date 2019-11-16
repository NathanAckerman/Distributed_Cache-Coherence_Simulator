public class L1Cache extends Cache
{
	// TODO init instance vars

	public L1Cache(int miss_penalty, int core_id, ...)
	{
	}

	public boolean access(RequestEntry req)
	{
		/* hit = no cache coherence necessary */
		if (inCache(req))
			return true;

		/* miss */
		L2Arbiter.queueRequest(core_id, req);
		return false;
	}
}
