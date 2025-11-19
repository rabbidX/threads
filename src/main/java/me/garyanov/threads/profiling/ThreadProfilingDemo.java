package main.java.me.garyanov.threads.profiling;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadProfilingDemo {
    private static final int THREAD_COUNT = 10;
    private static final Random random = new Random();

    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final Map<String, Integer> sharedMap = new ConcurrentHashMap<>();
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        ThreadProfilingDemo demo = new ThreadProfilingDemo();
        demo.startDemo();

        // Даем время для сбора данных в VisualVM
        Thread.sleep(90000);

        demo.stopDemo();
        demo.executor.shutdown();
        long startTime = System.currentTimeMillis();
        while (!demo.executor.isTerminated() &&
                (System.currentTimeMillis() - startTime) < 5000) {
            Thread.sleep(100);
        }
        System.out.println("Demo is finished");
    }

    public void startDemo() {
        // Запускаем различные типы задач
        startCPUIntensiveTasks();
        startIOTasks();
        startLockContentionTasks();
        startWaitingTasks();
        startProducerConsumer();
        startDeadlockScenario();
    }

    private void startCPUIntensiveTasks() {
        for (int i = 0; i < THREAD_COUNT / 2; i++) {
            executor.submit(() -> {
                while (running) {
                    // CPU-intensive work
                    double result = 0;
                    for (int j = 0; j < 100000; j++) {
                        result += Math.sin(j) * Math.cos(j);
                    }
                    Thread.yield(); // Даем возможность переключиться другим потокам
                }
            });
        }
    }

    private void startIOTasks() {
        for (int i = 0; i < THREAD_COUNT / 4; i++) {
            executor.submit(() -> {
                while (running) {
                    try {
                        // Имитация IO-операции
                        Thread.sleep(100 + random.nextInt(200));
                        // Короткая CPU-работа
                        sharedMap.put("key_" + Thread.currentThread().getName(), random.nextInt(1000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
    }

    private void startLockContentionTasks() {
        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                while (running) {
                    lock.lock();
                    try {
                        // Работа под lock
                        int value = random.nextInt(100);
                        Thread.sleep(50); // Увеличиваем время удержания lock для создания contention
                        sharedMap.put("contention_key", value);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        lock.unlock();
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // зачем это надо?
                    }
                }
            });
        }
    }

    private void startWaitingTasks() {
        executor.submit(() -> {
            lock.lock();
            try {
                long startWait = System.currentTimeMillis();
                while (System.currentTimeMillis() - startWait < 1000 && running) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            } finally {
                lock.unlock();
            }
        });
    }

    private void startProducerConsumer() {
        // Producer
        executor.submit(() -> {
            int count = 0;
            while (running && count < 1000) {
                try {
                    queue.put("Message-" + count++);
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        // Consumer
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                while (running) {
                    try {
                        var start = System.currentTimeMillis();
                        String message = null;
                        while (System.currentTimeMillis() - start > 100 && message == null) {
                            message = queue.poll();
                            Thread.sleep(10);
                        }
                        if (message != null) {
                            // Обработка сообщения
                            Thread.sleep(20);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
    }

    private void startDeadlockScenario() {
        Object lock1 = new Object();
        Object lock2 = new Object();

        // Поток 1
        executor.submit(() -> {
            while (running) {
                synchronized (lock1) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    synchronized (lock2) {
                        // Критическая секция
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // Поток 2 (потенциальный deadlock)
        executor.submit(() -> {
            while (running) {
                synchronized (lock2) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    synchronized (lock1) {
                        // Критическая секция
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void stopDemo() {
        running = false;
        lock.lock();
        try {
            condition.signalAll(); // Будим все ожидающие потоки
        } finally {
            lock.unlock();
        }
    }
}