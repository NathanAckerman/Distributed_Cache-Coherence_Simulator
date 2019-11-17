class RequestEntry
{
	public int delta;
	public long address;
	public int rw; // 0 for read, 1 for write
	public boolean resolved = false;
	public int cycle_issued;

	public RequestEntry(int delta, long address, int rw)
	{
		this.delta = delta;
		this.address = address;
		this.rw = rw;
	}

}
