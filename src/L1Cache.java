public class L1Cache extends Cache
{
	// TODO init instance vars

	public L1Cache(int miss_penalty, int core_id, ...)
	{
	}


	// Why the boolean return type? 
	public boolean access(RequestEntry req)
	{
		CacheBlock cacheBlock = inCache(req);

		// Hit and its valid
		if (cacheBlock != null) {
			if (req.rw == 0 || (req.rw == 1 && cacheBlock.state == CacheState.EXCLUSIVE)){
				// Read Request
				return true;
			}
		}

		// Miss or hit but invalid
		// send it to arbiter
		L2Arbiter.queueRequest(core_id, req);
		return false;
	}
}
