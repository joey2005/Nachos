package tester;

import nachos.machine.*;
import nachos.threads.*;

public class TestCondition2 {
	
	private static class T implements Runnable {
		String name;
		Condition2 condition;
		Lock commonLock;
		int period;
		
		public T(String name, Condition2 condition, Lock commonLock, int period) {
			this.name = name;
			this.condition = condition;
			this.commonLock = commonLock;
			this.period = period;
		}
		
		public void run() {
			for (int i = 0; i < 10; ++i) {
				commonLock.acquire();
				System.out.println("Thread " + name + " looped at " + i + " times");
				if (i % period == 0) {
					condition.wake();
				}
				condition.sleep();
				commonLock.release();
			}
		}
	}
	
	public static void selfTest() {
		KThread[] t = new KThread[2];
		
		Lock commonLock = new Lock();
		Condition2 condition = new Condition2(commonLock);
		
		
		t[0] = new KThread(new T("A", condition, commonLock, 2));
		t[1] = new KThread(new T("B", condition, commonLock, 5));
		
		for (int i = 0; i < 2; ++i) {
			t[i].fork();
		}

		
	}
}
