package nachos.threads;

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
		commonLock = new Lock();
		speaker = new Condition(commonLock);
		listener = new Condition(commonLock);
		counter = 0;
		lastWordReceived = true;
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
		commonLock.acquire();
		counter++;
		//System.out.println(counter);
		if (counter > 0) {
			speaker.sleep();
		}
		while (!lastWordReceived) {
			commonLock.release();
			KThread.yield();
			commonLock.acquire();
		}
		lastWordReceived = false;
		message = word;
		listener.wake();
		commonLock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		commonLock.acquire();
		counter--;
		//System.out.println("!" + counter);
		if (counter >= 0) {
			speaker.wake();
		}
		listener.sleep();
		int result = message;
		lastWordReceived = true;
		commonLock.release();
		return result;
	}
	
	private Condition speaker;
	private Condition listener;
	private Lock commonLock;
	private int counter;
	private int message;
	private boolean lastWordReceived;
}

