package at.daniel.geoGuessr.game;

import at.daniel.flow.Ctx;
import at.daniel.flow.EventHandle;
import at.daniel.flow.Filter;
import at.daniel.flow.Flow;
import at.daniel.geoGuessr.GeoGuessr;
import at.daniel.geoGuessr.GeoGuessrMap;
import at.daniel.geoGuessr.Vec2D;
import at.daniel.geoGuessr.editor.Item;
import at.daniel.geoGuessr.projection.Projection;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.text.Component;
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

public class Game implements Flow.Entry<GeoGuessr, Void, GameSpecifications> {

    private static final int MaxPointsPerRound = 5000;
    private static final int InfiniteEffectDuration = 1000000;
    private static final int NoWinningPlayer = -1;
    private static final float PitchStraightDown = 90f;

    private Player[] allPlayers;
    private int[] points;
    private GeoGuessrMap map;
    private Settings settings;
    private Ctx<GeoGuessr> ctx;

    @Override
    public Void run(Ctx<GeoGuessr> ctx, GameSpecifications gameSpecifications) {
        Stream<Player> otherPlayersStream = Arrays.stream(gameSpecifications.players());
        Stream<Player> hostStream = Stream.of(ctx.player());

        allPlayers = Stream.concat(hostStream, otherPlayersStream).toArray(Player[]::new);
        points = new int[allPlayers.length];
        map = gameSpecifications.map();
        settings = gameSpecifications.settings();
        this.ctx = ctx;

        playGame();
        return null;
    }

    private void playGame() {
        // Prepare projection and world
        Projection projection = ctx.sync(() -> ctx.plugin().getProjectionManager().getProjection(map, allPlayers.length));

        @Nullable World world = Bukkit.getWorld(map.getWorldId());
        if (world == null) {
            ctx.player().sendMessage("Map does not exist!");
            return;
        }

        //prepare mapSize constant
        Vec2D start = map.getStart();
        Vec2D end = map.getEnd();
        int mapSize = Math.max(end.x() - start.x(), end.y() - start.y());

        Random random = new Random();

        //game loop
        int winningPlayer = NoWinningPlayer;
        while(winningPlayer == NoWinningPlayer) {
            //Find a random location to guess
            Vector location = map.randomPoint(random);
            setupPlayersForRound(location, world);
            //launch handler for each player
            Vector[] guesses = makeGuesses(projection);

            int[] roundPoints = calculatePoints(guesses, location, mapSize);

            for (int i = 0; i < roundPoints.length; i++) {
                points[i] += roundPoints[i];
            }

            winningPlayer = getWinningPlayer();
        }

    }

    private void setupPlayersForRound(Vector location, World world) {
        Location locationObject = new Location(world, location.getX(), location.getY(), location.getZ());

        PotionEffectType effectType = PotionEffectType.INVISIBILITY;
        int amplifier = 1;
        boolean ambient = true;
        boolean showParticles = false;
        boolean showIcon = false;
        PotionEffect effect = new PotionEffect(effectType, InfiniteEffectDuration, amplifier, ambient, showParticles, showIcon);

        ctx.nonBlocking(() -> {
            for(Player player : allPlayers) {
                player.teleport(locationObject, PlayerTeleportEvent.TeleportCause.UNKNOWN, TeleportFlag.EntityState.RETAIN_VEHICLE);
                player.addPotionEffect(effect);
            }
        });
    }

    private Vector[] makeGuesses(Projection projection) {
        return mapIndexed(
                Arrays.stream(allPlayers),
                (i, player) -> ctx.launchForPlayer(player, context -> findGuess(context, projection, i)))
                .map(CompletableFuture::join)
                .toArray(Vector[]::new);
    }

    private int[] calculatePoints(Vector[] guesses, Vector location, int mapSize) {
        return Arrays.stream(guesses)
                .map(guess -> guess.distance(location))
                .mapToInt(distance -> MaxPointsPerRound - (int) (distance / mapSize))
                .toArray();
    }

    private Vector findGuess(Ctx<GeoGuessr> ctx, Projection proj, int playerIdx) {
        var inv = ctx.player().getInventory();

        try (var handler = ctx.subscribe(PlayerInteractEvent.class, Filter.isPlayer(ctx.player()))) {
            while(true) {
                ctx.sync(() -> {
                    for (int i = 0; i < 9; ++i) {
                        inv.setItem(i, Item.MapIcon);
                    }
                });

                var ignoreEvent = handler.awaitEvent();
                handler.pause();
                var point = makeGuess(ctx, proj, playerIdx);
                handler.resume();
                if (point != null) return point;
            }
        }
    }

    private int getWinningPlayer() {
        for (int i = 0; i < points.length; i++) {
            if (points[i] >= settings.winningPoints()) {
                return i;
            }
        }
        return -1;
    }


    private static class ProjectionHandle {
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
    private @Nullable Vector makeGuess(Ctx<GeoGuessr> ctx, Projection projection, int playerIdx) {
        ctx.sync(() -> {
            var inv = ctx.player().getInventory();
            inv.clear();
            inv.setItem(0, Item.ConfirmationPane);
            inv.setItem(1, Item.ExitItem);
            ctx.player().setAllowFlight(true);
            ctx.player().setFlying(true);
        });

        var initLocation = ctx.sync(() -> ctx.player().getLocation());

        int offsetX = projection.lenX() * playerIdx;
        int offsetY = projection.lenY() * playerIdx;

        float yaw = 0f;

        var currentLocation = new Location(
                projection.world(),
                (double) projection.lenX() / 2 + offsetX,
                projection.mapHeight(),
                (double) projection.lenY() / 2  + offsetY,
                yaw,
                PitchStraightDown
        );

		ctx.sync(() -> ctx.player().teleport(currentLocation));

        var projectionHandle = new ProjectionHandle(projection);

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
                            ctx.player().setAllowFlight(false);
                        });
                        return currentLocation.toVector();
                    }
                    if (interactEvent.getItem() == Item.ExitItem) {
                        ctx.nonBlocking(() -> {
                            ctx.player().teleport(initLocation);
                            ctx.player().setFlying(false);
                            ctx.player().setAllowFlight(false);
                        });
                        return null;
                    }
                }
                default -> throw new IllegalStateException("Unreachable");
                }
            }
        }
    }

    private void handleMoveEvent(Ctx<GeoGuessr> ctx, PlayerMoveEvent moveEvent, ProjectionHandle handle) {
        var currentLocation = moveEvent.getTo();

        int change = handle.getRequiredLayerShift(currentLocation.y());
        ctx.player().sendMessage(Component.text(change));
        if (change == 0) return;

        double scaleFactor = handle.scaleFactor();
        if (change == -1) {
            if (!handle.moveDown()) {
                ctx.player().sendMessage("Could not move down...");
                return;
            }
            ctx.player().sendMessage("moving down");
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
