package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;
import java.util.LinkedList;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		lock = new Lock();
		speakList = new LinkedList<S>();
		listenList = new LinkedList<L>();
		
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word
	 *            the integer to transfer.
	 */
	public void speak(int word) {
		lock.acquire();
		if (listenList.isEmpty()) {
			Condition c = new Condition(lock);
			speakList.add(new S(c, word));
			c.sleep();
			
		} else {
			L who = listenList.getFirst();
			who.result = word;
			listenList.removeFirst();
			who.condition.wake();
		}
		lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		int result = -1;
		L current;
		
		lock.acquire();
		if (speakList.isEmpty()) {
			Condition c = new Condition(lock);
			listenList.add(current = new L(c));
			c.sleep();
			result = current.result;
		} else {
			S who = speakList.getFirst();
			result = who.word;
			speakList.removeFirst();
			who.condition.wake();
		}
		lock.release();
		return result;
	}
	
	private class S {
		Condition condition;
		int word;
		
		S(Condition condition, int word) {
			this.condition = condition;
			this.word = word;
		}
	}
	
	private class L {
		Condition condition;
		int result;
		
		L(Condition condition) {
			this.condition = condition;
			this.result = -1;
		}
	}
	
	private LinkedList<S> speakList;
	private LinkedList<L> listenList;
	Lock lock;
}

