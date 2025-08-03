package at.daniel.flow.util;

import at.daniel.flow.Ctx;
import at.daniel.flow.EventHandle;
import at.daniel.flow.Flow;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;

public class Events {
    public static<R, P extends JavaPlugin, E extends Event> R withPausedHandler(Ctx<P> ctx, EventHandle<E> handle, Flow<P, R> flow) {
        handle.pause();
        try {
            return flow.run(ctx);
        } finally {
            handle.resume();
        }
    }
}
