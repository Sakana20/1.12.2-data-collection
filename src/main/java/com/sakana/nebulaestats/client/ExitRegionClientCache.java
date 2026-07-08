package com.sakana.nebulaestats.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ExitRegionClientCache {
    private static final List<ClientExitRegion> REGIONS = new ArrayList<ClientExitRegion>();
    private static BlockPos pos1;
    private static BlockPos pos2;

    private ExitRegionClientCache() {
    }

    public static synchronized void setSelection(BlockPos newPos1, BlockPos newPos2) {
        pos1 = newPos1;
        pos2 = newPos2;
    }

    public static synchronized void setRegionsFromJson(String json) {
        REGIONS.clear();
        JsonElement root = new JsonParser().parse(json == null ? "[]" : json);
        if (!root.isJsonArray()) {
            return;
        }

        JsonArray array = root.getAsJsonArray();
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                REGIONS.add(readRegion(element.getAsJsonObject()));
            }
        }
    }

    public static synchronized List<ClientExitRegion> getRegions() {
        return Collections.unmodifiableList(new ArrayList<ClientExitRegion>(REGIONS));
    }

    public static synchronized BlockPos getPos1() {
        return pos1;
    }

    public static synchronized BlockPos getPos2() {
        return pos2;
    }

    public static synchronized boolean hasCompleteSelection() {
        return pos1 != null && pos2 != null;
    }

    private static ClientExitRegion readRegion(JsonObject object) {
        return new ClientExitRegion(
                object.get("id").getAsString(),
                readPosition(object.getAsJsonObject("min")),
                readPosition(object.getAsJsonObject("max"))
        );
    }

    private static BlockPos readPosition(JsonObject object) {
        return new BlockPos(
                object.get("x").getAsInt(),
                object.get("y").getAsInt(),
                object.get("z").getAsInt()
        );
    }

    public static final class ClientExitRegion {
        private final String id;
        private final BlockPos min;
        private final BlockPos max;

        private ClientExitRegion(String id, BlockPos min, BlockPos max) {
            this.id = id;
            this.min = min;
            this.max = max;
        }

        public String getId() {
            return id;
        }

        public BlockPos getMin() {
            return min;
        }

        public BlockPos getMax() {
            return max;
        }
    }
}

