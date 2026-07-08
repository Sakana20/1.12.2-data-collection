package com.sakana.nebulaestats.exit;

import com.google.gson.JsonObject;
import net.minecraft.util.math.BlockPos;

public class ExitRegion {
    private final String id;
    private final BlockPos min;
    private final BlockPos max;

    public ExitRegion(String id, BlockPos pos1, BlockPos pos2) {
        this.id = id;
        this.min = new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
        );
        this.max = new BlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
        );
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

    public boolean contains(double x, double y, double z) {
        return x >= min.getX() && x <= max.getX() + 1.0D
                && y >= min.getY() && y <= max.getY() + 1.0D
                && z >= min.getZ() && z <= max.getZ() + 1.0D;
    }

    public JsonObject toJson() {
        JsonObject region = new JsonObject();
        region.addProperty("id", id);
        region.add("min", createPosition(min));
        region.add("max", createPosition(max));
        return region;
    }

    public static ExitRegion fromJson(JsonObject region) {
        String id = region.get("id").getAsString();
        BlockPos min = readPosition(region.getAsJsonObject("min"));
        BlockPos max = readPosition(region.getAsJsonObject("max"));
        return new ExitRegion(id, min, max);
    }

    private JsonObject createPosition(BlockPos pos) {
        JsonObject position = new JsonObject();
        position.addProperty("x", pos.getX());
        position.addProperty("y", pos.getY());
        position.addProperty("z", pos.getZ());
        return position;
    }

    private static BlockPos readPosition(JsonObject position) {
        return new BlockPos(
                position.get("x").getAsInt(),
                position.get("y").getAsInt(),
                position.get("z").getAsInt()
        );
    }
}

