package tester;

import nachos.machine.*;
import nachos.threads.*;

import java.util.*;

public class TestJoin {
	private static class T1 implements Runnable {
		String name;
		
		T1(String name) {
			this.name = name;
		}
		
		public void run() {
			System.out.println("Thread " + name + " is running!");

		}
	}
	
	private static class T2 implements Runnable {
		String name;
		KThread thread;
		
		T2(String name, KThread thread) {
			this.name = name;
			this.thread = thread;
		}
		
		public void run() {
			//System.out.println(thread.toString());
			thread.join();
			System.out.println("Thread " + name + " is running!");
		}
	}
	
	public static void selfTest() {
		System.out.println("This is a tester for Kthread.join:");
		KThread A = new KThread(new T1("A"));
		KThread B = new KThread(new T2("B", A));
		KThread C = new KThread(new T2("C", B));
		
		C.fork();
		B.fork();
		A.fork();
	}
}


