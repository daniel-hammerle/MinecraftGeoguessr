package at.daniel.geoGuessr;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum RoadKind {
    Tiny(Items.TinyItem),
    Small(Items.SmallItem),
    Normal(Items.NormalItem),
    Large(Items.LargeItem),
    ExtraLarge(Items.ExtraLargeItem);

    private final ItemStack item;

    RoadKind(ItemStack item) {
        this.item = item;
    }

    public ItemStack getItem() {
        return item;
    }

    static class Items {
        static ItemStack TinyItem = ItemStack.of(Material.RED_STAINED_GLASS_PANE);
        static ItemStack SmallItem = ItemStack.of(Material.ORANGE_STAINED_GLASS_PANE);
        static ItemStack NormalItem = ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE);
        static ItemStack LargeItem = ItemStack.of(Material.LIME_STAINED_GLASS_PANE);
        static ItemStack ExtraLargeItem = ItemStack.of(Material.GREEN_STAINED_GLASS_PANE);

        static {
            TinyItem.editMeta(it -> it.displayName(Component.text("Tiny")));
            SmallItem.editMeta(it -> it.displayName(Component.text("Small")));
            NormalItem.editMeta(it -> it.displayName(Component.text("Normal")));
            LargeItem.editMeta(it -> it.displayName(Component.text("Large")));
            ExtraLargeItem.editMeta(it -> it.displayName(Component.text("Extra Large")));
        }
    }
}
