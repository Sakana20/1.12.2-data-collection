package com.sakana.datacap.capture;

import com.sakana.datacap.DataCollectionMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CaptureRecorder {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<CaptureSample> samples = new ArrayList<CaptureSample>();

    private boolean recording;
    private UUID playerId;
    private String playerName;
    private long recordStartNanos;
    private int tick;
    private double previousX;
    private double previousY;
    private double previousZ;
    private double totalDistance2d;
    private double totalDistance3d;
    private boolean hasPreviousPosition;

    public synchronized boolean start(EntityPlayerMP player) {
        if (recording) {
            return false;
        }

        samples.clear();
        recording = true;
        playerId = player.getUniqueID();
        playerName = player.getName();
        recordStartNanos = System.nanoTime();
        tick = 0;
        previousX = 0.0D;
        previousY = 0.0D;
        previousZ = 0.0D;
        totalDistance2d = 0.0D;
        totalDistance3d = 0.0D;
        hasPreviousPosition = false;
        return true;
    }

    public synchronized StopResult stop(MinecraftServer server) throws IOException {
        if (!recording) {
            return StopResult.notRecording();
        }

        recording = false;
        File outputFile = writeCapture(server);
        return StopResult.saved(outputFile, samples.size(), getDurationSeconds(), totalDistance2d, totalDistance3d);
    }

    public synchronized ResetResult reset() {
        int removed = samples.size();
        recording = false;
        samples.clear();
        playerId = null;
        playerName = null;
        recordStartNanos = 0L;
        tick = 0;
        previousX = 0.0D;
        previousY = 0.0D;
        previousZ = 0.0D;
        totalDistance2d = 0.0D;
        totalDistance3d = 0.0D;
        hasPreviousPosition = false;
        return new ResetResult(removed);
    }

    public synchronized CaptureStatus getStatus() {
        return new CaptureStatus(recording, playerName, samples.size(), getDurationSeconds(), totalDistance2d, totalDistance3d);
    }

    public synchronized void sample(EntityPlayerMP player) {
        if (!recording || playerId == null || !player.getUniqueID().equals(playerId)) {
            return;
        }

        double x = player.posX;
        double y = player.posY;
        double z = player.posZ;
        double yaw = player.rotationYaw;
        double timeSeconds = (System.nanoTime() - recordStartNanos) / 1000000000.0D;

        double dx = 0.0D;
        double dy = 0.0D;
        double dz = 0.0D;
        double deltaDistance2d = 0.0D;
        double deltaDistance3d = 0.0D;

        if (hasPreviousPosition) {
            dx = x - previousX;
            dy = y - previousY;
            dz = z - previousZ;
            deltaDistance2d = Math.sqrt(dx * dx + dz * dz);
            deltaDistance3d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            totalDistance2d += deltaDistance2d;
            totalDistance3d += deltaDistance3d;
        }

        samples.add(new CaptureSample(
                tick,
                timeSeconds,
                x,
                y,
                z,
                yaw,
                dx,
                dy,
                dz,
                deltaDistance2d,
                deltaDistance3d,
                totalDistance2d,
                totalDistance3d
        ));

        previousX = x;
        previousY = y;
        previousZ = z;
        hasPreviousPosition = true;
        tick++;
    }

    private File writeCapture(MinecraftServer server) throws IOException {
        File captureDirectory = server.getFile("captures");
        if (!captureDirectory.exists() && !captureDirectory.mkdirs()) {
            throw new IOException("Could not create capture directory: " + captureDirectory.getAbsolutePath());
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File outputFile = new File(captureDirectory, timestamp + ".json");

        JsonObject root = new JsonObject();
        root.add("meta", createMeta());
        root.add("summary", createSummary());
        root.add("samples", createSamples());

        FileWriter writer = new FileWriter(outputFile);
        try {
            GSON.toJson(root, writer);
        } finally {
            writer.close();
        }

        if (DataCollectionMod.getLogger() != null) {
            DataCollectionMod.getLogger().info("Saved data capture to {}", outputFile.getAbsolutePath());
        }
        return outputFile;
    }

    private JsonObject createMeta() {
        JsonObject meta = new JsonObject();
        meta.addProperty("minecraftVersion", "1.12.2");
        meta.addProperty("modId", DataCollectionMod.MODID);
        meta.addProperty("modVersion", DataCollectionMod.VERSION);
        meta.addProperty("sampleRate", "1 tick");
        meta.addProperty("timeSource", "System.nanoTime");
        if (playerName != null) {
            meta.addProperty("playerName", playerName);
        }
        if (playerId != null) {
            meta.addProperty("playerUuid", playerId.toString());
        }
        return meta;
    }

    private JsonObject createSummary() {
        JsonObject summary = new JsonObject();
        summary.addProperty("sampleCount", samples.size());
        summary.addProperty("durationSeconds", getDurationSeconds());
        summary.addProperty("totalDistance2d", totalDistance2d);
        summary.addProperty("totalDistance3d", totalDistance3d);
        return summary;
    }

    private JsonArray createSamples() {
        JsonArray sampleArray = new JsonArray();
        for (CaptureSample sample : samples) {
            sampleArray.add(sample.toJson());
        }
        return sampleArray;
    }

    private double getDurationSeconds() {
        if (samples.isEmpty()) {
            return 0.0D;
        }

        return samples.get(samples.size() - 1).getTimeSeconds();
    }
}
