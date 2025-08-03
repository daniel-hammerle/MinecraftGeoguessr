package at.daniel.geoGuessr.editor;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum ItemType {
    Road(Items.RoadItem),
    Capture(Items.CaptureItem);

    public ItemStack getItem() {
        return item;
    }

    private static class Items {
        private static final ItemStack RoadItem = ItemStack.of(Material.GRAY_CONCRETE);
        private static final ItemStack CaptureItem = ItemStack.of(Material.FLOWER_POT);

        static {
            RoadItem.editMeta(it -> it.displayName(Component.text("Road")));
            CaptureItem.editMeta(it -> it.displayName(Component.text("Capture")));
        }
    }

    private final ItemStack item;

    ItemType(ItemStack item) {
        this.item = item;
    }

}
