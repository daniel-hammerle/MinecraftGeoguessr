package at.daniel.flow;

import org.bukkit.plugin.java.JavaPlugin;

public interface VoidFlow<P extends JavaPlugin> {
    void run(Ctx<P> ctx);
}
