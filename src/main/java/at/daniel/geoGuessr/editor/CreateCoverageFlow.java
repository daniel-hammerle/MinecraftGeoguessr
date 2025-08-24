package at.daniel.geoGuessr.editor;

import at.daniel.flow.Ctx;
import at.daniel.flow.EventHandle;
import at.daniel.flow.Filter;
import at.daniel.flow.util.InventoryUtil;
import at.daniel.geoGuessr.Capture;
import at.daniel.geoGuessr.Road;
import at.daniel.geoGuessr.RoadKind;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CreateCoverageFlow {

    public static @Nullable Selection createCoverageMetaData(Ctx<?> ctx) {
        CompletableFuture<String> textFuture = new CompletableFuture<>();
        AnvilGUI.Builder builder = new AnvilGUI.Builder().plugin(ctx.plugin())
                .title("Enter a name")
                .itemLeft(Item.Nothing)
                .onClick((a, s) ->
                    List.of(AnvilGUI.ResponseAction.close())
                )
                .onClose(it -> textFuture.complete(it.getText()));

        ctx.sync(() -> builder.open(ctx.player()));

        String name;
        try {
            name = textFuture.get(1000*20, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            //Timed out
            ctx.player().sendMessage("You failed to provide a name therefore the creation is abandoned!");
            return null;
        }

        ItemType type = selectType(ctx);

        return new Selection(name, type);
    }


    static ItemType selectType(Ctx<?> ctx) {
        Inventory inv = ctx.player().getInventory();
        InventoryUtil.clear(ctx, inv);

        ctx.sync(() -> {
            for (int i = 0; i < ItemType.values().length; i++) {
                inv.setItem(i, ItemType.values()[i].getItem());
            }
        });

        try (var handle = ctx.subscribe(PlayerInteractEvent.class, Filter.isPlayer(ctx.player()), true)) {
            for(;;) {
                PlayerInteractEvent event = handle.awaitEvent();
                ItemStack item = event.getItem();
                if (item == null) {
                    continue;
                }

                Optional<ItemType> type = Arrays.stream(ItemType.values()).filter(it -> it.getItem().equals(item)).findFirst();
                if (type.isEmpty()) {
                    continue;
                }
                return type.get();
            }
        }
    }

    public record Selection(String name, ItemType type) {}

    static RoadKind selectRoadKind(Ctx<?> ctx) {
        Inventory inv = ctx.sync(() -> Bukkit.createInventory(null, InventoryType.HOPPER, Component.text("Select a kind")));

        ctx.sync(() -> {
            for (RoadKind value : RoadKind.values()) {
                inv.addItem(value.getItem());
            }
            ctx.player().openInventory(inv);
        });


        try (var handle = EventHandle.join(
                ctx.subscribe(InventoryClickEvent.class, Filter.isInventory(inv), true),
                ctx.subscribe(InventoryCloseEvent.class, Filter.isPlayer(ctx.player())))
        ) {
            for(;;) {
                switch (handle.awaitEvent()) {
                    case InventoryCloseEvent ignoreEvent -> {
                        Sounds.play(ctx, Sound.BLOCK_CHEST_CLOSE);
                        return null;
                    }
                    case InventoryClickEvent event -> {
                        Optional<RoadKind> type = Arrays
                                .stream(RoadKind.values())
                                .filter(it -> it.getItem().equals(event.getCurrentItem()))
                                .findFirst();

                        if (type.isEmpty()) {
                            continue;
                        }
                        Sounds.click(ctx);
                        ctx.sync(() -> ctx.player().closeInventory());
                        return type.get();
                    }
                    default -> throw new RuntimeException("Unreachable");
                }
            }
        }



    }

    static final Set<ItemStack> SpecialItems = Set.of(Item.SelectionStick, Item.Undo, Item.ExitItem);

    static @Nullable Road createRoadCoverage(Ctx<?> ctx, String name, ParticleManager particles) {

        RoadKind roadKind = selectRoadKind(ctx);
        if (roadKind == null) {
            return null;
        }

        List<Vector> waypoints = new LinkedList<>();
        particles.setTemporaryRoad(waypoints);

        //load the waypoint creation tools:
        Inventory inv = ctx.player().getInventory();
        InventoryUtil.clear(ctx, inv);

        ctx.sync(() -> {
            inv.setItem(0, Item.SelectionStick);
            inv.setItem(6, Item.Undo);
            inv.setItem(8, Item.ExitItem);
            inv.setItem(7, Item.ConfirmationPane);
        });



        try (var handle = ctx.subscribe(
                PlayerInteractEvent.class,
                it -> it.getPlayer().equals(ctx.player()) && SpecialItems.contains(it.getItem()),
                true
        )) {
            for(;;) {
                PlayerInteractEvent event = handle.awaitEvent();

                ItemStack currentItem = event.getItem();

                if (Item.ExitItem.equals(currentItem)) {
                    Sounds.play(ctx, Sound.BLOCK_CHEST_CLOSE);
                    particles.setTemporaryRoad(null);
                    return null;
                }

                if (Item.SelectionStick.equals(currentItem)) {
                    Sounds.click(ctx);
                    handleSelection(ctx, event, waypoints, handle);
                }

                if (Item.ConfirmationPane.equals(currentItem)) {
                    Sounds.play(ctx, Sound.ENTITY_PLAYER_LEVELUP);
                    break;
                }

                //TODO undo logic etc
            }
        }




        particles.setTemporaryRoad(null);

        return Road.create(waypoints.toArray(new Vector[0]), roadKind);
    }

    static @Nullable Capture createCapture(Ctx<?> ctx, String name) {
        return null;
    }

    static void handleSelection(Ctx<?> ctx, PlayerInteractEvent event, List<Vector> waypoints, EventHandle<PlayerInteractEvent> interactHandle) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        Vector location = clickedBlock.getLocation().toVector().add(new Vector(0, 1, 0));
        int index = waypoints.indexOf(location);
        if (index == -1) {
            //in this case we clicked on a new block so we just want to add a new point
            waypoints.add(location);
            Sounds.play(ctx, Sound.ENTITY_PLAYER_LEVELUP);
            ctx.player().sendMessage("New waypoint selected at " + location.getX() + ", " + location.getY()+ ", " + location.getZ());
            return;
        }
        Vector initLocation = waypoints.get(index);
        //in this case we clicked an existing waypoint so we probably want to change it
        ctx.player().sendMessage("Waypoint picked up at " + location.getX() + ", " + location.getY()+ ", " + location.getZ());

        try(
            var handle = EventHandle.join(
                ctx.subscribe(PlayerMoveEvent.class, Filter.isPlayer(ctx.player())),
                EventHandle.preserve(interactHandle)
        ))  {
            for(;;) {
                switch (handle.awaitEvent()) {
                    case PlayerInteractEvent e -> {
                        if (e.getAction().isRightClick()) {
                            Sounds.play(ctx, Sound.UI_BUTTON_CLICK);
                            ctx.player().sendMessage("Reset");
                            waypoints.set(index, initLocation); //reset location bacv
                            return;
                        }

                        Block block = e.getClickedBlock();
                        if (block == null) {
                            //invalid input probably just a misclick of the user just continue
                            continue;
                        }
                        //clicked the final location waypoint is updated
                        waypoints.set(index, block.getLocation().toVector());
                        ctx.player().sendMessage("Waypoint changed");

                        return;
                    }
                    case PlayerMoveEvent ignore -> {
                        Block facingBlock = ctx.player().getTargetBlock(Set.of(Material.AIR), 100);
                        //until the player clicked we still update wherever the player is facing
                        //this allows for a smooth animation for the particles so the player can see what they change
                        waypoints.set(index, facingBlock.getLocation().toVector());
                    }
                    default -> throw new RuntimeException("Unreachable");
                }
            }

        }


    }



}
