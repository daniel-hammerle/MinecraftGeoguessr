package at.daniel.geoGuessr.editor;

import at.daniel.geoGuessr.Vec2D;
import org.bukkit.World;

public record LocationObject(Vec2D first, Vec2D second, World world) {
}
