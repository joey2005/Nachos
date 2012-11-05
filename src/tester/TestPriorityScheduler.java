package tester;

import nachos.machine.*;
import nachos.threads.*;

public class TestPriorityScheduler {

    public static class T implements Runnable {
        String name;
        int priority;

        public T(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        public void run() {
            for (int i = 0; i < 10; i++) {
                //ThreadedKernel.alarm.waitUntil(500);
                System.out.println("Thread " + name + " with Priority " + priority + " looped " + i + " times at time " + Machine.timer().getTime());
                KThread.yield();
            }
        }
    }

    public static class Tester implements Runnable {

        public void run() {
            KThread[] t = new KThread[4];
            String[] names = {"A", "B", "C", "D"};

            for (int i = 0; i < 4; ++i) {
                t[i] = new KThread(new T(names[i], i + 1));
                boolean intStatus = Machine.interrupt().disable();
                ThreadedKernel.scheduler.setPriority(t[i], i + 1);
                Machine.interrupt().restore(intStatus);
            }

            for (int i = 0; i < 4; ++i) {
                t[i].fork();
            }

            for (int i = 0; i < 4; ++i) {
                t[i].join();
            }

        }
    }

    public static void selfTest() {
        System.out.println("This is a test for PriorityScheduler");
        KThread TS = new KThread(new Tester());
        TS.fork();
        TS.join();

    }

}


