class RequestEntry
{
	int delta;
	long address;
	int rw; // 0 for read, 1 for write
	boolean resolved = false;

	public RequestEntry(int delta, long address, int rw)
	{
		this.delta = delta;
		this.address = address;
		this.rw = rw;
	}

}
