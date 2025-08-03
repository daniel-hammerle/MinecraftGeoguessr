package at.daniel.geoGuessr.commands;

import at.daniel.geoGuessr.GeoGuessr;
import at.daniel.geoGuessr.GeoGuessrMap;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ProjectCommand implements CommandExecutor {

    private final GeoGuessr plugin;

    public ProjectCommand(GeoGuessr plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        GeoGuessrMap map = plugin.getMapManager().getMaps().stream().filter(it -> it.getName().equals(args[0])).findFirst().orElse(null);

        if (map == null) {
            return false;
        }

        Bukkit.getScheduler().runTask(plugin, () -> plugin.getProjectionManager().getProjection(map, Integer.parseInt(args[1])));
        return true;
    }
}
