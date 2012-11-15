package nachos.threads;

import java.util.Iterator;
import java.util.LinkedList;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.PriorityScheduler.PriorityQueue;
import nachos.threads.PriorityScheduler.ThreadState;

/**
 * A scheduler that chooses threads using a lottery.
 * 
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 * 
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 * 
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking the
 * maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
	/**
	 * Allocate a new lottery scheduler.
	 */
	//Phase 2 Task 4
	public LotteryScheduler() {
	}
	
	/**
	 * Allocate a new lottery thread queue.
	 * 
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer tickets from
	 *            waiting threads to the owning thread.
	 * @return a new lottery thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		// implement me
		return new PriorityQueue(transferPriority);
	}
	
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}
	
	protected class PriorityQueue extends PriorityScheduler.PriorityQueue {
		PriorityQueue(boolean transferPriority) {
			super(transferPriority);
		}
		
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}
	}
	
	protected class ThreadState extends PriorityScheduler.ThreadState {
		ThreadState(KThread thread) {
			super(thread);
		}
		
		public int getEffectivePriority() {
			int donationPart2 = getJoinDonation(); 
			if (effectivePriority != -1) {
				return donationPart2 + effectivePriority;
			}
			effectivePriority = priority;
			for (PriorityQueue waitQueue : waitList) {
				if (!waitQueue.transferPriority) {
					continue;
				}
				for (Iterator it = waitQueue.waitQueue.iterator(); it.hasNext(); ) {
					ThreadState threadState = getThreadState((KThread)it.next());
					
					effectivePriority += threadState.getEffectivePriority();
				}
			}

			return effectivePriority + donationPart2;
		}
		
		protected LinkedList<PriorityQueue> waitList = new LinkedList<PriorityQueue>();
	}
}
