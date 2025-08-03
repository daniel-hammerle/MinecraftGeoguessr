package at.daniel.geoGuessr.editor;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Item {
    public static final ItemStack CreateItem = ItemStack.of(Material.STICK);
    public static final ItemStack SelectionStick = new ItemStack(Material.STICK);
    public static final ItemStack ConfirmationPane = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
    public static final ItemStack MapIcon = new ItemStack(Material.COMPASS);
    public static final ItemStack ExitItem = new ItemStack(Material.BARRIER);
    public static final ItemStack Nothing = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    public static final ItemStack Undo = new ItemStack(Material.SUNFLOWER);
    static {
        MapIcon.editMeta(it -> it.displayName(Component.text("Map")));
        Nothing.editMeta(it -> it.displayName(Component.text("")));
        CreateItem.editMeta(it -> it.displayName(Component.text("New")));
        SelectionStick.editMeta(it -> it.displayName(Component.text("Selection")));
        ConfirmationPane.editMeta(it -> it.displayName(Component.text("Confirm")));
        ExitItem.editMeta(it -> it.displayName(Component.text("Exit")));
        Undo.editMeta(it -> it.displayName(Component.text("Undo")));
    }
}
