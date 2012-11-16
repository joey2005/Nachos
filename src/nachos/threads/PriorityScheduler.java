package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

import java.util.*;

/**
 * A scheduler that chooses threads based on their priorities.
 * 
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 * 
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 * 
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 * 
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer priority from
	 *            waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority()
	{
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
		{
		  Machine.interrupt().restore(intStatus); // bug identified by Xiao Jia @ 2011-11-04
			return false;
		}

		setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority()
	{
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
		{
		  Machine.interrupt().restore(intStatus); // bug identified by Xiao Jia @ 2011-11-04
			return false;
		}

		setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 * 
	 * @param thread
	 *            the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			
			ThreadState threadState;
			if (owner != null) {
				owner.waitList.remove(this);
			}
			if ((threadState = pickNextThread()) == null) {
				owner = null;
				return null;
			}
			threadState.acquire(this);
			//assert(threadState == getThreadState(threadState.thread));
			return threadState.thread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			// implement me
			if (waitQueue.isEmpty()) {
				return null;
			}
			ThreadState result = null;
			for (Iterator it = waitQueue.iterator(); it.hasNext(); ) {
				ThreadState threadState = getThreadState((KThread)it.next());
				threadState.effectivePriority = -1;
			}
			for (Iterator it = waitQueue.iterator(); it.hasNext(); ) {
				ThreadState threadState = getThreadState((KThread)it.next());
				if (result == null || result.getEffectivePriority() < threadState.getEffectivePriority()) {
					result = threadState;
				}
			}
			//System.out.println(result.effectivePriority + " " + result.priority);
			return result;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
			for (Iterator it = waitQueue.iterator(); it.hasNext(); ) {
				ThreadState threadState = getThreadState((KThread)it.next());
				System.out.print("(" + threadState.thread + ": " + threadState.getPriority() + " " + threadState.getEffectivePriority() + ") ");
			}
			System.out.println("");
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		
		protected ThreadState owner = null;
		protected LinkedList waitQueue = new LinkedList();
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread
		 *            the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;

			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {//System.out.println("here!");
			// implement me
			int donationPart2 = getJoinDonation(); 
			if (effectivePriority != -1) {
				return Math.max(donationPart2, effectivePriority);
			}
			effectivePriority = priority;
			for (PriorityQueue waitQueue : waitList) {
				if (!waitQueue.transferPriority) {
					continue;
				}
				for (Iterator it = waitQueue.waitQueue.iterator(); it.hasNext(); ) {
					ThreadState threadState = getThreadState((KThread)it.next());
					if (threadState.getEffectivePriority() > effectivePriority) {
						effectivePriority = threadState.getEffectivePriority();
					}
				}
			}

			return Math.max(effectivePriority, donationPart2);
		}
		
		protected int getJoinDonation() {
			int result = -1;
			for (KThread joinThread : thread.joinList) {
				ThreadState threadState = getThreadState(joinThread);
				if (threadState.getEffectivePriority() > result) {
					result = threadState.getEffectivePriority();
				}
			}			
			return result;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 * 
		 * @param priority
		 *            the new priority.
		 */
		protected void modifyPath() {
			if (belong == null) {
				return;
			}
			for (PriorityQueue current = belong; current.owner != null; current = current.owner.belong) {
				if (!current.transferPriority) {
					break;
				}
				current.owner.effectivePriority = -1;
				if (current.owner.belong == null) {
					break;
				}
			}
		}
		
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;

			// implement me
			//effectivePriority = -1;
			//modifyPath();
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 * 
		 * @param waitQueue
		 *            the queue that the associated thread is now waiting on.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {//System.out.println("!");
			// implement me
			boolean intStatus = Machine.interrupt().disable();
			waitQueue.waitQueue.add(this.thread);
			belong = waitQueue;
			//modifyPath();
			Machine.interrupt().setStatus(intStatus);
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			// implement me
			boolean intStatus = Machine.interrupt().disable();
			waitQueue.waitQueue.remove(this.thread);
			/*
			if (waitQueue.transferPriority) {
				
				if (waitQueue.owner != null) {
					waitQueue.owner.modifyPath();
				}
			
			}
			*/
			waitQueue.owner = this;
			//waitQueue.owner.effectivePriority = -1;
			waitQueue.owner.waitList.add(waitQueue);
			waitQueue.owner.belong = null;
			Machine.interrupt().setStatus(intStatus);
		}

		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		
		protected int effectivePriority = -1;
		
		protected LinkedList<PriorityQueue> waitList = new LinkedList<PriorityQueue>();
		protected PriorityQueue belong = null;
	}
}

