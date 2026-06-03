package com.atomics.client.config;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class ConfigPaths {
    private ConfigPaths() {
    }

    public static Path atomicsClient() {
        return FMLPaths.CONFIGDIR.get().resolve("atomics_client.json");
    }
}
