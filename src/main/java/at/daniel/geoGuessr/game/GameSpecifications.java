package at.daniel.geoGuessr.game;

import at.daniel.geoGuessr.GeoGuessrMap;
import org.bukkit.entity.Player;

public record GameSpecifications(GeoGuessrMap map, Player[] players, Settings settings) {
}
