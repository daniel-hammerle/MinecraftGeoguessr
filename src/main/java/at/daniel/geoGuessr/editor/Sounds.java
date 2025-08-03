package at.daniel.geoGuessr.editor;

import at.daniel.flow.Ctx;
import org.bukkit.Sound;

public class Sounds {
    public static void click(Ctx<?> ctx) {
        ctx.nonBlocking(() -> ctx.player().playSound(ctx.player().getLocation(), Sound.UI_BUTTON_CLICK, 1, 1));
    }

    public static void play(Ctx<?> ctx, Sound sound) {
        ctx.nonBlocking(() -> ctx.player().playSound(ctx.player().getLocation(), sound, 1, 1));
    }
}
