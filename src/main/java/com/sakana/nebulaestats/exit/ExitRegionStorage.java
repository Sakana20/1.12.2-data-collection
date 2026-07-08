package com.sakana.nebulaestats.exit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class ExitRegionStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY_NAME = "nebulaestats";
    private static final String FILE_NAME = "exit_regions.json";

    private final ExitRegionManager regions;

    public ExitRegionStorage(ExitRegionManager regions) {
        this.regions = regions;
    }

    public synchronized void load(MinecraftServer server) throws IOException {
        File file = getStorageFile(server);
        regions.clear();

        if (!file.exists()) {
            return;
        }

        InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        try {
            JsonElement root = new JsonParser().parse(reader);
            if (!root.isJsonArray()) {
                throw new IOException("Exit region file must contain a JSON array.");
            }

            JsonArray array = root.getAsJsonArray();
            for (JsonElement element : array) {
                if (element.isJsonObject()) {
                    regions.add(ExitRegion.fromJson(element.getAsJsonObject()));
                }
            }
        } finally {
            reader.close();
        }
    }

    public synchronized void save(MinecraftServer server) throws IOException {
        File file = getStorageFile(server);
        File directory = file.getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create statistics data directory: " + directory.getAbsolutePath());
        }

        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        try {
            GSON.toJson(regions.toJson(), writer);
        } finally {
            writer.close();
        }
    }

    public File getStorageFile(MinecraftServer server) {
        return new File(server.getFile(DIRECTORY_NAME), FILE_NAME);
    }
}

