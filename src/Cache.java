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

	public Cache(int size, int blocksize, int assoc)
	{
		int nblocks = size * 1024 / blocksize;

		this.blocksize = blocksize;         // number of blocks in the cache
		this.nsets = nblocks / assoc;       // number of sets (entries) in the cache
		this.assoc = assoc;


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
		for (k=0; k < this.cache.assoc; k++)
		{
			if(this.cache.blocks[index][k].lru < this.cache.blocks[index][way].lru)
			{
				this.cache.blocks[index][k].lru = this.cache.blocks[index][k].lru + 1;
			}
		}
		this.cache.blocks[index][way].lru = 0;
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
		int block_address = (int) (address / (long) this.cache.blocksize);
		int tag = block_address / this.cache.nsets;
		int index = block_address - (tag * this.cache.nsets);
		int i, way, max;

		/**
		 * Check for cache hit
		 */
		for(i=0; i < this.cache.assoc; i++)
		{
			if(this.cache.blocks[index][i].tag == tag 
					&& this.cache.blocks[index][i].valid)
			{
				// HIT!
				this.updateLRU(index, i);
				if(access_type == 1)
				{
					this.cache.blocks[index][i].dirty = true;
				}
				return 0;
			}
		}

		/**
		 * If not a hit, then it is a miss :(
		 * Now, we are going to look for an invalid entry
		 */
		for(way=0; way<this.cache.assoc; way++)
		{
			if(!this.cache.blocks[index][way].valid)
			{
				// Found an invalid entry
				this.cache.blocks[index][way].valid = true;
				this.cache.blocks[index][way].tag = tag;
				this.cache.blocks[index][way].lru = way;
				this.updateLRU(index, way);
				if(access_type==0)
				{
					this.cache.blocks[index][way].dirty = false; 
				}
				else
				{
					this.cache.blocks[index][way].dirty = true;
				}
				return 1;

			}
		}

		/**
		 * No dirty block found :(
		 * Now, we are going to find the least recently used block
		 */
		max = this.cache.blocks[index][0].lru;
		way = 0;
		for(i=0; i<this.cache.assoc; i++)
		{
			if(this.cache.blocks[index][i].lru > max)
			{
				max = this.cache.blocks[index][i].lru;
				way = i;
			}
		}

		/**
		 * We found the least recently used block
		 * which is this.blocks[index][way]
		 */
		this.cache.blocks[index][way].tag = tag;
		this.updateLRU(index, way);

		// Check the condition of the evicted block
		if(!this.cache.blocks[index][way].dirty)
		{
			// The evicted block is clean!
			this.cache.blocks[index][way].dirty = (access_type==0 ? false : true) ;
			return 1;
		}
		else
		{
			// The evicted block is dirty
			this.cache.blocks[index][way].dirty = (access_type==0 ? false : true);
			// Let L2 know that the block is dirty
			return 2;
		}
	}

	/*
	 * \brief Check if its in Cache
	 * \param[in] req RequestEntry Object 
	 *
	 * \return true if hit, false if miss
	 */
	public CacheBlock inCache(RequestEntry req)
	{
		int block_address = (int) (req.address / (long) this.cache.blocksize);

		int tag = block_address / this.cache.nsets;
		int index = block_address - (tag * this.cache.nsets);

		// Checking for cache hit
		for(int i=0; i < this.cache.assoc; i++)
		{
			/* NOTE: 
			 * L1 could be invalid but L2 can never be invalid
			 */
			if(this.cache.blocks[index][i].tag == tag && this.cache.blocks[index][i].valid) {
				// Hit and valid
				return this.cache.blocks[index][i];
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
		return false;
	}
}
