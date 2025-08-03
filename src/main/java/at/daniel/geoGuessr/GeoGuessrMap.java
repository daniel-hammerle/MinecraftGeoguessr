package at.daniel.geoGuessr;



import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class GeoGuessrMap {
    private String name;
    private UUID worldId;
    private Vec2D start;
    private Vec2D end;

    private List<Road> roads;
    private List<Capture> captures;

    public GeoGuessrMap(String name, UUID worldId, Vec2D start, Vec2D end, List<Road> roads, List<Capture> captures) {
        this.name = name;
        this.worldId = worldId;
        this.start = start;
        this.end = end;
        this.roads = roads;
        this.captures = captures;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

    public Vec2D getStart() {
        return start;
    }

    public void setStart(Vec2D start) {
        this.start = start;
    }

    public Vec2D getEnd() {
        return end;
    }

    public void setEnd(Vec2D end) {
        this.end = end;
    }

    public List<Road> getRoads() {
        return roads;
    }


    public List<Capture> getCaptures() {
        return captures;
    }

    public void setCaptures(List<Capture> captures) {
        this.captures = captures;
    }

    public Vector randomPoint(Random random) {
        double combinedLength = roads.stream().map(Road::totalLength).reduce(Double::sum).orElse(0.0);
        double rand = random.nextDouble() * combinedLength;
        double acc = 0;
        for (Road road : roads) {
            acc+=road.totalLength();
            if (rand < acc) {
                return road.randomPoint(random);
            }
        }

        throw new IllegalStateException("This should not be reachable");
    }
}
