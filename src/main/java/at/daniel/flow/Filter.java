package at.daniel.flow;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.Inventory;

/**
 * A filter to determine whether an event should be handled.
 *
 * @param <E> the event type
 */
public interface Filter<E extends Event> {

    /**
     * Returns true if the event should be accepted.
     *
     * @param element the event to check
     * @return true if accepted
     */
    boolean validate(E element);

    /**
     * Accepts all events.
     */
    static <E extends Event> Filter<E> allow() {
        return _ -> true;
    }

    /**
     * Rejects all events.
     */
    static <E extends Event> Filter<E> deny() {
        return _ -> false;
    }

    /**
     * Accepts only events that belong to the specified player.
     */
    static <E extends Event> Filter<E> isPlayer(Player player) {
        return it -> switch (it) {
            case PlayerEvent e -> e.getPlayer().equals(player);
            case InventoryCloseEvent e -> e.getPlayer().equals(player);
            case InventoryClickEvent e -> e.getWhoClicked().equals(player);
            default -> throw new RuntimeException("Unexpected event for filter");
        };
    }

    /**
     * Accepts only events from a specific inventory.
     */
    static <E extends Event> Filter<E> isInventory(Inventory inventory) {
        return it -> switch (it) {
            case InventoryEvent e -> e.getInventory().equals(inventory);
            default -> throw new RuntimeException("Unexpected event for filter");
        };
    }

    /**
     * Accepts only if all filters match.
     */
    @SafeVarargs
    static <R extends Event> Filter<? extends R> all(Filter<R>... items) {
        return it -> {
            for (Filter<R> item : items) {
                if (!item.validate(it)) return false;
            }
            return true;
        };
    }

    /**
     * Accepts if any of the filters match.
     */
    @SafeVarargs
    static <R extends Event> Filter<? extends R> any(Filter<R>... items) {
        return it -> {
            for (Filter<R> item : items) {
                if (item.validate(it)) return true;
            }
            return false;
        };
    }
}
