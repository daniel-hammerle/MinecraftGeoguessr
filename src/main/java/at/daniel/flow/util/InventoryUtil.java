package at.daniel.flow.util;

import at.daniel.flow.Ctx;
import org.bukkit.inventory.Inventory;

public class InventoryUtil {
    public static void clear(Ctx<?> ctx, Inventory inventory) {
        ctx.sync(() -> {
            inventory.clear();
        });
    }


}
