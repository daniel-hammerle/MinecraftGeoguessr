package at.daniel.geoGuessr.maps;

import at.daniel.geoGuessr.GeoGuessrMap;
import at.daniel.geoGuessr.Vec2D;
import org.bukkit.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MapManager {
    private final List<GeoGuessrMap> maps;

    public MapManager(GeoGuessrMap[] maps) {
        this.maps = new ArrayList<>(Arrays.asList(maps));
    }

    public List<GeoGuessrMap> getMaps() {
        return maps;
    }

    public @Nullable GeoGuessrMap getMap(String name) {
        return maps.stream().filter(it -> it.getName().equals(name)).findFirst().orElse(null);
    }

    public void addMap(World world, Vec2D start, Vec2D end, String name) {
        Vec2D actualStart = new Vec2D(Math.min(start.x(), end.x()), Math.min(start.y(), end.y()));
        Vec2D actualEnd = new Vec2D(Math.max(start.x(), end.x()), Math.max(start.y(), end.y()));
        maps.add(new GeoGuessrMap(name, world.getUID(), actualStart, actualEnd, new ArrayList<>(), new ArrayList<>()));
    }

    public boolean mapExists(String name) {
        return maps.stream().anyMatch(it -> it.getName().equals(name));
    }
}
