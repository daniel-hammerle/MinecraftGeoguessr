package at.daniel.flow;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CtxImplementation<P extends JavaPlugin> implements Ctx<P> {

    private final P plugin;
    private final Player player;
    private AtomicBoolean isKilled = new AtomicBoolean(false);

    public CtxImplementation(P plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    private void checkKilled() {
        if (isKilled.get()) {
            throw new DeathException();
        }
    }

    @Override
    public @NotNull Player player() {
        return player;
    }

    @Override
    public @NotNull P plugin() {
        return plugin;
    }

    @Override
    public <T> @NotNull T sync(@NotNull Factory<T> task) {
        checkKilled();
        if (Bukkit.isPrimaryThread()) {
            return task.run();
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> future.complete(task.run()));

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull <T> CompletableFuture<T> nonBlocking(@NotNull Factory<T> task) {
        checkKilled();
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> future.complete(task.run()));
        return future;
    }

    @Override
    public void nonBlocking(@NotNull Runnable task) {
        checkKilled();
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void sync(@NotNull Runnable task) {
        checkKilled();
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            task.run();
            future.complete(null);
        });

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <E extends Event> EventHandle<E> subscribe(@NotNull Class<E> event, @NotNull Filter<E> filter, boolean cancel) {
        checkKilled();
        return new EventHandleImpl<>(event, this, cancel, isKilled);
    }

    @Override
    public <R> CompletableFuture<R> launchForPlayer(Player player, Flow<P, R> flow) {
        checkKilled();
        var newCtx = new CtxImplementation<>(plugin, player);
        return CompletableFuture.supplyAsync(() -> flow.run(newCtx));
    }

    public void setKilled(boolean killed) {
        isKilled.set(killed);
    }
}
