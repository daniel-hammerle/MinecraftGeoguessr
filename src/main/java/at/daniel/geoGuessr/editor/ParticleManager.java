package at.daniel.geoGuessr.editor;

import at.daniel.flow.Ctx;
import at.daniel.geoGuessr.GeoGuessr;
import at.daniel.geoGuessr.GeoGuessrMap;
import at.daniel.geoGuessr.Road;
import at.daniel.geoGuessr.Vec2D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class ParticleManager implements AutoCloseable {
    private final Ctx<GeoGuessr> ctx;
    private final GeoGuessrMap map;
    private final int renderRange;
    private World world;
    private BukkitTask task;

    private volatile List<Vector> waypoints;
    public ParticleManager(Ctx<GeoGuessr> ctx, GeoGuessrMap map, int renderRange) {
        this.ctx = ctx;
        this.map = map;
        this.renderRange = renderRange;
    }

    public void enable() {
        world = Bukkit.getWorld(map.getWorldId());
        if (world == null) {
            throw new IllegalStateException("World does not exist");
        }

        task = Bukkit.getScheduler().runTaskTimer(ctx.plugin(), this::update, 0, 10);
    }

    private int count = 0;
    public void update() {
        Location playerLocation = ctx.player().getLocation();
        if (waypoints != null) {
            renderRoad(waypoints, playerLocation);
        }

        if (count++ % 4 != 0) return;

        renderBeam(map.getStart(), playerLocation);
        renderBeam(new Vec2D(map.getStart().x(), map.getEnd().y()), playerLocation);
        renderBeam(new Vec2D(map.getEnd().x(), map.getStart().y()), playerLocation);
        renderBeam(map.getEnd(), playerLocation);

        for (Road road : map.getRoads()) {
            renderRoad(Arrays.asList(road.waypoints()), playerLocation);
        }


    }

    void renderRoad(Iterable<Vector> road, Location playerLocation) {
        Vector previous = null;
        boolean previousInView = false;
        for (Vector waypoint : road) {
            boolean isInView = waypoint.isInSphere(playerLocation.toVector(), renderRange);
            if (isInView) {
                renderWayPoint(waypoint);
            }

            if (!isInView  && !previousInView) {
                //no need to draw line in between
                previous = waypoint;
                continue;
            }

            if (previous != null) drawLine(previous, waypoint);

            previous = waypoint;
            previousInView = isInView;
        }
    }

    public void setTemporaryRoad(@Nullable List<Vector> waypoints) {
        this.waypoints = waypoints;
    }

    void drawLine(Vector first, Vector second) {
        double dist;
        Vector step;
        {
            Vector distVec = new Vector(second.getX() - first.getX(), second.getY() - first.getY(), second.getZ() - first.getZ());
            dist = distVec.length();
            step = distVec
                    .normalize();
        }

        for (double i = 0; i < dist; i += .5) {
            world.spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    first.getX() + step.getX() * i,
                    first.getY() + step.getY() * i,
                    first.getZ() + step.getZ() * i,
                    1
            );
        }
    }

    void renderWayPoint(Vector waypoint) {
        world.spawnParticle(Particle.GLOW, waypoint.getX(), waypoint.getY(), waypoint.getZ(), 5);
        world.spawnParticle(Particle.GLOW, waypoint.getX(), waypoint.getY() + 1, waypoint.getZ(), 5);
    }

    void renderBeam(Vec2D beam, Location playerLocation) {
        if (!beam.isInRange(playerLocation, renderRange)) return;
        double playerY =playerLocation.getY();

        for(double d = playerY - 20; d < playerY + 20; d+=0.5) {
            world.spawnParticle(Particle.HAPPY_VILLAGER, beam.x(), d, beam.y(), 5);
        }
    }

    @Override
    public void close() {
        task.cancel();
    }
}
