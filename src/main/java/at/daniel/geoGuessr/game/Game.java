package at.daniel.geoGuessr.game;

import at.daniel.flow.Ctx;
import at.daniel.flow.EventHandle;
import at.daniel.flow.Filter;
import at.daniel.geoGuessr.GeoGuessr;
import at.daniel.geoGuessr.GeoGuessrMap;
import at.daniel.geoGuessr.Vec2D;
import at.daniel.geoGuessr.editor.Item;
import at.daniel.geoGuessr.projection.Projection;
import io.papermc.paper.entity.TeleportFlag;
import org.apache.commons.lang3.NotImplementedException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class Game {


    public static void playGame(Ctx<GeoGuessr> ctx, Player[] players, GeoGuessrMap map, Settings settings) {
        Player[] allPlayers = Stream.concat(Arrays.stream(players), Stream.of(ctx.player()))
                .toArray(Player[]::new);

        Projection proj = ctx.sync(() -> ctx.plugin().getProjectionManager().getProjection(map, allPlayers.length));

        World world = Bukkit.getWorld(map.getWorldId());
        if (world == null) {
            ctx.player().sendMessage("Map does not exist!");
            return;
        }
        Random random = new Random();

        for(;;) {
            //Find a random location to guess
            Vector location = map.randomPoint(random);

            Map<Player, Vector> distances = HashMap.newHashMap(allPlayers.length);

            Location loc = new Location(world, location.getX(), location.getY(), location.getZ());
            ctx.nonBlocking(() -> {
                for(Player player : allPlayers) {
                    player.teleport(loc, PlayerTeleportEvent.TeleportCause.UNKNOWN, TeleportFlag.EntityState.RETAIN_VEHICLE);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100000, 1, true, false, false));
                }
            });

            //launch handler for each player
            var guesses = mapIndexed(
                    Arrays.stream(players),
                    (i, player) -> ctx.launchForPlayer(player, context -> findGuess(context, map, proj, i)))
                    .map(CompletableFuture::join)
                    .toArray(Vector[]::new);


            //display finish screen
            //for this we need to find an apropriate projection level

        }
    }

    static Vector findGuess(Ctx<GeoGuessr> ctx, GeoGuessrMap map, Projection proj, int playerIdx) {
        var inv = ctx.player().getInventory();

        try (var handler = ctx.subscribe(PlayerInteractEvent.class, Filter.isPlayer(ctx.player()))) {
            while(true) {
                ctx.sync(() -> {
                    for (int i = 0; i < 9; ++i) {
                        inv.setItem(i, Item.MapIcon);
                    }
                });

                var _ = handler.awaitEvent();
                handler.pause();
                var point = makeGuess(ctx, map, proj, playerIdx);
                handler.resume();
                if (point != null) return point;
            }
        }
    }

    static class ProjectionHandle {
        private final int[] scales;
        private double scaleFactor;
        private final int layerHeight;
        private int projectionIdx;

		ProjectionHandle(Projection projection) {
			this.scales = projection.scales();
            this.projectionIdx = scales.length - 1;
            this.layerHeight = projection.mapHeight() / scales.length;
            this.scaleFactor = (double) scales[projectionIdx] / (double) scales[Math.max(projectionIdx-1, 0)];
        }

        public int getRequiredLayerShift(double y) {
            if(y - projectionIdx*layerHeight < (double) layerHeight / scaleFactor) return -1;
            if (y - projectionIdx*layerHeight == layerHeight) return 1;
            return 0;
        }

        public boolean isLowestLayer() {
            return projectionIdx <= 1;
        }

        public boolean moveDown() {
            if (isLowestLayer()) return false;
            int nextScale = scales[projectionIdx--];
            scaleFactor = (double) nextScale / (double) scales[Math.max(projectionIdx-1, 0)];
            return true;
        }

        public boolean isHighestLayer() {
            return projectionIdx >= scales.length - 1;
        }

        public double scaleFactor() {
            return scaleFactor;
        }

        public boolean moveUp() {
            if (isHighestLayer()) return false;
            int nextScale = scales[projectionIdx++];
            scaleFactor = (double) scales[Math.max(projectionIdx-1, 0)] / (double) nextScale;
            return true;
        }

	}

    static @Nullable Vector makeGuess(Ctx<GeoGuessr> ctx, GeoGuessrMap map, Projection proj, int playerIdx) {
        ctx.sync(() -> {
            var inv = ctx.player().getInventory();
            inv.clear();
            inv.setItem(0, Item.ConfirmationPane);
            inv.setItem(1, Item.ExitItem);
            ctx.player().setFlying(true);
        });

        var initLocation = ctx.sync(() -> ctx.player().getLocation());
        var cleanLocation = Vec2D.fromLocation(initLocation).minus(map.getStart());

        var offsetX = proj.lenX() * playerIdx;
        var offsetY = proj.lenY() * playerIdx;

        var currentLocation =new Location(
                proj.world(),
                cleanLocation.x() + offsetX,
                proj.mapHeight(),
                cleanLocation.y() + offsetY,
                0f,
                90f
        );

		ctx.nonBlocking(() -> ctx.player().teleport(currentLocation));

        var projectionHandle = new ProjectionHandle(proj);

        try(var handler = EventHandle.join(
                ctx.subscribe(PlayerMoveEvent.class, Filter.isPlayer(ctx.player())),
                ctx.subscribe(PlayerInteractEvent.class, Filter.isPlayer(ctx.player()))
        )) {
            while(true) {
                var event = handler.awaitEvent();

                switch (event) {
                case PlayerMoveEvent moveEvent -> {
                   handleMoveEvent(ctx, moveEvent, projectionHandle);
                }
                case PlayerInteractEvent interactEvent -> {
                    if (interactEvent.getItem() == Item.ConfirmationPane) {
                        ctx.nonBlocking(() -> {
                            ctx.player().teleport(initLocation);
                            ctx.player().setFlying(false);
                        });
                        return currentLocation.toVector();
                    }
                    if (interactEvent.getItem() == Item.ExitItem) {
                        ctx.nonBlocking(() -> {
                            ctx.player().teleport(initLocation);
                            ctx.player().setFlying(false);
                        });
                        return null;
                    }
                }
                default -> throw new IllegalStateException("Unreachable");
                }
            }
        }
    }

    static void handleMoveEvent(Ctx<GeoGuessr> ctx, PlayerMoveEvent moveEvent, ProjectionHandle handle) {
        var currentLocation = moveEvent.getTo();

        int change = handle.getRequiredLayerShift(currentLocation.y());

        if (change == 0) return;
        double scaleFactor = handle.scaleFactor();
        if (change == -1) {
            if (!handle.moveDown()) return;
            currentLocation = currentLocation.multiply(scaleFactor);
        }
        if (change == 1) {
            if (!handle.moveUp()) return;
            currentLocation = currentLocation.multiply(1 / scaleFactor);
        }

        Location finalCurrentLocation = currentLocation;
        ctx.sync(() -> {
            ctx.player().teleport(finalCurrentLocation);
            ctx.player().setVelocity(ctx.player().getVelocity().multiply(change == -1 ? scaleFactor : 1 / scaleFactor));
        });
    }


    public static <T, R> Stream<R> mapIndexed(
            Stream<T> stream,
            BiFunction<Integer, T, R> mapper
    ) {
        AtomicInteger index = new AtomicInteger(0);
        return stream.map(item -> mapper.apply(index.getAndIncrement(), item));
    }

}
