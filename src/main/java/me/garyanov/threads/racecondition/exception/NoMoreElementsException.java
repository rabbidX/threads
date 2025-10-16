package main.java.me.garyanov.threads.racecondition.exception;

public class NoMoreElementsException extends RuntimeException{
    public NoMoreElementsException() {
        super("No more elements");
    }
}
