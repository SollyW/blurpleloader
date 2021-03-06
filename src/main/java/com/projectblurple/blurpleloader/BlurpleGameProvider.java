package com.projectblurple.blurpleloader;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class BlurpleGameProvider extends MinecraftGameProvider {
    @Override
    public boolean isEnabled() {
        return true;
    }

    @SuppressWarnings("resource")
    @Override
    public void initialize(FabricLauncher launcher) {
        Log.info(LogCategory.GAME_PROVIDER, "Running update check...");

        final List<Path> filesToDelete = new ArrayList<>();
        final Path modDir = getLaunchDirectory().resolve("mods");
        final Map<String, URL> manifest = new HashMap<>();

        try {
            String manifestString = new String(
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
            throw new UncheckedIOException(e);
        }

        try {
            if (!Files.exists(modDir)) Files.createDirectory(modDir);
            Files.list(modDir).forEach(path -> {
                String filename = path.getFileName().toString();
                if (!manifest.containsKey(filename)) {
                    Log.info(LogCategory.GAME_PROVIDER, "Marking " + filename + " for deletion");
                    filesToDelete.add(path);
                } else manifest.remove(filename);
            });
        } catch (IOException e) {
            Log.error(LogCategory.GAME_PROVIDER, "Error listing mods from mod dir: " + modDir);
            throw new UncheckedIOException(e);
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
                    throw new UncheckedIOException("Error downloading mod " + file, e);
                }
            }
        }

        Log.info(LogCategory.GAME_PROVIDER, "Done checking for mods to download. Deleting old mods...");

        for (Path path : filesToDelete) {
            Log.info(LogCategory.GAME_PROVIDER, "Deleting " + path);
            if (!path.toFile().delete()) {
                Log.error(LogCategory.GAME_PROVIDER, "Error deleting mod: " + path);
                throw new RuntimeException("Failed to delete file " + path);
            }
        }

        Log.info(LogCategory.GAME_PROVIDER, "Mod updates finished.");

        custom:
        {
            final Path customModDir = getLaunchDirectory().resolve("usermods");
            if (!Files.isDirectory(modDir)) break custom;

            File[] mods = customModDir.toFile().listFiles();
            if (mods == null) break custom;

            for (File mod : mods) {
                Log.info(LogCategory.GAME_PROVIDER, "Loading user-provided mod " + mod.getName());
            }

            StringBuilder builder = new StringBuilder();
            Arrays.stream(mods)
                    .forEach(file -> builder.append(File.pathSeparatorChar)
                            .append(file.getAbsolutePath()));

            builder.deleteCharAt(0);

            System.setProperty(SystemProperties.ADD_MODS, builder.toString());
        }

        super.initialize(launcher);
    }
}
