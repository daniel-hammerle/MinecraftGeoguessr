package at.daniel.flow;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a unit of flow logic — a coroutine-style interaction that can run in a player context.
 *
 * @param <P> the plugin type
 * @param <R> the return value type
 */
public interface Flow<P extends JavaPlugin, R> {

    /**
     * Executes the flow in the given player context.
     *
     * @param ctx the context
     * @return the result
     */
    R run(Ctx<P> ctx);

    /**
     * A flow entrypoint with typed input parameters.
     *
     * @param <P> the plugin type
     * @param <R> the result type
     * @param <Props> the input props
     */
    interface Entry<P extends JavaPlugin, R, Props> {
        R run(Ctx<P> ctx, Props props);
    }

    /**
     * A wrapper that allows launching a Flow with parameters for a specific player.
     *
     * @param <P> the plugin type
     * @param <R> the result type
     * @param <Props> the input props
     */
    class Application<P extends JavaPlugin, R, Props> {
        private final Flow.Entry<P, R, Props> application;
        private final P plugin;
        /**
         * Creates a new application.
         *
         * @param application the flow entry
         * @param plugin the plugin instance
         */
        public Application(Flow.Entry<P, R, Props> application, P plugin) {
            this.application = application;
            this.plugin = plugin;
        }

        /**
         * Launches the flow asynchronously for a given player and input props.
         *
         * @param player the player
         * @param props the input data
         * @return a future that completes with the result
         */
        public CompletableFuture<R> forPlayer(Player player, Props props) {
            Ctx<P> context = new CtxImplementation<>(plugin, player);

            CompletableFuture<R> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                future.complete(application.run(context, props));
            });

            return future;
        }
    }

    /**
     * Creates a new Flow application for launching.
     *
     * @param plugin the plugin instance
     * @param app the flow logic
     * @return an application wrapper
     */
    static <P extends JavaPlugin, R, Props> Application<P, R, Props> create(P plugin, Flow.Entry<P, R, Props> app) {
        return new Application<>(app, plugin);
    }
}

