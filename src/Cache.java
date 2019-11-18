/**
 * Cache.java
 * Author: Maher Khan
 * (Translated code given in project description from C to Java)
 */

/**
 * The cache is represented by a 2-D array of blocks. 
 * The first dimension of the 2D array is "nsets" which is the number of sets (entries)
 * The second dimension is "assoc", which is the number of blocks in each set.
 */
public class Cache
{
	public int nsets;                       // number of sets
	public int blocksize;                   // block size 
	public int assoc;                       // associativity
	public int miss_penalty;                // the miss penalty
	public CacheBlock[][] blocks;   // a pointer to the array of cache blocks
	public int core_id;

	public Cache(int size, int blocksize, int assoc, int core_id)
	{
		int nblocks = size * 1024 / blocksize;

		this.blocksize = blocksize;         // number of blocks in the cache
		this.nsets = nblocks / assoc;       // number of sets (entries) in the cache
		this.assoc = assoc;
		this.core_id = core_id;

		// Create the cahce blocks
		this.blocks = new CacheBlock[this.nsets][this.assoc];
		for(int i=0; i<this.nsets; i++)
		{
			for(int j=0; j<this.assoc; j++)
			{
				this.blocks[i][j] = new CacheBlock();
			}

		}
	}

	public void updateLRU(int index, int way)
	{
		int k;
		for (k=0; k < this.assoc; k++)
		{
			if(this.blocks[index][k].lru < this.blocks[index][way].lru)
			{
				this.blocks[index][k].lru = this.blocks[index][k].lru + 1;
			}
		}
		this.blocks[index][way].lru = 0;
	}

	/**
	 * This function is used to simulate a single cache access
	 * @param  address     [memory address]
	 * @param  access_type [0 for read, 1 for write]
	 * @return             [returns 0 (if a hit), 
	 *                              1 (if a miss but no dirty block is writen back), or
	 *                              2 (if a miss and a dirty block is writen back)]
	 */ 
	public int cache_access(long address, int access_type)
	{
		int block_address = (int) (address / (long) this.blocksize);
		int tag = block_address / this.nsets;
		int index = block_address - (tag * this.nsets);
		int i, way, max;

		/**
		 * Check for cache hit
		 */
		for (i=0; i < this.assoc; i++) {
			if (this.blocks[index][i].tag == tag 
					&& this.blocks[index][i].valid) {
				// HIT!
				this.updateLRU(index, i);
				if (access_type == 1)
					this.blocks[index][i].dirty = true;
				return 0;
			}
		}

		/**
		 * If not a hit, then it is a miss :(
		 * Now, we are going to look for an invalid entry
		 */
		for (way=0; way<this.assoc; way++) {
			if (!this.blocks[index][way].valid) {
				// Found an invalid entry
				this.blocks[index][way].valid = true;
				this.blocks[index][way].tag = tag;
				this.blocks[index][way].lru = way;
				this.updateLRU(index, way);
				this.blocks[index][way].dirty = (access_type != 0);
				this.blocks[index][i].address = address;
				return 1;

			}
		}

		/**
		 * No invalid block found :(
		 * Now, we are going to find the least recently used block
		 */
		max = this.blocks[index][0].lru;
		way = 0;
		for(i=0; i<this.assoc; i++) {
			if(this.blocks[index][i].lru > max) {
				max = this.blocks[index][i].lru;
				way = i;
			}
		}

		/**
		 * We found the least recently used block
		 * which is this.blocks[index][way]
		 */
		this.blocks[index][way].tag = tag;
		this.updateLRU(index, way);

		// Check the condition of the evicted block
		this.blocks[index][way].dirty = (access_type != 0);
		handleEvictedBlock(this.blocks[index][way]);

		this.blocks[index][i].address = address;
		if (!this.blocks[index][way].dirty)
			return 1;
		else
			return 2;
	}

	public CacheBlock getCacheBlock(RequestEntry req)
	{
		return getCacheBlock(req.address);
	}

	/*
	 * \brief Check if its in Cache
	 * \param[in] req RequestEntry Object 
	 *
	 * \return cacheBlock if it exist, null otherwise
	 */
	public CacheBlock getCacheBlock(long address)
	{
		int block_address = (int) (address / (long) this.blocksize);

		int tag = block_address / this.nsets;
		int index = block_address - (tag * this.nsets);

		// Checking for cache hit
		for(int i=0; i < this.assoc; i++)
		{
			/* NOTE: 
			 * L1 could be invalid but L2 can never be invalid
			 */
			if(this.blocks[index][i].tag == tag && this.blocks[index][i].valid) {
				// Hit and valid
				return this.blocks[index][i];
			}
		}

		// Consider all other case to be miss
		return null;
	}

	/*
	 * \brief Retrive data from memory
	 * \param[in] addr Address of data to be accessed
	 *
	 * \return true if hit, false if miss
	 */
	public boolean access(RequestEntry req)
	{
		System.out.println("you fucked up and called the abstract access looool");
		System.exit(1);
		return false;
	}

	public void handleEvictedBlock(CacheBlock cacheBlock)
	{
		System.out.println("you fucked up and called the abstract handle evicted block looool");
		System.exit(1);
	}
}
