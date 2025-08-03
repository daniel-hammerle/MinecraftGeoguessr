package at.daniel.flow.util;

import at.daniel.flow.Ctx;
import at.daniel.flow.Flow;
import at.daniel.flow.VoidFlow;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerUtil {
    public static <P extends JavaPlugin, R, Props> R withTemporaryGameMode(Ctx<P> ctx, GameMode gameMode,  Flow<P, R> flow) {
        Player player = ctx.player();
        GameMode initial = ctx.sync(player::getGameMode);
        ctx.sync(() -> player.setGameMode(gameMode));

        try {
            return flow.run(ctx);
        } finally {
            ctx.sync(() -> player.setGameMode(initial));
        }
    }

    public static<P extends JavaPlugin, R> R preserveInventory(Ctx<P> ctx, Flow<P, R> flow) {
        ItemStack[] contents = ctx.sync(() -> ctx.player().getInventory().getStorageContents().clone());
        try {
            return flow.run(ctx);
        } finally {
            ctx.sync(() -> ctx.player().getInventory().setContents(contents));
        }
    }

}
