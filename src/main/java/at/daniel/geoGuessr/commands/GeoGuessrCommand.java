package at.daniel.geoGuessr.commands;

import at.daniel.flow.Flow;
import at.daniel.geoGuessr.GeoGuessr;
import at.daniel.geoGuessr.GeoGuessrMap;
import at.daniel.geoGuessr.editor.CreateMapFlow;
import at.daniel.geoGuessr.editor.LocationObject;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class GeoGuessrCommand implements CommandExecutor {
    private final GeoGuessr plugin;

    public GeoGuessrCommand(GeoGuessr plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        if (args.length < 2) {
            return false;
        }

        List<String> arguments = Arrays.asList(args).subList(1, args.length);
        return switch (args[0]) {
            case "new" -> createMap(player, arguments);
            case "edit" -> editMap(player, arguments);
            case "party" -> party(player, arguments);
            default -> false;
        };

    }


    boolean createMap(Player sender, List<String> args) {
        if (!sender.hasPermission("geoguessr.map.create")) {
            sender.sendMessage("§4You don't have permission!");
            return true;
        }

        if (args.size() != 1) {
            sender.sendMessage(args.toString());
            return false; //invalid usage
        }

        if (plugin.getMapManager().mapExists(args.getFirst())) {
            sender.sendMessage("Map already exists (use edit instead)");
            return true;
        }

        try {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> handleMapCreation(sender, args.getFirst()));
        } catch (IllegalStateException e) {
            sender.sendMessage("§4 An error occurred: " + e.getMessage());
        }
        return true;
    }

    void handleMapCreation(Player player, String name) {
        Future<LocationObject> coordinates = plugin.getCreateMapFlow().forPlayer(player, null);
        LocationObject boundaries;
        try {
            boundaries = coordinates.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        
        plugin.getMapManager().addMap(boundaries.world(), boundaries.first(), boundaries.second(), name);
        player.sendMessage("Successfully created `" + name + "`");
    }

    boolean editMap(Player sender, List<String> args) {
        if (!sender.hasPermission("geoguessr.map.edit")) {
            sender.sendMessage("§4You don't have permission!");
            return true;
        }

        if (args.size() != 1) {
            sender.sendMessage(args.toString());
            return false; //invalid usage
        }

        GeoGuessrMap map = plugin.getMapManager().getMap(args.getFirst());
        if (map == null) {
            sender.sendMessage("Map doesn't exist create it using new");
            return true;
        }

        try {
            plugin.getEditMapFlow().forPlayer(sender, map);
            return true;
        } catch (IllegalStateException e) {
            sender.sendMessage("§4 An error occurred: " + e.getMessage());
        }
        return false;
    }

    boolean party(Player sender, List<String> args) {
        return false;
    }
}
