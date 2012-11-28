package nachos.vm;

import java.util.HashMap;
import java.util.LinkedList;

import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;

public class SwapFile {
	public SwapFile() {
		filename = "NachosDiskFile";
		mapping = new HashMap<Pair, TranslationEntry>();
		usedPage = new HashMap<Pair, Integer>();
		freePage = new LinkedList<Integer>(); 
	}
	
	public SwapFile(String name) {
		filename = name;
		mapping = new HashMap<Pair, TranslationEntry>();
		usedPage = new HashMap<Pair, Integer>();
		freePage = new LinkedList<Integer>(); 
	}
	
	public void init() {
		pagesize = Machine.processor().pageSize;
		swapFile = Machine.stubFileSystem().open(filename, true);
	}
	
	//invoked by unloadsections()
	public void clearPage(int processID) {
		Pair[] keys = new Pair[1];
		keys = usedPage.keySet().toArray(keys);
		for (int i = 0; i < keys.length; ++i) {//(pid, vpn)
			if (keys[i].first == processID) {
				mapping.remove(keys[i]);
				freePage.add(usedPage.remove(keys[i]));
			}
		}
	}
	
	public void close() {
		swapFile.close();
		Machine.stubFileSystem().remove(filename);
	}

	public int swapToFile(int processID, int vpn, TranslationEntry entry) {
		if (entry == null) {
			return 0;
		}
		Pair p = new Pair(processID, vpn);
		int page = usedPage.containsKey(p) ? usedPage.get(p) : allocate();
		mapping.put(p, entry);
		usedPage.put(p, new Integer(page));
		return swapFile.write(calcOffset(page), Machine.processor().getMemory(), Processor.makeAddress(page, 0), pagesize);
	}
	
	public TranslationEntry swapToMemory(int processID, int vpn, int ppn) {
		Pair p = new Pair(processID, vpn);
		if (!mapping.containsKey(p)) {
			return null;
		}
		int page = usedPage.get(p);
		int readLen = swapFile.read(calcOffset(page), Machine.processor().getMemory(), Processor.makeAddress(page, 0), pagesize);
		if (readLen < pagesize) {
			return null;
		}
		TranslationEntry entry = mapping.get(p);
		entry.ppn = ppn;
		entry.dirty = false;
		entry.used = false;
		return entry;
	}
	
	private int calcOffset(int page) {
		return page * pagesize;
	}
	
	private int allocate() {
		if (freePage.isEmpty()) {
			return pageCount++;
		}
		return freePage.removeFirst();
	}
	
	private HashMap<Pair, TranslationEntry> mapping;
	private HashMap<Pair, Integer> usedPage;
	private LinkedList<Integer> freePage; 
	
	private int pageCount = 0;
	
	private String filename;
	private static int pagesize;
	OpenFile swapFile;
}
