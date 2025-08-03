package at.daniel.geoGuessr.commands;

import at.daniel.flow.Ctx;
import at.daniel.flow.EventHandle;
import at.daniel.flow.Filter;
import at.daniel.flow.Flow;
import at.daniel.geoGuessr.GeoGuessr;
import at.daniel.geoGuessr.editor.Item;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class TravelCommand implements CommandExecutor {

    private final GeoGuessr plugin;

    private Flow.Application<GeoGuessr, Void, Void> app;

    public TravelCommand(GeoGuessr plugin) {
        this.plugin = plugin;
    }

    private Flow.Application<GeoGuessr, Void, Void> getFlowApp(GeoGuessr plugin) {
        if (this.app == null) {
            this.app = Flow.create(plugin, (ctx, ignore) -> {
                TravelCommand.transportPlayer(ctx);
                return null;
            });
        }
        return this.app;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        getFlowApp(plugin).forPlayer((Player) sender, null);
        return true;
    }


    static void transportPlayer(Ctx<GeoGuessr> ctx) {
        List<World> worlds = ctx.sync(Bukkit::getWorlds);

        World selection = worldSelection(ctx, worlds);
        if (selection == null) {
            return;
        }

        ctx.sync(() -> {
            Location oldLoc = ctx.player().getLocation();
            oldLoc.setWorld(selection);
            ctx.player().teleport(oldLoc);
        });
    }

    static @Nullable World worldSelection(Ctx<GeoGuessr> ctx, List<World> worlds) {
        Inventory inv = ctx.sync(() -> Bukkit.createInventory(null, InventoryType.CHEST, Component.text("Select world")));

        ctx.sync(() -> {
            for (World world : worlds) {
                ItemStack item = new ItemStack(Material.ACACIA_LEAVES);
                item.editMeta(it -> it.displayName(Component.text(world.getName())));
                inv.addItem(item);
            }
            ctx.player().openInventory(inv);
        });

        try (var handle = EventHandle.join(
                ctx.subscribe(InventoryClickEvent.class, Filter.isPlayer(ctx.player()), true),
                ctx.subscribe(InventoryCloseEvent.class, Filter.isPlayer(ctx.player())))
        ) {
            for(;;) {
                switch (handle.awaitEvent(Duration.ofSeconds(10))) {
                    case InventoryClickEvent e -> {
                        ItemStack currentItem = e.getCurrentItem();
                        if (currentItem == null) {
                            continue;
                        }

                        String displayName = currentItem.getItemMeta().getDisplayName();

                        return worlds.stream().filter(it -> it.getName().equals(displayName)).findFirst().orElse(null);
                    }
                    case InventoryCloseEvent ignoreE -> {
                        return null;
                    }
                    default -> throw new AssertionError("Unreachable!");
                }
            }
        } catch (TimeoutException e) {
            ctx.player().sendMessage(Component.text("You took too long to make a selection!"));
            ctx.sync(() -> ctx.player().closeInventory());
            return null;
        }
    }
}
