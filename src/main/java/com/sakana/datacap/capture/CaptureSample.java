package com.sakana.datacap.capture;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class CaptureSample {
    private final int tick;
    private final double timeSeconds;
    private final double x;
    private final double y;
    private final double z;
    private final double yaw;
    private final double dx;
    private final double dy;
    private final double dz;
    private final double distance2d;
    private final double distance3d;
    private final double totalDistance2d;
    private final double totalDistance3d;
    private final String exitRegionId;

    public CaptureSample(
            int tick,
            double timeSeconds,
            double x,
            double y,
            double z,
            double yaw,
            double dx,
            double dy,
            double dz,
            double distance2d,
            double distance3d,
            double totalDistance2d,
            double totalDistance3d,
            String exitRegionId
    ) {
        this.tick = tick;
        this.timeSeconds = timeSeconds;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.distance2d = distance2d;
        this.distance3d = distance3d;
        this.totalDistance2d = totalDistance2d;
        this.totalDistance3d = totalDistance3d;
        this.exitRegionId = exitRegionId;
    }

    public double getTimeSeconds() {
        return timeSeconds;
    }

    public JsonObject toJson() {
        JsonObject sample = new JsonObject();
        sample.addProperty("tick", tick);
        sample.addProperty("timeSeconds", timeSeconds);
        sample.add("position", createPosition());
        sample.addProperty("yaw", yaw);
        if (exitRegionId == null) {
            sample.add("exitRegionId", JsonNull.INSTANCE);
        } else {
            sample.addProperty("exitRegionId", exitRegionId);
        }
        sample.add("delta", createDelta());
        sample.add("odometer", createOdometer());
        return sample;
    }

    private JsonObject createPosition() {
        JsonObject position = new JsonObject();
        position.addProperty("x", x);
        position.addProperty("y", y);
        position.addProperty("z", z);
        return position;
    }

    private JsonObject createDelta() {
        JsonObject delta = new JsonObject();
        delta.addProperty("dx", dx);
        delta.addProperty("dy", dy);
        delta.addProperty("dz", dz);
        delta.addProperty("distance2d", distance2d);
        delta.addProperty("distance3d", distance3d);
        return delta;
    }

    private JsonObject createOdometer() {
        JsonObject odometer = new JsonObject();
        odometer.addProperty("distance2d", totalDistance2d);
        odometer.addProperty("distance3d", totalDistance3d);
        return odometer;
    }
}
