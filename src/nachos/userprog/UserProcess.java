package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		/* useless
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i = 0; i < numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
		*/
		
		processID = processCount++;
		processTable.put(new Integer(processID), this);
		
		descriptor.put(UserKernel.console.openForReading(), 0);
		descriptor.put(UserKernel.console.openForWriting(), 1);
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		thread = new UThread(this).setName(name);
		thread.fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr
	 *            the starting virtual address of the null-terminated string.
	 * @param maxLength
	 *            the maximum number of characters in the string, not including
	 *            the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 *         found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		/*
		int amount = readVirtualMemory(vaddr, data, 0, data.length);
		for (int i = 0; i < data.length; ++i) {
			System.out.print(data[i] + " ");
		}
		System.out.println();
		return amount;*/
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @param offset
	 *            the first byte to write in the array.
	 * @param length
	 *            the number of bytes to transfer from virtual memory to the
	 *            array.
	 * @return the number of bytes successfully transferred.
	 */
	// Phase 2 Task 2
	
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();
		
		if (vaddr < 0 || vaddr >= memory.length) {
			return 0;
		}

		/*
		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(memory, vaddr, data, offset, amount);
		*/
		
		// Virtual address [vaddr, vaddrEnd)
		int vaddrEnd = vaddr + length - 1;
		// virtual address in that start virtual page
		int startVAddr = Processor.offsetFromAddress(vaddr);
		// Virtual Page num intervals [VPNStart, VPNEnd]
		int VPNStart = Processor.pageFromAddress(vaddr);
		int VPNEnd = Processor.pageFromAddress(vaddrEnd);
		int amount = 0;
		for (int vpn = VPNStart; vpn <= VPNEnd; ++vpn) {
			// do copy
			int len = Math.min(length - amount, pageSize - startVAddr);
			TranslationEntry PP = getPP(vpn, false);
			if (PP == null) {
				return amount;
			}
			System.arraycopy(memory, Processor.makeAddress(PP.ppn, startVAddr), data, offset, len);
			offset += len;
			amount += len;
			
			//clear start virtual address
			startVAddr = 0;
		}

		return amount;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		/*
		int amount = writeVirtualMemory(vaddr, data, 0, data.length);
		for (int i = 0; i < data.length; ++i) {
			System.out.print(data[i] + " ");
		}
		System.out.println();
		return amount;*/
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @param offset
	 *            the first byte to transfer from the array.
	 * @param length
	 *            the number of bytes to transfer from the array to virtual
	 *            memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();
		
		if (vaddr < 0 || vaddr >= memory.length) {
			return 0;
		}

		/*
		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(data, offset, memory, vaddr, amount);
		*/
		
		// Virtual address [vaddr, vaddrEnd)
		int vaddrEnd = vaddr + length - 1;
		// virtual address in that start virtual page
		int startVAddr = Processor.offsetFromAddress(vaddr);
		// Virtual Page num intervals [VPNStart, VPNEnd]
		int VPNStart = Processor.pageFromAddress(vaddr);
		int VPNEnd = Processor.pageFromAddress(vaddrEnd);
		int amount = 0;
		for (int vpn = VPNStart; vpn <= VPNEnd; ++vpn) {
			// do copy
			int len = Math.min(length - amount, pageSize - startVAddr);
			TranslationEntry PP = getPP(vpn, true);
			// exception
			if (PP == null) {
				return amount;
			}
			System.arraycopy(data, offset, memory, Processor.makeAddress(PP.ppn, startVAddr), len);
			offset += len;
			amount += len;
			
			//clear start virtual address
			startVAddr = 0;
		}

		return amount;
	}
	
	// Phase 2 Task 2
	TranslationEntry getPP(int vpn, boolean writeBit) {
		if (vpn >= pageTable.length) {
			return null;
		}
		if (pageTable[vpn].readOnly && writeBit) {
			return null;
		}
		pageTable[vpn].used = true;
		pageTable[vpn].dirty |= writeBit;
		return pageTable[vpn];
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		} catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib
					.assertTrue(writeVirtualMemory(entryOffset,
							stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib
					.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib
					.assertTrue(writeVirtualMemory(stringOffset,
							new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	
	// Phase 2 task 2
	protected boolean loadSections() {
		int[] ppList = UserKernel.allocatePage(numPages);
		if (ppList == null) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}
		
		pageTable = new TranslationEntry[numPages];

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				/*
				// for now, just assume virtual addresses=physical addresses
				 *
				 */

				int ppn = ppList[vpn];
				pageTable[vpn] = new TranslationEntry(vpn, ppn, true, section.isReadOnly(), false, false);
				section.loadPage(i, ppn);
			}
		}
		
		// next comes the stack; stack pointer initially points to top of it
		// and finally reserve 1 page for arguments
		int count = stackPages + 1;
		for (int i = 0; i < count; ++i) {
			int at = numPages - count + i;
			pageTable[at] = new TranslationEntry(at, ppList[at], true, false, false, false);
		}

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		for (int i = 0; i < numPages; ++i) {
			UserKernel.releasePage(pageTable[i].ppn);
		}
		pageTable = null;
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < Processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {
		if (UserKernel.rootProcess != this) {
			return 0;
		}

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall
	 *            the syscall number.
	 * @param a0
	 *            the first syscall argument.
	 * @param a1
	 *            the second syscall argument.
	 * @param a2
	 *            the third syscall argument.
	 * @param a3
	 *            the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	
	/**
	 * Attempt to open the named disk file, creating it if it does not exist,
	 * and return a file descriptor that can be used to access the file.
	 *
	 * Note that creat() can only be used to create files on disk; creat() will
	 * never return a file descriptor referring to a stream.
	 *
	 * Returns the new file descriptor, or -1 if an error occurred.
	 */
	private int handleCreate(int namePos) {
		String name = readVirtualMemoryString(namePos, maxLength);
		//name does not exist
		if (name == null || name.length() > maxLength) {
			return -1;
		}
		//no free descriptor
		if (!descriptor.hasFree()) {
			return -1;
		}
		//it is already in removeList
		if (removeList.contains(name)) {
			return -1;
		}
		OpenFile file = UserKernel.fileSystem.open(name, true);
		if (file == null) {
			return -1;
		}
		int newDes = descriptor.getFree();
		descriptor.put(file, newDes);
		fileStore.put(name, file);
		return newDes;
	}
	
	/**
	 * Attempt to open the named file and return a file descriptor.
	 *
	 * Note that open() can only be used to open files on disk; open() will never
	 * return a file descriptor referring to a stream.
	 *
	 * Returns the new file descriptor, or -1 if an error occurred.
	 */
	private int handleOpen(int namePos) {
		String name = readVirtualMemoryString(namePos, maxLength);
		if (name == null || name.length() > maxLength) {
			return -1;
		}
		//no free descriptor
		if (!descriptor.hasFree()) {
			return -1;
		}
		//it is already in removeList
		if (removeList.contains(name)) {
			return -1;
		}
		OpenFile file = UserKernel.fileSystem.open(name, false);
		if (file == null) {
			return -1;
		}
		int newDes = descriptor.getFree();
		descriptor.put(file, newDes);
		fileStore.put(name, file);
		return newDes;
	}
	
	/**
	 * Attempt to read up to count bytes into buffer from the file or stream
	 * referred to by fileDescriptor.
	 *
	 * On success, the number of bytes read is returned. If the file descriptor
	 * refers to a file on disk, the file position is advanced by this number.
	 *
	 * It is not necessarily an error if this number is smaller than the number of
	 * bytes requested. If the file descriptor refers to a file on disk, this
	 * indicates that the end of the file has been reached. If the file descriptor
	 * refers to a stream, this indicates that the fewer bytes are actually
	 * available right now than were requested, but more bytes may become available
	 * in the future. Note that read() never waits for a stream to have more data;
	 * it always returns as much as possible immediately.
	 *
	 * On error, -1 is returned, and the new file position is undefined. This can
	 * happen if fileDescriptor is invalid, if part of the buffer is read-only or
	 * invalid, or if a network stream has been terminated by the remote host and
	 * no more data is available.
	 */
	private int handleRead(int fd, int addr, int size) {
		OpenFile file = descriptor.get(fd);
		// file does not exist
		if (file == null) {
			return -1;
		}
		byte[] tmp = new byte[size];
		int length = file.read(tmp, 0, size);
		int count = this.writeVirtualMemory(addr, tmp, 0, length);
		return count;
	}

	/**
	 * Attempt to write up to count bytes from buffer to the file or stream
	 * referred to by fileDescriptor. write() can return before the bytes are
	 * actually flushed to the file or stream. A write to a stream can block,
	 * however, if kernel queues are temporarily full.
	 *
	 * On success, the number of bytes written is returned (zero indicates nothing
	 * was written), and the file position is advanced by this number. It IS an
	 * error if this number is smaller than the number of bytes requested. For
	 * disk files, this indicates that the disk is full. For streams, this
	 * indicates the stream was terminated by the remote host before all the data
	 * was transferred.
	 *
	 * On error, -1 is returned, and the new file position is undefined. This can
	 * happen if fileDescriptor is invalid, if part of the buffer is invalid, or
	 * if a network stream has already been terminated by the remote host.
	 */
	private int handleWrite(int fd, int addr, int size) {
		OpenFile file = descriptor.get(fd);
		// file does not exist
		if (file == null) {
			return -1;
		}
		byte[] tmp = new byte[size];
		int length = this.readVirtualMemory(addr, tmp, 0, size);
		// It IS an
		// error if this number is smaller than the number of bytes requested
		if (length < size) {
			return -1;
		}
		int count = file.write(tmp, 0, length);
		if (count < length) {
			return -1;
		}
		return count;
	}
	
	/**
	 * Close a file descriptor, so that it no longer refers to any file or stream
	 * and may be reused.
	 *
	 * If the file descriptor refers to a file, all data written to it by write()
	 * will be flushed to disk before close() returns.
	 * If the file descriptor refers to a stream, all data written to it by write()
	 * will eventually be flushed (unless the stream is terminated remotely), but
	 * not necessarily before close() returns.
	 *
	 * The resources associated with the file descriptor are released. If the
	 * descriptor is the last reference to a disk file which has been removed using
	 * unlink, the file is deleted (this detail is handled by the file system
	 * implementation).
	 *
	 * Returns 0 on success, or -1 if an error occurred.
	 */
	private int handleClose(int fd) {
		OpenFile file = descriptor.get(fd);
		if (file == null) {
			return -1;
		}
		String name = file.getName();
		if (name == null) {
			return -1;
		}
		if (fd != 0 && fd != 1) {
			fileStore.remove(file);
			descriptor.remove(new Integer(fd));
		}
		//System.err.println(fileStore.get(file.getName()).length);
		file.close();
		if (removeList.contains(name) && fileStore.get(name).length == 0) {
			if (!UserKernel.fileSystem.remove(name)) {
				return -1;
			}
			removeList.remove(name);
		} 
		return 0;
	}
	
	/**
	 * Delete a file from the file system. If no processes have the file open, the
	 * file is deleted immediately and the space it was using is made available for
	 * reuse.
	 *
	 * If any processes still have the file open, the file will remain in existence
	 * until the last file descriptor referring to it is closed. However, creat()
	 * and open() will not be able to return new file descriptors for the file
	 * until it is deleted.
	 *
	 * Returns 0 on success, or -1 if an error occurred.
	 */
	private int handleUnlink(int namePos) {
		String name = readVirtualMemoryString(namePos, maxLength);
		if (name == null || name.length() > maxLength) {
			return -1;
		}
		if (fileStore.get(name).length == 0) {
			if (!UserKernel.fileSystem.remove(name)) {
				return -1;
			}
		} else {
			removeList.add(name);
		}
		return 0;
	}
	
	/**
	 * Terminate the current process immediately. Any open file descriptors
	 * belonging to the process are closed. Any children of the process no longer
	 * have a parent process.
	 *
	 * status is returned to the parent process as this process's exit status and
	 * can be collected using the join syscall. A process exiting normally should
	 * (but is not required to) set status to 0.
	 *
	 * exit() never returns.
	 */
	private int handleExit(int status) {
		this.status = status;
		
	 	//Any open file descriptors belonging to the process are close
		int[] list = descriptor.getAll();
		for (int i = 0; i < list.length; ++i) {
			handleClose(list[i]);
		}
		
		// need to free all pages
		unloadSections();
		
		processTable.remove(new Integer(this.processID));
		/*
		System.err.println(this.processID + ": ");
		for (Iterator it = processTable.keySet().iterator(); it.hasNext(); ) {
			System.err.println((Integer)it.next());		
		}
		System.err.println("==============");*/
		if (processTable.size() == 0) {
			Kernel.kernel.terminate();
		}
		UThread.finish();
		
		return 0;
	}
	
	/**
	 * Execute the program stored in the specified file, with the specified
	 * arguments, in a new child process. The child process has a new unique
	 * process ID, and starts with stdin opened as file descriptor 0, and stdout
	 * opened as file descriptor 1.
	 *
	 * file is a null-terminated string that specifies the name of the file
	 * containing the executable. Note that this string must include the ".coff"
	 * extension.
	 *
	 * argc specifies the number of arguments to pass to the child process. This
	 * number must be non-negative.
	 *
	 * argv is an array of pointers to null-terminated strings that represent the
	 * arguments to pass to the child process. argv[0] points to the first
	 * argument, and argv[argc-1] points to the last argument.
	 *
	 * exec() returns the child process's process ID, which can be passed to
	 * join(). On error, returns -1.
	 */
	private int handleExec(int filePos, int argc, int argvPos) {
		String name = this.readVirtualMemoryString(filePos, maxLength);
		if (name == null || name.length() > maxLength) {
			return -1;
		}
		if (!name.endsWith(".coff")) {
			return -1;
		}
		if (argc < 0) {
			return -1;
		}
		
		final int length = argc * 4;
		byte[] tmp = new byte[length];
		if (readVirtualMemory(argvPos, tmp, 0, length) < length) {
			return -1;
		}
		String[] args = new String[argc];
		for (int i = 0; i < argc; ++i) {
			int pos = Lib.bytesToInt(tmp, i * 4, 4);
			args[i] = readVirtualMemoryString(pos, maxLength);
			if (args[i] == null) {
				return -1;
			}
		}
		
		UserProcess child = new UserProcess();	
		childProcess.add(new Integer(child.processID));

		if (!child.execute(name, args)) {//exit on error, such as file does not existed
			childProcess.remove(new Integer(child.processID));
			processTable.remove(new Integer(child.processID));
			return -1;
		}

		return child.processID;
	}
	
	/**
	 * Suspend execution of the current process until the child process specified
	 * by the processID argument has exited. If the child has already exited by the
	 * time of the call, returns immediately. When the current process resumes, it
	 * disowns the child process, so that join() cannot be used on that process
	 * again.
	 *
	 * processID is the process ID of the child process, returned by exec().
	 *
	 * status points to an integer where the exit status of the child process will
	 * be stored. This is the value the child passed to exit(). If the child exited
	 * because of an unhandled exception, the value stored is not defined.
	 *
	 * If the child exited normally, returns 1. If the child exited as a result of
	 * an unhandled exception, returns 0. If processID does not refer to a child
	 * process of the current process, returns -1.
	 */
	private int handleJoin(int processID, int statusPos) {
		if (!childProcess.contains(new Integer(processID))) {
			return -1;
		}
		UserProcess child = (UserProcess)processTable.get(new Integer(processID));
		if (child == null) {
			//If the child exited as a result of an unhandled exception
			processTable.remove(new Integer(processID));
			return 0;
		}
		child.thread.join();
		
		byte[] content = Lib.bytesFromInt(child.status);
		this.writeVirtualMemory(statusPos, content);
		return 1;//exit normally
	}
	
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		//System.err.println(this.processID + " " + syscall);
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		//Phase 2 Task 1: 
		case syscallCreate:
			return handleCreate(a0);
		case syscallOpen:
			return handleOpen(a0);
		case syscallRead:
			return handleRead(a0, a1, a2);
		case syscallWrite:
			return handleWrite(a0, a1, a2);
		case syscallClose:
			return handleClose(a0);
		case syscallUnlink:
			return handleUnlink(a0);
		//Phase 2 Task 3:
		case syscallExit:
			return handleExit(a0);
		case syscallExec:
			return handleExec(a0, a1, a2);
		case syscallJoin:
			return handleJoin(a0, a1);
			

		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
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
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0), processor
							.readRegister(Processor.regA1), processor
							.readRegister(Processor.regA2), processor
							.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
		}
	}

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;
	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = Config.getInteger("Processor.numStackPages", 8);

	private int initialPC, initialSP;
	private int argc, argv;

	private static final int pageSize = Processor.pageSize;
	private static final char dbgProcess = 'a';
	
	
	//Phase 2 Task 3
	private HashSet childProcess = new HashSet();
	private int processID;
	private int status;
	
	private KThread thread;
	
	private static int processCount = 0;
	private static HashMap processTable = new HashMap(); 
	
	//Phase 2 Task 1
	
	
	private static int maxLength = 256;
	private static int maxDescriptorCount = 256;
	
	private HashSet removeList = new HashSet();
	
	private Descriptor descriptor = new Descriptor();
	private static FileStore fileStore = new FileStore();
	
	// associated name with descriptor
	private class Descriptor {
		Descriptor() {
			for (int i = 0; i < maxDescriptorCount; ++i) {
				free.add(new Integer(i));
			}
		}
		
		boolean hasFree() {
			return !free.isEmpty();
		}
		
		int getFree() {
			return (Integer)free.iterator().next();
		}
		
		// return descriptor
		int get(OpenFile file) {
			return (Integer)fileTable.get(file);
		}
		
		// return all descriptors
		int[] getAll() {
			ArrayList<Integer> list = new ArrayList<Integer>();
			for (Iterator it = descriptorTable.keySet().iterator(); it.hasNext(); ) {
				list.add((Integer)it.next());
			}
			int[] result = new int[list.size()];
			for (int i = 0; i < result.length; ++i) {
				result[i] = list.get(i).intValue();
			}
			return result;
		}
		
		// return file
		OpenFile get(int descriptor) {
			return (OpenFile)descriptorTable.get(descriptor);
		}
		
		void put(OpenFile file, int descriptor) {
			fileTable.put(file, new Integer(descriptor));
			descriptorTable.put(new Integer(descriptor), file);
			free.remove(new Integer(descriptor));
		}
		
		void remove(int descriptor) {
			fileTable.remove(descriptorTable.get(new Integer(descriptor)));
			descriptorTable.remove(new Integer(descriptor));
			free.add(new Integer(descriptor));
		}
		
		HashSet free = new HashSet();
		HashMap fileTable = new HashMap();	// file mapped to descriptor
		HashMap descriptorTable = new HashMap(); 	// descriptor mapped to file
	}
	
	// associated name with real file on disk through OpenFile
	private static class FileStore {
		FileStore() {
		}
		
		// provided for outside uses
		OpenFile[] get(String name) {
			ArrayList list = (ArrayList)fileTable.get(name);
			if (list == null) {
				return new OpenFile[0];
			}
			OpenFile[] tmp = new OpenFile[list.size()];
			int count = 0;
			for (Iterator it = list.iterator(); it.hasNext(); ) {
				tmp[count++] = (OpenFile)it.next();
			}
			return tmp;
		}
		
		void put(String name, OpenFile file) {
			ArrayList list = (ArrayList)fileTable.get(name);
			if (list == null) {
				list = new ArrayList();
				list.add(file);
				fileTable.put(name, list);
			} else {
				list.add(file);
			}
		}
		
		void remove(OpenFile file) {
			String name = file.getName();
			ArrayList list = (ArrayList)fileTable.get(name);
			list.remove(file);
			if (list != null) {
				list.remove(name);
			}
		}
		
		HashMap fileTable = new HashMap(); // name mapped to a list of file 
	}
	
}
