package com.benjiweber.yield;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;

import static com.benjiweber.yield.Completed.completed;
import static com.benjiweber.yield.Exceptions.unchecked;
import static com.benjiweber.yield.FlowControl.youMayProceed;
import static com.benjiweber.yield.IfAbsent.ifAbsent;
import static com.benjiweber.yield.Message.message;

public interface Yielderable<T> extends Iterable<T> {

    void execute(YieldDefinition<T> builder);

    default Iterator<T> iterator() {
        YieldDefinition<T> yieldDefinition = new YieldDefinition<>();
        Thread collectorThread = new Thread(() -> {
            yieldDefinition.waitUntilFirstValueRequested();
            try {
                execute(yieldDefinition);
            } catch (BreakException e) {
            }
            yieldDefinition.signalComplete();
        });
        collectorThread.setDaemon(true);
        collectorThread.start();
        yieldDefinition.onClose(collectorThread::interrupt);
        return yieldDefinition.iterator();
    }
}

class YieldDefinition<T> implements Iterable<T>, Iterator<T>, AutoCloseable {
    private final SynchronousQueue<Message<T>> dataChannel = new SynchronousQueue<>();
    private final SynchronousQueue<FlowControl> flowChannel = new SynchronousQueue<>();
    private final AtomicReference<Optional<T>> currentValue = new AtomicReference<>(Optional.empty());
    private List<Runnable> toRunOnClose = new CopyOnWriteArrayList<>();

    public void returning(T value) {
        publish(value);
        waitUntilNextValueRequested();
    }

    public void breaking() {
        throw new BreakException();
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        calculateNextValue();
        Message<T> message = unchecked(() -> dataChannel.take());
        if (message instanceof Completed) return false;
        currentValue.set(message.value());
        return true;
    }

    @Override
    public T next() {
        try {
            ifAbsent(currentValue.get()).then(this::hasNext);
            return currentValue.get().get();
        } finally {
            currentValue.set(Optional.empty());
        }
    }

    public void signalComplete() {
        unchecked(() -> this.dataChannel.put(completed()));
    }

    public void waitUntilFirstValueRequested() {
        waitUntilNextValueRequested();
    }

    private void waitUntilNextValueRequested() {
        unchecked(() -> flowChannel.take());
    }

    private void publish(T value) {
        unchecked(() -> dataChannel.put(message(value)));
    }

    private void calculateNextValue() {
        unchecked(() -> flowChannel.put(youMayProceed));
    }

    @Override
    public void close() throws Exception {
        toRunOnClose.forEach(Runnable::run);
    }

    public void onClose(Runnable onClose) {
        this.toRunOnClose.add(onClose);
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}

interface Message<T> {
    Optional<T> value();
    static <T> Message<T> message(T value) {
        return () -> Optional.of(value);
    }
}

interface Completed<T> extends Message<T> {
    static <T> Completed<T> completed() { return () -> Optional.empty(); }
}

interface FlowControl {
    static FlowControl youMayProceed = new FlowControl() {};
}

class BreakException extends RuntimeException {
    public synchronized Throwable fillInStackTrace() {
        return null;
    }
}

interface Then<T> {
    void then(Runnable r);
}
class IfAbsent {
    public static <T> Then<T> ifAbsent(Optional<T> optional) {
        return runnable -> {
            if (!optional.isPresent()) runnable.run();
        };
    }
}


