package com.sakana.nebulaestats.selection;

import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {
    private final Map<UUID, Selection> selections = new HashMap<UUID, Selection>();

    public synchronized void setPos1(UUID playerId, BlockPos pos) {
        getOrCreateSelection(playerId).setPos1(pos);
    }

    public synchronized void setPos2(UUID playerId, BlockPos pos) {
        getOrCreateSelection(playerId).setPos2(pos);
    }

    public synchronized Selection getSelection(UUID playerId) {
        return selections.get(playerId);
    }

    private Selection getOrCreateSelection(UUID playerId) {
        Selection selection = selections.get(playerId);
        if (selection == null) {
            selection = new Selection();
            selections.put(playerId, selection);
        }
        return selection;
    }
}

