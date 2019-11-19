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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;


public class CacheSimulator
{
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
			L1Cache l1cache = new L1Cache((int)Math.pow(2, n1), (int)Math.pow(2, b), (int)Math.pow(2, a), i);
			L2Piece l2piece = new L2Piece((int)Math.pow(2, n2), (int)Math.pow(2, b), (int)Math.pow(2, a), i);
			core_arr[i] = new Core(i);
			core_arr[i].l1cache = l1cache;
			core_arr[i].l2piece = l2piece;
		}

		int[] last_req_cycle = new int[number_of_cores];//keep track of last req to know the delta

		for (int i = 0; i < number_of_cores; i++) {
			last_req_cycle[i] = 0;
		}


		try(BufferedReader br = new BufferedReader(new FileReader(trace_file))) 
		{
			for(String line; (line = br.readLine()) != null; ) {
				splitted = line.split("[ |\t]", 4);
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
		
		L2Arbiter.init(dc, dm, number_of_cores, p, core_arr);

		Queue<RequestEntry>[] data_lookup = L2Arbiter.data_lookup;
		Queue<RequestEntry>[] mem_lookup = L2Arbiter.mem_lookup;
		ArrayList<MsgSentOutMap<Integer, Integer>>[] msg_sent_out = L2Arbiter.msg_sent_out;
		HashMap<CacheBlock, DirectoryEntry>[] directory = L2Arbiter.directory;
		
		do_simulation(core_arr);
	}

	public static void do_simulation(Core[] core_arr)
	{
		System.out.println("Starting Simulation\n\n");
		int cycle = 0;
		boolean accesses_left = true;
		while (accesses_left) {
			//do single cycle
			for (Core c : core_arr)
				c.do_cycle();
			L2Arbiter.do_cycle(cycle);
			accesses_left = update_accesses_left(core_arr);
			if (!accesses_left) {
				System.out.println("\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
				System.out.println("Simulation Done at cycle "+cycle);
				System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n");
				break;
			}
			cycle++;
		}
		if (Debug.debug_mode) {
			System.out.println("******************************");
			System.out.println("******************************");
			print_cache_states(core_arr);
			System.out.println("******************************");
			System.out.println("******************************");
			print_cache_states_l2(core_arr);
		}
		print_global_stats(core_arr);
	}

	public static void print_cache_states(Core[] core_arr)
	{
		int width  = core_arr[0].l1cache.blocks[0].length;
		int height = core_arr[0].l1cache.blocks.length;
		boolean last_row_empty = false;
		boolean this_row_empty = true;

		System.out.println("Printing cache address, tag, valid, dirty");
		for (Core core : core_arr) {
			System.out.println("Core " + core.core_num + "'s L1 cache:");

			for (int row = 0; row < height; row++) {
				last_row_empty = this_row_empty;
				this_row_empty = true;

				int addr = row * width;
				String addr_str = String.format("%08X ", addr);
				String print_str = addr_str;
				for (int col = 0; col < width; col++) {
					print_str += "| ";
					CacheBlock cb = core.l1cache.blocks[row][col];
					print_str += String.format("%08X ", cb.tag);
					print_str += cb.valid ? "v " : "i ";
					print_str += cb.dirty ? "d " : "c ";
					switch (cb.state) {
					case INVALIDATED:
						print_str += "I ";
						break;
					case EXCLUSIVE:
						print_str += "E ";
						break;
					case SHARED:
						print_str += "S ";
					}

					if (cb.valid && this_row_empty)
						this_row_empty = false;
				}

				if (this_row_empty && !last_row_empty)
					System.out.println(addr_str + "| *");
				else if (!this_row_empty)
					System.out.println(print_str);
				else if (row + 1 == height)
					System.out.println(addr_str + "| *");
			}
			System.out.println();
		}
	}

	public static void print_cache_states_l2(Core[] core_arr)
	{
		int width  = core_arr[0].l2piece.blocks[0].length;
		int height = core_arr[0].l2piece.blocks.length;
		boolean last_row_empty = false;
		boolean this_row_empty = true;

		System.out.println("Printing cache address, tag, valid, dirty");
		for (Core core : core_arr) {
			System.out.println("Core " + core.core_num + "'s L1 cache:");

			for (int row = 0; row < height; row++) {
				last_row_empty = this_row_empty;
				this_row_empty = true;

				int addr = row * width;
				String addr_str = String.format("%08X ", addr);
				String print_str = addr_str;
				for (int col = 0; col < width; col++) {
					print_str += "| ";
					CacheBlock cb = core.l2piece.blocks[row][col];
					print_str += String.format("%08X ", cb.tag);
					print_str += cb.valid ? "v " : "i ";
					print_str += cb.dirty ? "d " : "c ";
					switch (cb.state) {
					case INVALIDATED:
						print_str += "I ";
						break;
					case EXCLUSIVE:
						print_str += "E ";
						break;
					case SHARED:
						print_str += "S ";
					}

					if (cb.valid && this_row_empty)
						this_row_empty = false;
				}

				if (this_row_empty && !last_row_empty)
					System.out.println(addr_str + "| *");
				else if (!this_row_empty)
					System.out.println(print_str);
				else if (row + 1 == height)
					System.out.println(addr_str + "| *");
			}
			System.out.println();
		}
	}

	public static void print_global_stats(Core[] core_arr)
	{
		System.out.println("******************************");
		System.out.println("******************************");
		System.out.println("Global Stats:\n");
		
		System.out.println("Data msgs: "+L2Arbiter.num_data_msgs);
		System.out.println("Control msgs: "+L2Arbiter.num_control_msgs);
		System.out.println("\n");

		int total_misses = 0;
		int total_miss_penalties = 0;

		for (Core c : core_arr) {
			total_misses += c.total_requests_missed;
			total_miss_penalties += c.total_miss_penalty;
			System.out.println("Core "+c.core_num+" completed at cycle "+c.cycle_done);
		}
		double avg_miss_penalty = (double)total_miss_penalties / (double)total_misses;
		System.out.println("\nTotal L1 misses: "+total_misses);
		System.out.println("Total L1 miss time in cycles: "+total_miss_penalties);
		System.out.println("Average L1 miss time in cycles: "+avg_miss_penalty);


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
