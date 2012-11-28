package nachos.vm;

import nachos.userprog.UserKernel;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
		
		tlbScheduler = new TLBScheduler();
		pageScheduler = new PageScheduler();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
		tlbScheduler.init();
		pageScheduler.init();
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		pageScheduler.swapFile.close();
		super.terminate();
	}

	private static final char dbgVM = 'v';
	
	public static TLBScheduler tlbScheduler;
	
	public static PageScheduler pageScheduler;
}
