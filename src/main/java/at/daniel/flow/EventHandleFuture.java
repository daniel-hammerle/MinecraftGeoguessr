package at.daniel.flow;

import org.bukkit.event.Event;

import java.util.concurrent.CompletableFuture;

interface EventHandleFuture<E extends Event>{
    CompletableFuture<E> waitForEvent(State state);

    class State {
        private boolean active = true;

        public boolean doIfActive(Runnable task) {
            synchronized (this) {
                if (!active) return false;
                task.run();
                active = false;
            }
            return true;
        }

    }
}
