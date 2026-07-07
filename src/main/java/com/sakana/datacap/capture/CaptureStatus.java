package com.sakana.datacap.capture;

public class CaptureStatus {
    private final boolean recording;
    private final String playerName;
    private final int sampleCount;
    private final double durationSeconds;
    private final double totalDistance2d;
    private final double totalDistance3d;

    public CaptureStatus(
            boolean recording,
            String playerName,
            int sampleCount,
            double durationSeconds,
            double totalDistance2d,
            double totalDistance3d
    ) {
        this.recording = recording;
        this.playerName = playerName;
        this.sampleCount = sampleCount;
        this.durationSeconds = durationSeconds;
        this.totalDistance2d = totalDistance2d;
        this.totalDistance3d = totalDistance3d;
    }

    public boolean isRecording() {
        return recording;
    }

    public String getPlayerName() {
        return playerName;
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
