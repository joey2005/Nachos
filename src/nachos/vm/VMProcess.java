package nachos.vm;

import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;
import nachos.userprog.UserProcess;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		VMKernel.tlbScheduler.clearTLB(processID);
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {

	}
	
	public TranslationEntry getPP(int vpn, boolean writeBit) {
		TranslationEntry entry = VMKernel.pageScheduler.getPageEntry(loader, processID, vpn);
		if (entry == null || entry.readOnly && writeBit) {
			return null;
		}
		return entry;
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {//use lazy loader
		loader = new LazyLoader(coff);
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		VMKernel.tlbScheduler.clearTLB(processID);
		VMKernel.pageScheduler.clearPage(processID);
		coff.close();
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause
	 *            the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionTLBMiss:
			VMKernel.tlbScheduler.handleTLBMiss(loader, processID, Processor.pageFromAddress(Machine.processor().readRegister(Processor.regBadVAddr)));
			break;
			
		case Processor.exceptionPageFault:
			VMKernel.pageScheduler.handlePageFault(loader, processID, Processor.pageFromAddress(Machine.processor().readRegister(Processor.regBadVAddr)));
			break;
		
		default:
			super.handleException(cause);
			break;
		}
	}
	
	private LazyLoader loader;

	private static final int pageSize = Processor.pageSize;
	private static final char dbgProcess = 'a';
	private static final char dbgVM = 'v';
}
