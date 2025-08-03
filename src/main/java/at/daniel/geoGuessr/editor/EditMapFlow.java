package at.daniel.geoGuessr.editor;

import at.daniel.flow.Ctx;
import at.daniel.flow.Filter;
import at.daniel.flow.util.Events;
import at.daniel.flow.util.InventoryUtil;
import at.daniel.flow.util.PlayerUtil;
import at.daniel.geoGuessr.Capture;
import at.daniel.geoGuessr.GeoGuessr;
import at.daniel.geoGuessr.GeoGuessrMap;
import at.daniel.geoGuessr.Road;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;


public class EditMapFlow {

    public static void editMap(Ctx<GeoGuessr> ctx, GeoGuessrMap map) {
        Player player = ctx.player();

        InventoryUtil.clear(ctx, player.getInventory());
        ctx.sync(() -> {
            player.getInventory().setItem(0, Item.CreateItem);
            player.getInventory().setItem(8, Item.ExitItem);
        });

        try (
                var handle = ctx.subscribe(PlayerInteractEvent.class, Filter.isPlayer(player));
                var particleManager = new ParticleManager(ctx, map, 40)
        ) {
            particleManager.enable();
            while (true) {
                PlayerInteractEvent event = handle.awaitEvent();
                ItemStack item = event.getItem();
                if (item == null) {
                    continue;
                }

                if (item.equals(Item.ExitItem)) {
                    ctx.sync(() -> ctx.player().playSound(ctx.player().getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 1));
                    break;
                }

                if (item.equals(Item.CreateItem)) {
                    ctx.sync(() -> ctx.player().playSound(ctx.player().getLocation(), Sound.ENTITY_GLOW_ITEM_FRAME_PLACE, 1, 1));
                   Events.withPausedHandler(ctx, handle, c -> PlayerUtil.preserveInventory(ctx, d -> {
                       handleCreateLogic(c, map, particleManager);
                       return null;
                   }));
                }
            }
        }


    }

    static void handleCreateLogic(Ctx<GeoGuessr> ctx, GeoGuessrMap map, ParticleManager particles) {
        //handle create logic
        CreateCoverageFlow.Selection selection = CreateCoverageFlow.createCoverageMetaData(ctx);
        if (selection == null) {
            return;
        }
        switch (selection.type()) {
            case Road -> {
                Road road = CreateCoverageFlow.createRoadCoverage(ctx, selection.name(), particles);
                if (road == null) {
                    ctx.player().sendMessage("Invalid selection canceling!");
                    return;
                }

                map.getRoads().add(road);
            }
            case Capture -> {
                Capture capture = CreateCoverageFlow.createCapture(ctx, selection.name());
                if (capture == null) {
                    ctx.player().sendMessage("Invalid selection canceling!");
                    return;
                }

                map.getCaptures().add(capture);
            }
        }
    }
}
