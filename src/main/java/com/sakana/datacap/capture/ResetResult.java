package com.sakana.datacap.capture;

public class ResetResult {
    private final int removedSampleCount;

    public ResetResult(int removedSampleCount) {
        this.removedSampleCount = removedSampleCount;
    }

    public int getRemovedSampleCount() {
        return removedSampleCount;
    }
}
