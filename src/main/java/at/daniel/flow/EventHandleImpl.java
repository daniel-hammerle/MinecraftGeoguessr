package at.daniel.flow;

import org.bukkit.Bukkit;
import org.bukkit.event.*;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

public class EventHandleImpl<E extends Event> implements EventHandle<E>, Listener, EventHandleFuture<E> {

    private final Queue<E> events = new LinkedList<>();
    private final Ctx<?> ctx;
    private final boolean cancel;
    private boolean notified = false;
    private boolean paused = false;
    private final Queue<AsyncRequest> asyncRequests = new LinkedList<>();

    class AsyncRequest {
        private final State state;
        private final CompletableFuture<E> completion;

        AsyncRequest(State state, CompletableFuture<E> completion) {
            this.state = state;
            this.completion = completion;
        }

        public CompletableFuture<E> getCompletion() {
            return completion;
        }

        public State getState() {
            return state;
        }
    }

    public EventHandleImpl(Class<E> clazz, Ctx<?> ctx, boolean cancel) {
        this.ctx = ctx;
        this.cancel = cancel;

        ctx.nonBlocking(() ->
                Bukkit.getPluginManager().registerEvent(clazz, this, EventPriority.NORMAL, this::handleEvent, ctx.plugin())
        );
    }


    private void handleEvent(Listener ignoreListener, Event event) {
        if (cancel) {
            if (event instanceof Cancellable cancellable) cancellable.setCancelled(true);
        }

        synchronized (this) {
            if (paused) {
                return;
            }

            while(!asyncRequests.isEmpty()) {
                AsyncRequest request = asyncRequests.remove();
                //noinspection unchecked
                if (request.getState().doIfActive(() -> request.getCompletion().complete((E) event))) {
                    return;
                }
            }

            //noinspection unchecked
            events.add((E) event);
            notified = true;
            this.notifyAll();

        }
    }

    @Override
    public E awaitEvent() {
        synchronized (this) {
            if (!events.isEmpty()) {
                return events.remove();
            }
        }
        try {
            synchronized (this) {
                this.wait();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        synchronized (this) {
            notified = false;
            return events.remove();
        }
    }

    @Override
    public E awaitEvent(Duration duration) throws TimeoutException {
        synchronized (this) {
            if (!events.isEmpty()) {
                return events.remove();
            }
        }
        try {
            synchronized (this) {
                this.wait(duration.toMillis());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        synchronized (this) {
            if (!notified) {
                throw new TimeoutException("No event happened in the provided time frame!");
            }
            notified = false;
            return events.remove();
        }
    }

    @Override
    public void pause() {
        synchronized (this) {
            paused = true;
        }
    }

    @Override
    public void resume() {
        synchronized (this) {
            paused = false;
        }
    }

    @Override
    public void close() {
        ctx.nonBlocking(() -> HandlerList.unregisterAll(this));
    }

    @Override
    public CompletableFuture<E> waitForEvent(State state) {
        CompletableFuture<E> future = new CompletableFuture<>();
        synchronized (this) {
            if (!events.isEmpty()) {
                state.doIfActive(() ->  future.complete(events.remove()));
                return future;
            }
        }

        synchronized (this) {
            asyncRequests.add(new AsyncRequest(state, future));
        }
        return future;
    }
}
