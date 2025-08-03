package at.daniel.geoGuessr.editor;

import at.daniel.flow.Ctx;
import at.daniel.flow.EventHandle;
import at.daniel.flow.Filter;
import at.daniel.flow.Flow;
import at.daniel.flow.util.InventoryUtil;
import at.daniel.geoGuessr.GeoGuessr;
import at.daniel.geoGuessr.GeoGuessrMap;
import at.daniel.geoGuessr.Vec2D;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public class CreateMapFlow {



    public static LocationObject createMapFlow(Ctx<GeoGuessr> ctx) {
        Player player = ctx.player();

        ctx.sync(() -> player.getInventory().setItem(0, Item.SelectionStick));
        ParticleDisplay[] displays = new ParticleDisplay[4];
        Location firstMarker;
        Location secondMarker;
        player.sendMessage("called");
        try(var handle = ctx.subscribe(PlayerInteractEvent.class, Filter.isPlayer(player), true)) {
            firstMarker = selectLocation(handle, ctx);

            displays[0] = createDisplay(
                    ctx,
                    firstMarker.getWorld(),
                    firstMarker.getBlockX(),
                    firstMarker.getBlockZ()
            );
            secondMarker = selectLocation(handle, ctx);
        }

        displays[1] = createDisplay(
                ctx,
                secondMarker.getWorld(),
                secondMarker.getBlockX(),
                secondMarker.getBlockZ()
        );
        displays[2] = createDisplay(
                ctx,
                firstMarker.getWorld(),
                firstMarker.getBlockX(),
                secondMarker.getBlockZ()
        );
        displays[3] = createDisplay(
                ctx,
                firstMarker.getWorld(),
                secondMarker.getBlockX(),
                firstMarker.getBlockZ()
        );

        //Now we place the confirmation item
        ctx.sync(() -> {
            player.getInventory().setItem(0, Item.ConfirmationPane);
            player.getInventory().setItem(8, Item.ExitItem);
        });

        try(var handle = ctx.subscribe(PlayerInteractEvent.class, Filter.isPlayer(player), true)) {
            for(;;) {
                PlayerInteractEvent event = handle.awaitEvent(Duration.ofSeconds(20));
                if (Objects.equals(event.getItem(), Item.ExitItem)) {
                    cleanUpDisplays(ctx, displays);
                    Sounds.play(ctx, Sound.BLOCK_CHEST_CLOSE);
                    return null;
                }
                if (Objects.equals(event.getItem(), Item.ConfirmationPane)) {
                    cleanUpDisplays(ctx, displays);
                    Sounds.play(ctx, Sound.ENTITY_ARROW_HIT_PLAYER);
                    return new LocationObject(Vec2D.fromLocation(firstMarker), Vec2D.fromLocation(secondMarker), firstMarker.getWorld());
                }
            }
        } catch (TimeoutException e) {
            cleanUpDisplays(ctx, displays);
            return null;
        }

    }

    static void cleanUpDisplays(Ctx<?> ctx, ParticleDisplay[] displays) {
        ctx.sync(() -> {
            for (ParticleDisplay display : displays) {
                display.stop();
            }
        });

    }

    static ParticleDisplay createDisplay(Ctx<GeoGuessr> ctx, World world, int x, int z) {
        ParticleDisplay display = new ParticleDisplay(
                x, z,
                world,
                ctx.plugin()
        );

        ctx.nonBlocking(display::start);
        return display;
    }

    static Location selectLocation(EventHandle<PlayerInteractEvent> handle, Ctx<GeoGuessr> ctx) {
        while(true) {
            PlayerInteractEvent event = handle.awaitEvent();
            if (event.getClickedBlock() != null) {
                Sounds.click(ctx);
                event.getPlayer().sendMessage("Location selected!");
                return event.getClickedBlock().getLocation();

            }
        }
    }

}
