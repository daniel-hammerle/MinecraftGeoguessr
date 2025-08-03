package at.daniel.geoGuessr;

import org.bukkit.Location;

public record Vec2D(int x, int y) {
    public static Vec2D fromLocation(Location location) {
        return new Vec2D(location.getBlockX(), location.getBlockZ());
    }

    public boolean isInRange(Location location, int range) {
        double dx = Math.abs(location.getX() - x);
        double dy = Math.abs(location.getZ() - y);

        return (dx * dx + dy * dy) <= (range * range);
    }

    public Vec2D minus(Vec2D other) {
        return new Vec2D(x - other.x, y - other.y);
    }
}