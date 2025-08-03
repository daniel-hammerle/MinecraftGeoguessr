package at.daniel.flow;

import org.bukkit.event.Event;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * A handle to a subscribed Bukkit event, scoped to a specific flow context.
 * Automatically unsubscribes when closed.
 *
 * @param <E> the event type
 */
public interface EventHandle<E extends Event> extends AutoCloseable {

    /**
     * Blocks until the next matching event occurs.
     *
     * @return the received event
     */
    E awaitEvent();

    /**
     * Blocks until a matching event occurs or the timeout is reached.
     *
     * @param duration the maximum wait time
     * @return the received event
     * @throws TimeoutException if the wait exceeds the duration
     */
    E awaitEvent(Duration duration) throws TimeoutException;

    /**
     * Temporarily disables the handler, discarding any matching events.
     * Useful to avoid processing stale or out-of-scope events.
     */
    void pause();

    /**
     * Resumes handling of matching events after being paused.
     */
    void resume();

    /**
     * Unsubscribes the handler and cleans up any associated listeners.
     */
    @Override
    void close();

    /**
     * Joins multiple event handles into a single one. The joined handle awaits
     * events from any of the underlying handles.
     *
     * @param handles the handles to join
     * @return a composite handle that listens to all
     */
    @SafeVarargs
    static <R extends Event> EventHandle<R> join(EventHandle<? extends R>... handles) {
        return new JointEventHandle<>(handles);
    }

    /**
     * Wraps an existing handle in one that can be re-used after closure.
     *
     * @param handle the handle to preserve
     * @return a preserving wrapper
     */
    static <E extends Event> EventHandle<E> preserve(EventHandle<E> handle) {
        return new PreservingEventHandle<>(handle);
    }
}
