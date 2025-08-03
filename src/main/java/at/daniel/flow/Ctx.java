package at.daniel.flow;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public interface Ctx<P extends JavaPlugin> {

    /**
     * Gets the player interacting with this context.
     *
     * @return the player
     */
    @NotNull Player player();

    /**
     * Gets the plugin associated with this context.
     *
     * @return the plugin instance
     */
    @NotNull P plugin();

    /**
     * Executes a task synchronously on the main thread and returns the result.
     *
     * @param task the task to be executed synchronously
     * @param <T> the type of the result
     * @return the result of the task
     */
    @NotNull <T> T sync(@NotNull Factory<T> task);

    /**
     * Runs a task on the main thread without blocking, returning a future for the result.
     *
     * @param task the task to run
     * @param <T> the return type
     */
    @NotNull <T> CompletableFuture<T> nonBlocking(@NotNull Factory<T> task);

    /**
     * Runs a task on the main thread without blocking the current thread.
     *
     * @param task the task to run
     */
    void nonBlocking(@NotNull Runnable task);

    /**
     * Executes a task synchronously on the main thread without returning any result.
     *
     * @param task the task to be executed synchronously
     */
    void sync(@NotNull Runnable task);

    /**
     * Subscribes to a specific event with a filter to specify the conditions.
     *
     * @param event the event type to subscribe to
     * @param filter the filter to determine if the event should be handled
     * @param <E> the event type
     */
    default <E extends Event> EventHandle<E> subscribe(@NotNull Class<E> event, @NotNull Filter<E> filter) {
        return subscribe(event, filter, false);
    }

    /**
     * Subscribes to a specific event with a filter to specify the conditions.
     *
     * @param event the event type to subscribe to
     * @param filter the filter to determine if the event should be handled
     * @param <E> the event type
     */
    <E extends Event> EventHandle<E> subscribe(@NotNull Class<E> event, @NotNull Filter<E> filter, boolean cancel);

    /**
     * Subscribes to a specific event without any filtering conditions.
     *
     * @param event the event type to subscribe to
     * @param <E> the event type
     */
    default <E extends Event> EventHandle<E> subscribe(@NotNull Class<E> event) {
        return subscribe(event, Filter.allow());
    }

    /**
     * Subscribes to a specific event without any filtering conditions.
     *
     * @param event the event type to subscribe to
     * @param <E> the event type
     */
    default <E extends Event> EventHandle<E> subscribe(@NotNull Class<E> event, boolean cancel) {
        return subscribe(event, Filter.allow(), cancel);
    }

    /**
     * Launches another flow for a different player.
     *
     * @param player the target player
     * @param flow the flow to run
     * @param <R> the return type
     */
    <R> CompletableFuture<R> launchForPlayer(Player player, Flow<P, R> flow);

    /**
     * Factory interface for creating tasks to be run synchronously.
     *
     * @param <T> the result type of the task
     */
    interface Factory<T> {
        T run();
    }
}
