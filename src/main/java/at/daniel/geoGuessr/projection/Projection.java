package at.daniel.geoGuessr.projection;

import org.bukkit.World;
import org.bukkit.util.Vector;

public record Projection(World world, int[] scales, int mapHeight, int lenX, int lenY) {
}
