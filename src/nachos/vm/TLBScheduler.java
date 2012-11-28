//Phase 3
package nachos.vm;

import java.util.Random;

import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

public class TLBScheduler {
	
	public TLBScheduler() {
		randgen = new Random();
		tlbsize = Machine.processor().getTLBSize();
	}
	
	public void init() {

	}
	
	// clear TLB Entry
	public void clear(int processID, int vpn) {
		boolean intStatus = Machine.interrupt().disable();
		for (int i = 0; i < tlbsize; ++i) {
			TranslationEntry entry = Machine.processor().readTLBEntry(i);
			if (entry.vpn == vpn) {
				writeBackTLBEntry(processID, i);
				TranslationEntry newEntry = new TranslationEntry(entry);
				newEntry.valid = false;
				writeTLBEntry(i, newEntry);
			}
		}
		Machine.interrupt().setStatus(intStatus);
	}
	
	public void clearTLB(int processID) {
		boolean intStatus = Machine.interrupt().disable();
		for (int i = 0; i < tlbsize; ++i) {
			writeBackTLBEntry(processID, i);
			TranslationEntry newEntry = new TranslationEntry(Machine.processor().readTLBEntry(i));
			newEntry.valid = false;
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
		
		return true;
	}
	
	public static int tlbsize;
	
	// Randomized
	private static Random randgen;

	private int getVictim() {
		for (int i = 0; i < tlbsize; ++i) {
			TranslationEntry entry = Machine.processor().readTLBEntry(i);
			if (entry.used) {
				return i;
			}
		}
		return randgen.nextInt(tlbsize);
	}
}
