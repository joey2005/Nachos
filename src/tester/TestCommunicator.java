
package tester;

import nachos.machine.*;
import nachos.threads.*;

public class TestCommunicator {
	private static class T implements Runnable {
		String name;
		int type;
		Communicator pipe;
		
		public T(String name, int type, Communicator pipe) {
			this.name = name;
			this.type = type;
			this.pipe = pipe;
		}
		
		public void run() {
			if (type == 0) {
				for (int i = 0; i < 12; ++i) {
					pipe.speak(i);
					System.out.println("Thread " + name + " speaks " + i + " at time " + Machine.timer().getTime());
				}
			} else {
				for (int i = 0; i < 4; ++i) {
					System.out.println("Thread " + name + " receives " + pipe.listen() + " at time " + Machine.timer().getTime());
				}
			}
		}
	}
	
	public static void selfTest() {
		Communicator pipe = new Communicator();
		KThread[] t = new KThread[4];
		
		t[0] = new KThread(new T("Speaker", 0, pipe));
		t[1] = new KThread(new T("Listener1", 1, pipe));
		t[2] = new KThread(new T("Listener2", 1, pipe));
		t[3] = new KThread(new T("Listener3", 1, pipe));
		
		System.out.println("This is test for communicator:");
		for (int i = 0; i < 4; ++i) {
			t[i].fork();
		}

	}
}


