package com.projectblurple.blurpleloader;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlurpleGameProvider extends MinecraftGameProvider {
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void initialize(FabricLauncher launcher) {
        Log.info(LogCategory.GAME_PROVIDER, "Running update check...");

        final List<Path> filesToDelete = new ArrayList<>();
        final Path modDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        final Map<String, URL> manifest = new HashMap<>();

        try {
            var manifestString = new String(
                    new URL("https://raw.githubusercontent.com/SollyW/blurplepack/master/mod_manifest")
                            .openStream()
                            .readAllBytes());

            for (String line : manifestString.split("\n")) {
                String[] words = line.split("; ", 2);
                if (words.length < 2) continue;
                manifest.put(words[0], new URL(words[1]));
            }
        } catch (IOException e) {
            Log.error(LogCategory.GAME_PROVIDER, "Error fetching manifest file");
            e.printStackTrace();
        }

        try {
            Files.list(modDir).forEach(path -> {
                String filename = path.getFileName().toString();
                if (!manifest.containsKey(filename)) {
                    Log.info(LogCategory.GAME_PROVIDER, "Deleting " + filename);
                    filesToDelete.add(path);
                } else manifest.remove(filename);
            });
        } catch (IOException e) {
            Log.error(LogCategory.GAME_PROVIDER, "Error listing mods from mod dir: " + modDir);
            e.printStackTrace();
        }

        Log.info(LogCategory.GAME_PROVIDER, "Done checking files. Checking for mods to download...");

        if (!manifest.isEmpty()) {
            for (Map.Entry<String, URL> entry : manifest.entrySet()) {
                Log.info(LogCategory.GAME_PROVIDER, "Downloading " + entry.getKey() + " from " + entry.getValue());

                File file = modDir.resolve(entry.getKey()).toFile();
                try {
                    if (file.createNewFile()) {
                        ReadableByteChannel in = Channels.newChannel(entry.getValue().openStream());
                        FileOutputStream out = new FileOutputStream(file);
                        out.getChannel()
                                .transferFrom(in, 0, Long.MAX_VALUE);
                    }
                } catch (IOException e) {
                    Log.error(LogCategory.GAME_PROVIDER, "Error downloading mod: " + file);
                    e.printStackTrace();
                }
            }
        }

        Log.info(LogCategory.GAME_PROVIDER, "Done checking for mods to download. Deleting old mods...");

        for (Path path : filesToDelete) {
            Log.info(LogCategory.GAME_PROVIDER, path.toFile().delete() + "");
        }

        Log.info(LogCategory.GAME_PROVIDER, "Mod updates finished.");

        super.initialize(launcher);
    }
}
