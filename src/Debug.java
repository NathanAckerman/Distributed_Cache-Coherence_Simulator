public final class Debug {
	public static boolean debug_mode = false;
	
	public static void println(String str)
	{
		if (debug_mode) {
			System.out.println(str);
		}
	}

}
