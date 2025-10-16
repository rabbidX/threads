package main.java.me.garyanov.threads.racecondition.queue;

public class ConcurrentNaturalNumbersQueue extends NaturalNumbersQueue {

    @Override
    public int getNext() {
        synchronized (this) {
            return super.getNext();
        }
    }
}
