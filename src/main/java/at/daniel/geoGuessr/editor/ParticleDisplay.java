package at.daniel.geoGuessr.editor;

import at.daniel.geoGuessr.GeoGuessr;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class ParticleDisplay implements Display {
    final int x;
    final int z;
    final World world;

    private BukkitTask runnable;
    private final GeoGuessr plugin;

    public ParticleDisplay(int x, int z, World world, GeoGuessr plugin) {
        this.x = x;
        this.z = z;
        this.world = world;
        this.plugin = plugin;
    }

    public static ParticleDisplay launch(int x, int z, World world, GeoGuessr plugin) {
        ParticleDisplay display = new ParticleDisplay(x, z, world, plugin);
        display.start();
        return display;
    }

    @Override
    public void start() {
        final double minY = 0;
        final double maxY = 255;
        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                for (double y = minY; y <= maxY; y+= 0.25) {
                    // Create the location with fixed X and Z and varying Y
                    Location particleLocation = new Location(world, x + 0.5, y, z + 0.5);

                    // Send a particle to that location
                    world.spawnParticle(Particle.HAPPY_VILLAGER, particleLocation, 0); // You can change the particle type
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    @Override
    public void stop() {
        runnable.cancel();
    }
}
