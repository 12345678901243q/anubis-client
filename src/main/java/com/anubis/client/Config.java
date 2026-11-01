package com.anubis.client;

import com.anubis.client.module.Module;
import com.anubis.client.module.ModuleManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static boolean loading = false;
    /** The player's own FOV before FOV Changer touched it (-1 = unknown). */
    public static int origFov = -1;

    private static class Data {
        Map<String, Boolean> enabled = new HashMap<>();
        Map<String, Double> values = new HashMap<>();
        int origFov = -1;
    }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("anubis.json"); }

    public static void load() {
        loading = true;
        try {
            Path f = file();
            if (Files.exists(f)) {
                Data d = GSON.fromJson(Files.readString(f), Data.class);
                if (d != null) {
                    origFov = d.origFov;
                    for (Module m : ModuleManager.ALL) {
                        Double v = d.values.get(m.name);
                        if (v != null && m.hasSlider) m.value = v;
                    }
                    for (Module m : ModuleManager.ALL) {
                        if (Boolean.TRUE.equals(d.enabled.get(m.name))) m.setEnabled(true);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        loading = false;
    }

    public static void save() {
        if (loading) return;
        try {
            Data d = new Data();
            d.origFov = origFov;
            for (Module m : ModuleManager.ALL) {
                d.enabled.put(m.name, m.enabled);
                if (m.hasSlider) d.values.put(m.name, m.value);
            }
            Files.writeString(file(), GSON.toJson(d));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
