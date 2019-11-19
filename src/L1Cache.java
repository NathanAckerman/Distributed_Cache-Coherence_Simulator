public class L1Cache extends Cache
{
	public L1Cache(int size, int blocksize, int assoc, int core_id)
	{
		super(size, blocksize, assoc, core_id);
	}

	public void updateEntry(RequestEntry req) 
	{
		CacheBlock cacheBlock = this.getCacheBlock(req);
		if (cacheBlock == null) {
			System.out.println("Panic");
			System.exit(1);
		}
		
		if (req.requestType == CacheState.SHARED) {
			cacheBlock.state = CacheState.SHARED;
		}else if(req.requestType == CacheState.EXCLUSIVE) {
			cacheBlock.state = CacheState.INVALIDATED;
			cacheBlock.valid = false;
		}
	}


	// Why the boolean return type? 
	public boolean access(RequestEntry req)
	{
		CacheBlock cacheBlock = getCacheBlock(req);

		int ret = cache_access(req.address, req.rw);
		// hit
		if (ret == 0)
			if (req.rw == 0 || (req.rw == 1 && cacheBlock.state == CacheState.EXCLUSIVE))
				return true;

		// otherwise send to arbiter
		L2Arbiter.queueRequest(req);
		return false;
	}

	public void handleEvictedBlock(CacheBlock cacheblock)
	{
		L2Arbiter.removeL1(cacheblock.address, core_id);
	}

	public void invalidate(long address)
	{
		CacheBlock cache_block = getCacheBlock(address);
		cache_block.valid = false;
	}

}
