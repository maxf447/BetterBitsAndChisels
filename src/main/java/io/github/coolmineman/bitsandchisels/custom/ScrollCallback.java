package io.github.coolmineman.bitsandchisels.custom;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;

// Callback for mouse scroll handling
public interface ScrollCallback {
    Event<ScrollCallback> EVENT = EventFactory.createArrayBacked(ScrollCallback.class,
        (listeners) -> (yOffset) -> {
            for (ScrollCallback listener : listeners) {
                ActionResult result = listener.interact(yOffset);
                if(result != ActionResult.PASS) return result;
            }
            return ActionResult.PASS;
        });

    ActionResult interact(double yOffset);
}