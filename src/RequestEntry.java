class RequestEntry
{
	public int delta;
	public long address;
	public int rw; // 0 for read, 1 for write
	public boolean resolved = false;
	public int cycle_issued;
	public L1Cache l1cache; 
	public CacheState requestType;
	public int requesterCoreNum;

	public RequestEntry(int delta, long address, int rw, L1Cache l1cache, CacheState requestType, int requesterCoreNum)
	{
		this.delta = delta;
		this.address = address;
		this.rw = rw;
		this.l1cache = l1cache;
		this.requestType = requestType;
		this.requesterCoreNum = requesterCoreNum;
	}

}
