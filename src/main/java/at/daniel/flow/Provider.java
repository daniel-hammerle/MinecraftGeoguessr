package at.daniel.flow;

import org.jetbrains.annotations.NotNull;

public interface Provider<T> {
    @NotNull T provide();
}
