package at.daniel.geoGuessr.maps;

import at.daniel.geoGuessr.GeoGuessrMap;
import at.daniel.geoGuessr.Road;
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
        maps.add(new GeoGuessrMap(name, world.getUID(), start, end, new ArrayList<>(), new ArrayList<>()));
    }

    public boolean mapExists(String name) {
        return maps.stream().anyMatch(it -> it.getName().equals(name));
    }
}
