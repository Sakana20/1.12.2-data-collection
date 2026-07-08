package com.sakana.nebulaestats.capture;

import java.io.File;

public class StopResult {
    private final boolean recordingWasActive;
    private final File outputFile;
    private final int sampleCount;
    private final double durationSeconds;
    private final double totalDistance2d;
    private final double totalDistance3d;

    private StopResult(
            boolean recordingWasActive,
            File outputFile,
            int sampleCount,
            double durationSeconds,
            double totalDistance2d,
            double totalDistance3d
    ) {
        this.recordingWasActive = recordingWasActive;
        this.outputFile = outputFile;
        this.sampleCount = sampleCount;
        this.durationSeconds = durationSeconds;
        this.totalDistance2d = totalDistance2d;
        this.totalDistance3d = totalDistance3d;
    }

    public static StopResult notRecording() {
        return new StopResult(false, null, 0, 0.0D, 0.0D, 0.0D);
    }

    public static StopResult saved(
            File outputFile,
            int sampleCount,
            double durationSeconds,
            double totalDistance2d,
            double totalDistance3d
    ) {
        return new StopResult(true, outputFile, sampleCount, durationSeconds, totalDistance2d, totalDistance3d);
    }

    public boolean wasRecordingActive() {
        return recordingWasActive;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public double getTotalDistance2d() {
        return totalDistance2d;
    }

    public double getTotalDistance3d() {
        return totalDistance3d;
    }
}

