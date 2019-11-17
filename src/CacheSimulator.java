/**
 * CacheSimulator.java
 * Author: Maher Khan
 * (Translated code given in project description from C to Java)
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.IllegalArgumentException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Math;


public class CacheSimulator
{
	private Cache cache;

	public CacheSimulator(int size, int blocksize, int assoc)
	{
		this.cache = new Cache(size, blocksize, assoc);
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
			return 2;
		}
	}

	public static void main(String [] args) throws FileNotFoundException, IOException
	{
		if (args.length == 3) {
			Debug.debug_mode = true;
		} else {
			Debug.debug_mode = false;
		}

		String config_file = args[0];
		String trace_file = args[1];

		//vars for config file
		int p=0;
		int n1=0;
		int n2=0;
		int b=0;
		int a=0;
		int dc=0;
		int dm=0;
		//config file format:
		//p=num
		//n1=num
		//n2=num
		//b=num
		//a=num
		//dc=num
		//dm=num


		//parse config file
		try(BufferedReader br = new BufferedReader(new FileReader(config_file))) 
		{
			String[] splitted = new String[2];
			for(String line; (line = br.readLine()) != null; ) {
				splitted = line.split("=");
				switch (splitted[0]) {
					case "p":
						p = Integer.parseInt(splitted[1]);
						break;
					case "n1":
						n1 = Integer.parseInt(splitted[1]);
						break;
					case "n2":
						n2 = Integer.parseInt(splitted[1]);
						break;
					case "b":
						b = Integer.parseInt(splitted[1]);
						break;
					case "a":
						a = Integer.parseInt(splitted[1]);
						break;
					case "dc":
						dc = Integer.parseInt(splitted[1]);
						break;
					case "dm":
						dm = Integer.parseInt(splitted[1]);
						break;
					default:
						System.out.println("Error: malformed config file");
				}
			}
		}


		//parse trace file


		String[] splitted;
		int req_cycle;
		int core_id;
		long address;
		int access_type;
		int res;

		int number_of_cores = (int)Math.pow(2,p);
		Core[] core_arr = new Core[number_of_cores];

		for (int i = 0; i < number_of_cores; i++) {
			core_arr[i] = new Core(i);
		}

		int[] last_req_cycle = new int[number_of_cores];//keep track of last req to know the delta

		for (int i = 0; i < number_of_cores; i++) {
			last_req_cycle[i] = 0;
		}


		try(BufferedReader br = new BufferedReader(new FileReader(trace_file))) 
		{
			for(String line; (line = br.readLine()) != null; ) {
				splitted = line.split(" ", 4);
				req_cycle = Integer.parseInt(splitted[0]);
				core_id = Integer.parseInt(splitted[1]);
				address = Long.decode(splitted[3]);
				access_type = Integer.parseInt(splitted[2]);

				int last_req_for_that_core = last_req_cycle[core_id];
				last_req_cycle[core_id] = req_cycle;
				int req_delta = req_cycle - last_req_for_that_core;
				core_arr[core_id].add_request_entry(req_delta, address, access_type);

			}
		}
		
		//TODO build caches on cores
		
		do_simulation(core_arr);
		

	}

	public static void do_simulation(Core[] core_arr)
	{
		System.out.println("Starting Simulation\n\n");
		int cycle = 0;
		boolean accesses_left = true;
		while (accesses_left) {
			//do single cycle
			for (Core c : core_arr) {
				c.do_cycle();
			}
			accesses_left = update_accesses_left(core_arr);
			if (!accesses_left) {
				System.out.println("Simulation Done at cycle "+cycle);
				break;
			}
			cycle++;
		}
		if (Debug.debug_mode) {
			System.out.println("\n\n");
			System.out.println("******************************");
			System.out.println("******************************");
			print_cache_states(core_arr);
		}
		System.out.println("\n\n");
		System.out.println("******************************");
		System.out.println("******************************");
		System.out.println("Global Stats: ");
		print_global_stats(core_arr);
	}

	public static void print_cache_states(Core[] core_arr)
	{
		//TODO implement this method
		//are we print l2 also? in which case we need arbiter reference?
	}

	public static void print_global_stats(Core[] core_arr)
	{
		//TODO still need to track and print breakdown of control/data messages
		int total_misses = 0;
		int total_miss_penalties = 0;

		for (Core c : core_arr) {
			total_misses += c.total_requests_missed;
			total_miss_penalties += c.total_miss_penalty;
		}
		double avg_miss_penalty = (double)total_misses / (double)total_miss_penalties;
		System.out.println("\nTotal l1 misses: "+total_misses);
		System.out.println("\nTotal l1 miss time in cycles: "+total_miss_penalties);
		System.out.println("\nAverage l1 miss time in cycles: "+avg_miss_penalty);


	}

	public static boolean update_accesses_left(Core[] core_arr)
	{
		boolean found_entries = false;
		for (Core c : core_arr) {
			found_entries = found_entries | (c.get_dq_size() != 0);
		}
		return found_entries;
	}
}
