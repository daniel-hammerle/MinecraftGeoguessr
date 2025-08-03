package at.daniel.flow;

import org.bukkit.event.Event;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JointEventHandle<E extends Event> implements EventHandle<E>, EventHandleFuture<E> {
    private final EventHandle<? extends E>[] eventHandles;

    public JointEventHandle(EventHandle<? extends E>[] eventHandles) {
        this.eventHandles = eventHandles;
    }

    @Override
    public E awaitEvent() {
        State state = new State();
        var eventFutures = Arrays.stream(eventHandles)
                .map(it -> ((EventHandleFuture<E>) it).waitForEvent(state))
                .toArray(CompletableFuture[]::new);
        var result = CompletableFuture.anyOf(eventFutures);
        try {
            //noinspection unchecked
            return (E) result.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public E awaitEvent(Duration duration) throws TimeoutException {
        State state = new State();
        var eventFutures = Arrays.stream(eventHandles)
                .map(it -> ((EventHandleFuture<E>) it).waitForEvent(state))
                .toArray(CompletableFuture[]::new);
        var result = CompletableFuture.anyOf(eventFutures);
        try {
            //noinspection unchecked
            return (E) result.get(duration.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void pause() {
        Arrays.stream(eventHandles).forEach(EventHandle::pause);
    }

    @Override
    public void resume() {
        Arrays.stream(eventHandles).forEach(EventHandle::resume);
    }

    @Override
    public void close() {
        Arrays.stream(eventHandles).forEach(EventHandle::close);
    }



    @Override
    public CompletableFuture<E> waitForEvent(EventHandleFuture.State state) {
        var events =  Arrays.stream(eventHandles)
                .map(it -> ((EventHandleFuture<E>) it).waitForEvent(state))
                .toArray(CompletableFuture[]::new);
		//      \/--for some reason a normal cast to CompletableFuture<E> fails but using this function call it works🤦
		return typeMagic(CompletableFuture.anyOf(events));
    }

    private static<A, B> A typeMagic(B value) {
        return (A) value;
    }
}
