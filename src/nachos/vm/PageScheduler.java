/*
 * Phase 3
 * use LRU;
 */

package nachos.vm;

import java.util.HashMap;
import java.util.LinkedList;

import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;
import nachos.threads.Lock;

public class PageScheduler {
	public PageScheduler() {
		swapFile = new SwapFile();
		pageTable = new InvertedPageTable();
	}

	public void init() {
		queue.clear();
		int length = Machine.processor().getNumPhysPages();
		for (int i = 0; i < length; ++i) {
			queue.add(new Integer(i));
		}

		swapFile.init();
		
		pageLock = new Lock();
	}
	
	// clock page scheduler
	private LinkedList<Integer> queue = new LinkedList<Integer>();
	
	private int getVictim() {
		while (true) {
			int ppn = queue.removeFirst();
			TranslationEntry entry = pageTable.getTranslationEntry(ppn);
			if (entry == null || !entry.used) {
				return ppn;
			}
			entry.used = false;
			int processID = pageTable.getProcessID(ppn);
			pageTable.put(processID, entry);
			queue.add(new Integer(ppn));
		}
	}
	
	// clear all pages when unloadSections() is invoked
	public void clearPage(int processID) {
		pageTable.removeProcessPage(processID);
		swapFile.clearPage(processID);
	}
	
	public void writePageEntry(int processID, TranslationEntry entry) {
		pageTable.put(processID, entry);
	}
	
	public TranslationEntry getPageEntry(LazyLoader loader, int processID, int vpn) {
		TranslationEntry entry = pageTable.getTranslationEntry(processID, vpn);
		
		if (entry == null) {
			handlePageFault(loader, processID, vpn);
			entry = pageTable.getTranslationEntry(processID, vpn);
		}
		
		return entry;
	}
	
	public boolean handlePageFault(LazyLoader loader, int processID, int vpn) {
		pageLock.acquire();
		
		int tmpppn = getVictim();
		int tmppid = pageTable.getProcessID(tmpppn);
		int tmpvpn = pageTable.getvpn(tmpppn);
		
		//swapToFile
		if (tmppid == processID) {
			VMKernel.tlbScheduler.clear(tmppid, tmpvpn);
		}
		TranslationEntry entry = pageTable.getTranslationEntry(tmpppn);
		//check if dirty
		swapFile.swapToFile(tmppid, tmpvpn, entry);
		pageTable.removePage(tmppid, tmpvpn);
		
		//swapToMemory
		int ppn = tmpppn;
		entry = swapFile.swapToMemory(processID, vpn, ppn);
		boolean needToLoadSection = entry == null;
		if (needToLoadSection) {
			entry = new TranslationEntry(vpn, ppn, true, false, false, false);
		}
		pageTable.put(processID, entry);
		queue.add(new Integer(ppn));
		
		//load pages
		if (needToLoadSection) {
			if (loader.isCodeSection(vpn)) {
				entry.readOnly = loader.loadSection(vpn, ppn).readOnly;
			} else {//flush
				byte[] data = Machine.processor().getMemory();
				int start = Processor.makeAddress(ppn, 0);
				int end = start + Processor.pageSize;
				for (int i = start; i < end; i++) {
					data[i] = 0;
				}
			}
		}
		
		pageLock.release();
		
		return true;
	}
	
	public InvertedPageTable pageTable;
	
	//inverted page table
	private class InvertedPageTable {
		public InvertedPageTable() {
			int length = Machine.processor().getNumPhysPages();
			coreMapPID = new int[length];
			coreMapVPN = new int[length];
			coreMapEntry = new TranslationEntry[length];
			mapping = new HashMap<Pair, Integer>();
		}
		
		public TranslationEntry getTranslationEntry(Integer ppn) {
			if (ppn == null || ppn < 0 || ppn >= coreMapEntry.length) {
				return null;
			}
			return coreMapEntry[ppn];
		}
		
		public TranslationEntry getTranslationEntry(int processID, int vpn) {
			return getTranslationEntry(mapping.get(new Pair(processID, vpn)));
		}
		
		public void put(int processID, TranslationEntry entry) {
			coreMapPID[entry.ppn] = processID;
			coreMapVPN[entry.ppn] = entry.vpn;
			coreMapEntry[entry.ppn] = entry;
			mapping.put(new Pair(processID, entry.vpn), new Integer(entry.ppn));
		}
		
		public int getProcessID(Integer ppn) {
			if (ppn == null || ppn < 0 || ppn >= coreMapPID.length) {
				return -1;
			}
			return coreMapPID[ppn];
		}
		
		public int getvpn(Integer ppn) {
			if (ppn == null || ppn < 0 || ppn >= coreMapVPN.length) {
				return -1;
			}
			return coreMapVPN[ppn];			
		}
		
		public void removeProcessPage(int processID) {
			for (int i = 0; i < coreMapPID.length; ++i) {
				if (coreMapPID[i] == processID) {
					removePage(processID, coreMapVPN[i]);
				}
			}
		}
		
		public void removePage(int processID, int vpn) {
			Pair del = new Pair(processID, vpn);
			if (mapping.containsKey(del)) {
				int ppn = mapping.remove(del);
				coreMapPID[ppn] = -1;
				coreMapVPN[ppn] = -1;
				coreMapEntry[ppn] = null;
			}
		}
		
		public void removePage(int ppn) {
			removePage(coreMapPID[ppn], coreMapVPN[ppn]);
		}
		
		private int[] coreMapPID;
		private int[] coreMapVPN;
		private TranslationEntry[] coreMapEntry;
		
		private HashMap<Pair, Integer> mapping;
	}
	
	private Lock pageLock;
	
	public SwapFile swapFile;
}
