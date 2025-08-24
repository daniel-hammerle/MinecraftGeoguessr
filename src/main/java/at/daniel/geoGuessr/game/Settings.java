package at.daniel.geoGuessr.game;

import java.time.Duration;

public record Settings(boolean noMove, Duration clock, Duration afterLockIn, int winningPoints) {
}
