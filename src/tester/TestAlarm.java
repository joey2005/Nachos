package tester;

import java.util.*;
import nachos.machine.*;
import nachos.threads.*;

public class TestAlarm {
	private static class T implements Runnable {
		String name;
		long wakeTime;
		Alarm alarm;
		
		T(String name, long wakeTime, Alarm alarm) {
			this.name = name;
			this.wakeTime = wakeTime;
			this.alarm = alarm;
		}
		
		public void run() {
			for (int i = 0; i < 10; ++i) {
				System.out.println("Thread " + name + " looped at " + i + " times at time " + Machine.timer().getTime());
				alarm.waitUntil(wakeTime);
			}
		}
	}
	
	public static void selfTest() {
		System.out.println("This is tester for alarm!");
		Alarm alarm = new Alarm();
		KThread A = new KThread(new T("A", 250, alarm)).setName("A");
		KThread B = new KThread(new T("B", 750, alarm)).setName("B");
		A.fork();
		B.fork();
		//A.join();
		//B.join();

	}
}


