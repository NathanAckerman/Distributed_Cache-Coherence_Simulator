	/**
	 * This is a simple cache block.
	 * Note that no actual data will be stored in the cache
	 */
	public class CacheBlock
	{
		public long tag;
		public Boolean valid;
		public Boolean dirty;
		public int lru; // to be used to build the LRU stack for the blocks in a cache set
		public CacheState state;

		public CacheBlock()
		{
			this.tag = 0;
			this.valid = false;
			this.dirty = false;
			this.lru = 0;
			this.state = CacheState.INVALIDATED;
		}
	}