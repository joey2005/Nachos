//Phase 3
package nachos.vm;

import java.util.Random;

import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

public class TLBScheduler {
	
	public TLBScheduler() {

	}
	
	public void init() {
		tlbsize = Machine.processor().getTLBSize();
		lru = new long[tlbsize];
		for (int i = 0; i < tlbsize; ++i) {
			lru[i] = Machine.timer().getTime();
		}
	}
	
	// clear TLB Entry
	public void clear(int processID, int vpn) {
		boolean intStatus = Machine.interrupt().disable();
		TranslationEntry newEntry = new TranslationEntry();
		newEntry.valid = false;
		for (int i = 0; i < tlbsize; ++i) {
			TranslationEntry entry = Machine.processor().readTLBEntry(i);
			if (entry.vpn == vpn) {
				writeBackTLBEntry(processID, i);
				writeTLBEntry(i, newEntry);
			}
		}
		Machine.interrupt().setStatus(intStatus);
	}
	
	public void clearTLB(int processID) {
		boolean intStatus = Machine.interrupt().disable();
		TranslationEntry newEntry = new TranslationEntry();
		newEntry.valid = false;
		for (int i = 0; i < tlbsize; ++i) {
			writeBackTLBEntry(processID, i);
			writeTLBEntry(i, newEntry);
		}
		Machine.interrupt().setStatus(intStatus);
	}
	
	public void addTLBEntry(int processID, TranslationEntry entry) {
		boolean intStatus = Machine.interrupt().disable();
		int at = getVictim();
		writeBackTLBEntry(processID, at);
		writePageEntry(processID, entry);
		writeTLBEntry(at, entry);
		Machine.interrupt().setStatus(intStatus);
	}

	public void writeTLBEntry(int at, TranslationEntry entry) {
		boolean intStatus = Machine.interrupt().disable();
		Machine.processor().writeTLBEntry(at, entry);
		setTime(at);
		Machine.interrupt().setStatus(intStatus);
	}
	
	public void writeBackTLB(int processID) {
		for (int i = 0; i < tlbsize; ++i) {
			writeBackTLBEntry(processID, i);
		}
	}
	
	public void writeBackTLBEntry(int processID, int at) {
		boolean intStatus = Machine.interrupt().disable();
		TranslationEntry entry = Machine.processor().readTLBEntry(at);
		if (entry.dirty) {
			writePageEntry(processID, entry);
		}
		Machine.interrupt().setStatus(intStatus);
	}
	
	public void writePageEntry(int processID, TranslationEntry entry) {
		VMKernel.pageScheduler.writePageEntry(processID, entry);
	}
	
	public TranslationEntry getPageEntry(LazyLoader loader, int processID, int vpn) {
		return VMKernel.pageScheduler.getPageEntry(loader, processID, vpn);
	}
	
	public boolean handleTLBMiss(LazyLoader loader, int processID, int vpn) {
		TranslationEntry entry = getPageEntry(loader, processID, vpn);
		
		if (entry == null) {
			return false;
		}
		
		addTLBEntry(processID, entry);
		/*
		System.err.println("======");
		for (int i = 0; i < tlbsize; ++i) {
			System.err.println(Machine.processor().readTLBEntry(i).vpn + " " + Machine.processor().readTLBEntry(i).valid);
		}*/
		
		return true;
	}
	
	public static int tlbsize;
	
	// least recently used
	private long[] lru;

	private int getVictim() {
		long time = lru[0];
		int at = 0;
		for (int i = 1; i < tlbsize; ++i) {
			if (lru[i] < time) {
				time = lru[i];
				at = i;
			}
		}
		return at;
	}
	
	private void setTime(int at) {
		lru[at] = Machine.timer().getTime();
	}
}
