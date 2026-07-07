package com.sakana.datacap.exit;

import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExitRegionManager {
    private final Map<String, ExitRegion> regions = new LinkedHashMap<String, ExitRegion>();

    public synchronized boolean add(ExitRegion region) {
        if (regions.containsKey(region.getId())) {
            return false;
        }

        regions.put(region.getId(), region);
        return true;
    }

    public synchronized boolean remove(String id) {
        return regions.remove(id) != null;
    }

    public synchronized ExitRegion get(String id) {
        return regions.get(id);
    }

    public synchronized Collection<ExitRegion> getAll() {
        return new ArrayList<ExitRegion>(regions.values());
    }

    public synchronized String findContainingRegionId(double x, double y, double z) {
        for (ExitRegion region : regions.values()) {
            if (region.contains(x, y, z)) {
                return region.getId();
            }
        }

        return null;
    }

    public synchronized JsonArray toJson() {
        JsonArray regionArray = new JsonArray();
        for (ExitRegion region : regions.values()) {
            regionArray.add(region.toJson());
        }
        return regionArray;
    }
}
