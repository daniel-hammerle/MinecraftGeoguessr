package at.daniel.flow;

import org.bukkit.event.Event;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

class PreservingEventHandle<E extends Event> implements EventHandle<E>, EventHandleFuture<E>{

    private final EventHandle<E> handle;

    public PreservingEventHandle(EventHandle<E> handle) {
        this.handle = handle;
    }

    @Override
    public E awaitEvent() {
        return handle.awaitEvent();
    }

    @Override
    public E awaitEvent(Duration duration) throws TimeoutException {
        return handle.awaitEvent(duration);
    }

    @Override
    public void pause() {
        handle.pause();
    }

    @Override
    public void resume() {
        handle.resume();

    }

    @Override
    public void close() {

    }

    @Override
    public CompletableFuture<E> waitForEvent(State state) {
        return ((EventHandleFuture<E>) handle).waitForEvent(state);
    }
}
