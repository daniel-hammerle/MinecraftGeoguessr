package at.daniel.geoGuessr.projection;

import at.daniel.flow.RequiresSync;
import at.daniel.geoGuessr.GeoGuessr;
import at.daniel.geoGuessr.GeoGuessrMap;
import at.daniel.geoGuessr.Vec2D;
import org.bukkit.*;
import org.bukkit.block.Block;
import java.util.*;


public class ProjectionManager {

    static final int MAP_HEIGHT = 20;

    record ProjectionMap(Projection projection, int playerCount) {}

    private final Map<GeoGuessrMap, ProjectionMap> projections = new HashMap<>();
    private final GeoGuessr plugin;

    public ProjectionManager(GeoGuessr plugin) {
        this.plugin = plugin;
    }

    @RequiresSync
    public Projection getProjection(GeoGuessrMap map, int playerCount) {
        if (projections.containsKey(map)) {
            ProjectionMap projection = projections.get(map);

            if (projection.playerCount >= playerCount) {
                return projection.projection;
            }

            //copy the projection along for each player we don't have yet
            extendPlayers(
                    projection.projection.world(),
                    projection.playerCount,
                    playerCount,
                    projection.projection.lenX(),
                    projection.projection.lenY(),
                    projection.projection.mapHeight()
            );

            //create a new object for that and replace it
            ProjectionMap newMap = new ProjectionMap(projection.projection, playerCount);
            projections.put(map, newMap);

            return newMap.projection;
        }

        World origin = Bukkit.getWorld(map.getWorldId());
        if (origin == null) {
            throw new IllegalArgumentException("World of the map does not exist!");
        }

        Vec2D first = new Vec2D(Math.min(map.getStart().x(), map.getEnd().x()), Math.min(map.getStart().y(), map.getEnd().y()));
        Vec2D second  = new Vec2D(Math.max(map.getStart().x(), map.getEnd().x()), Math.max(map.getStart().y(), map.getEnd().y()));

        ProjectionState projectionState = project(origin, first, second);

        //create the world we actually want to place the projection in
        WorldCreator creator = new WorldCreator(map.getName() + UUID.randomUUID());
        creator.generator(new EmptyWorldGenerator());

        World projectionWorld = creator.createWorld();
        assert projectionWorld != null;
        projectionWorld.setAutoSave(false);
        writeProjection(projectionWorld, projectionState);

        //copy it for the right amount of players:
        int lenX =second.x() - first.x();
        int lenY = second.y() - first.y();
        extendPlayers(
                projectionWorld,
                1,
                playerCount,
                lenX,
                lenY,
                projectionState.projection.length * MAP_HEIGHT
        );


        Projection projection = new Projection(
                projectionWorld,
                projectionState.scales,
                MAP_HEIGHT * projectionState.scales.length,
                lenX,
                lenY
        );
        ProjectionMap projectionMap = new ProjectionMap(projection, playerCount);

        projections.put(map, projectionMap);

        return projection;
    }

    void extendPlayers(World target, int currentPlayers, int targetPlayers, int lenX, int lenY, int height) {
        int numTargets = targetPlayers - currentPlayers;
        int currentTarget = currentPlayers * (lenX + 1);

        int[] offsets = new int[numTargets];

        for (int i = 0; i < numTargets; ++i) {
            offsets[i] = currentTarget;
            currentTarget += (lenX + 1);
        }

        for (int x = -1; x <= lenX; x++) for (int y = -1; y <= lenY; y++) for (int h = 0; h < height; h++) {
            Block block = target.getBlockAt(x, h, y);
            for (int offset : offsets) {
                target.setBlockData(x + offset, h, y, block.getBlockData());
            }
        }
    }

    void writeProjection(World target, ProjectionState projection) {
        int height = 0;
        for (Material[][] grid : projection.projection) {
            int lenX = grid.length;
            int lenY = grid[0].length;

            for (int x = 0; x < lenX; x++) for (int y = 0; y < lenY; y++) {
                target.getBlockAt(x, height, y).setType(grid[x][y]);
                target.getBlockAt(x, height-1, y).setType(Material.BLACK_CONCRETE);
                //System.out.println("set block " + x + ", " + height + ", " + "to be " + grid[x][y]);
            }

            //create a bedrock grid around it

            for (int x = -1; x < (lenX + 1); x++) {
                for (int h = height; h < height + MAP_HEIGHT; h++) {
                    target.getBlockAt(x, h, -1).setType(Material.BLACK_CONCRETE);
                    target.getBlockAt(x, h, lenY).setType(Material.BLACK_CONCRETE);
                }
            }

            for (int y = 0; y < lenY; y++) {
                for (int h = height; h < height + MAP_HEIGHT; h++) {
                    target.getBlockAt(-1, h, y).setType(Material.BLACK_CONCRETE);
                    target.getBlockAt(lenX, h, y).setType(Material.BLACK_CONCRETE);
                }
            }

            height += MAP_HEIGHT;
        }
    }

    record ProjectionState(int[] scales, Material[][][] projection) {}

    ProjectionState project(World origin, Vec2D first, Vec2D second) {
        int[] zoomLevels = findDegrees(first, second);

        int distX = second.x() - first.x();
        int distY = second.y() - first.y();
        Material[][][] projections = Arrays.stream(zoomLevels)
                .mapToObj(zoomLevel -> calculateProjection(first, Math.floorDiv(distX, zoomLevel), Math.floorDiv(distY, zoomLevel), zoomLevel, origin))
                .toArray(Material[][][]::new);

        return new ProjectionState(zoomLevels, projections);
    }

    Material[][] calculateProjection(Vec2D start, int pixelsX, int pixelsY, int pixelWidth, World world) {
        Material[][] projection = new Material[pixelsX][pixelsY];
        int dy = start.y();
        for(int i = 0; i < pixelsY; ++i) {
            int dx = start.x();
            for (int j = 0; j < pixelsX; j++) {
                projection[j][i] = pixelDensity(dx, dy, pixelWidth, world);
                dx+=pixelWidth;
            }
            dy+=pixelWidth;
        }

        return projection;
    }

    Material pixelDensity(int x, int y, int pixelWidth, World world) {
        Map<Material, Integer> densityMap = new HashMap<>();

        for (int dx = 0; dx < pixelWidth; dx++) for(int dy = 0; dy < pixelWidth; dy++) {
            Material type = world.getHighestBlockAt(x + dx, y + dy).getType();
            densityMap.merge(type, 1, Integer::sum);
        }

        return densityMap.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(Material.WHITE_WOOL);
    }

    int[] findDegrees(Vec2D first, Vec2D second) {
        int dx = second.x() - first.x();
        int dy = second.y() - first.y();

        List<Integer> zoomLevels = new ArrayList<>(10);
        zoomLevels.add(1);

        for(int scale = 2; (dx / (scale- 1)) >= 40 && (dy / (scale-1)) >= 40; scale*=2) {
            zoomLevels.add(scale);
        }

        return zoomLevels.stream().mapToInt(Integer::intValue).toArray();
    }
}
