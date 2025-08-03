package at.daniel.geoGuessr;

import at.daniel.flow.Flow;
import at.daniel.flow.VoidFlow;
import at.daniel.flow.util.PlayerUtil;
import at.daniel.geoGuessr.commands.GeoGuessrCommand;
import at.daniel.geoGuessr.commands.ProjectCommand;
import at.daniel.geoGuessr.commands.TravelCommand;
import at.daniel.geoGuessr.editor.CreateMapFlow;
import at.daniel.geoGuessr.editor.EditMapFlow;
import at.daniel.geoGuessr.editor.LocationObject;
import at.daniel.geoGuessr.maps.MapManager;
import at.daniel.geoGuessr.projection.ProjectionManager;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class GeoGuessr extends JavaPlugin {
    static final Path PluginPath = Path.of("plugins/geoguessr");
    static final Path MapsPath = PluginPath.resolve("maps");
    private final static Gson gson = new Gson();


    private final Flow.Application<GeoGuessr, LocationObject, Void> createMapFlow = Flow.create(
            this,
            (ctx, ignoreProps) -> PlayerUtil.preserveInventory(ctx, CreateMapFlow::createMapFlow)
    );

    private final Flow.Application<GeoGuessr, Void, GeoGuessrMap> editMapFlow = Flow.create(
            this,
            (ctx, map) ->
                PlayerUtil.preserveInventory(ctx,  c -> {
                    EditMapFlow.editMap(ctx, map);
                    return null;
                })
    );

    private MapManager mapManager;
    private ProjectionManager projectionManager;
    @Override
    public void onEnable() {
        // Plugin startup logic
        try {
            GeoGuessrMap[] maps = loadResources();
            mapManager = new MapManager(maps);
        } catch (IOException e) {
            panic("Failed to create or access necessary directories");
            return;
        }

        projectionManager = new ProjectionManager(this);

        PluginCommand command = getCommand("geoguessr");
        if (command == null) {
            panic("Could not find geoguessr command");
            return;
        }

        command.setExecutor(new GeoGuessrCommand(this));

        PluginCommand travelCommand = getCommand("travel");
        if (travelCommand == null) {
            panic("Could not find travel command");
            return;
        }

        travelCommand.setExecutor(new TravelCommand(this));

        PluginCommand projectCommand = getCommand("project");
        if (projectCommand == null) {
            panic("Could not find project command");
            return;
        }

        projectCommand.setExecutor(new ProjectCommand(this));
    }


    @Override
    public void onDisable() {
        try {
            storeResources();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to save");
        }

    }

    void panic(String message) {
        getLogger().log(Level.SEVERE, message);
        Bukkit.getPluginManager().disablePlugin(this);
    }

    GeoGuessrMap[] loadResources() throws IOException {
        //create plugin dir if it doesn't exist
        if (!Files.isDirectory(PluginPath)) {
            Files.createDirectory(PluginPath);
        }

        //create maps dir if it doesn't exist
        if (!Files.isDirectory(MapsPath)) {
            Files.createDirectory(MapsPath);
        }

        //parse all geoguessr maps
        try (Stream<Path> stream = Files.list(MapsPath)) {
            return stream
                    .filter(it -> it.getFileName().endsWith(".json"))
                    .map(it -> {
                        try {
                            return gson.fromJson(Files.readString(it), GeoGuessrMap.class);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toArray(GeoGuessrMap[]::new);
        }

    }

    void storeResources() throws IOException {
        for (GeoGuessrMap map : mapManager.getMaps()) {
            String json = gson.toJson(map);
            Files.writeString(MapsPath.resolve(map.getName()), json);
        }
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public Flow.Application<GeoGuessr, LocationObject, Void> getCreateMapFlow() {
        return createMapFlow;
    }

    public Flow.Application<GeoGuessr, Void, GeoGuessrMap> getEditMapFlow() {
        return editMapFlow;
    }

    public ProjectionManager getProjectionManager() {
        return projectionManager;
    }
}
