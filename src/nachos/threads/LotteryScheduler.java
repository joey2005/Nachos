package nachos.threads;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

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
	@Override
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}
	
	@Override
	protected LotteryThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new LotteryThreadState(thread);

		return (LotteryThreadState) thread.schedulingState;
	}
	
	protected class LotteryQueue extends PriorityScheduler.PriorityQueue {

		LotteryQueue(boolean transferPriority) {
			super(transferPriority);
		}
		
		@Override
		protected ThreadState pickNextThread() {
			if (waitQueue.isEmpty()) {
				return null;
			}
			LotteryThreadState result = null;
			for (Iterator it = waitQueue.iterator(); it.hasNext(); ) {
				getThreadState((KThread)it.next()).effectivePriority = -1;
			}
			int intervals = 0;
			for (Iterator it = waitQueue.iterator(); it.hasNext(); ) {
				intervals += getThreadState((KThread)it.next()).getEffectivePriority();
			}
			int lucky = new Random().nextInt(intervals);
			int high = 0;
			for (Iterator it = waitQueue.iterator(); it.hasNext(); ) {
				LotteryThreadState cur = getThreadState((KThread)it.next());
				high += cur.getEffectivePriority();
				if (high > lucky) {
					return cur;
				}
			}
			return result;
		}

	}
	
	protected class LotteryThreadState extends PriorityScheduler.ThreadState {

		LotteryThreadState(KThread thread) {
			super(thread);
		}
		
		@Override
		public int getEffectivePriority() {
			int donationPart2 = getJoinDonation(); 
			if (effectivePriority != -1) {
				return donationPart2 + effectivePriority;
			}
			effectivePriority = priority;
			for (LotteryQueue waitQueue : waitList) {
				if (!waitQueue.transferPriority) {
					continue;
				}
				
				for (Iterator it = waitQueue.waitQueue.iterator(); it.hasNext(); ) {
					effectivePriority += getThreadState((KThread)it.next()).getEffectivePriority();
				}
			}

			return effectivePriority + donationPart2;
		}
		
		@Override
		protected int getJoinDonation() {
			int result = 0;
			for (KThread joinThread : thread.joinList) {
				result += getThreadState(joinThread).getEffectivePriority();
			}			
			return result;
		}
		
		protected LinkedList<LotteryQueue> waitList = new LinkedList<LotteryQueue>();
		protected LotteryQueue belong = null;
	}
	
}
