//Phase 3
package nachos.vm;

import nachos.machine.Coff;
import nachos.machine.CoffSection;
import nachos.machine.Lib;
import nachos.machine.TranslationEntry;

public class LazyLoader {
	public LazyLoader() {
		
	}
	
	public LazyLoader(Coff coffFile) {
		coff = coffFile;
		
		int sectionCount = coff.getNumSections();
		
		numPages = 0;
		for (int s = 0; s < sectionCount; ++s) {
			numPages += coff.getSection(s).getLength();
		}
		pageSectionNum = new int[numPages];
		pageSectionOffset = new int[numPages];
		
		for (int s = 0; s < sectionCount; ++s) {
			CoffSection section = coff.getSection(s);
			int len = section.getLength();
			for (int i = 0; i < len; ++i) {
				int vpn = section.getFirstVPN() + i;
				pageSectionNum[vpn] = s;
				pageSectionOffset[vpn] = i;
				//System.err.println(vpn);
			}
		}
	}

	public TranslationEntry loadSection(int vpn, int ppn) {
		//System.err.println(vpn + " " + ppn + " " + pageSectionNum.length);
		TranslationEntry entry;
		if (vpn >= 0 && vpn < numPages) {
			CoffSection section = coff.getSection(pageSectionNum[vpn]);
			entry = new TranslationEntry(vpn, ppn, true, section.isReadOnly(), false, false);
			section.loadPage(pageSectionOffset[vpn], ppn);
		} else {
			entry = new TranslationEntry(vpn, ppn, true, false, false, false); 
		}
		return entry;
	}
	
	private Coff coff;
	private int numPages;
	private int sectionCount;
	private int[] pageSectionNum;
	private int[] pageSectionOffset;
}
