package com.projectblurple.blurpleloader;

import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public class BlurpleGameProvider extends MinecraftGameProvider {
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void initialize(FabricLauncher launcher) {
        Log.info(LogCategory.GAME_PROVIDER, "Blurple game provider loaded; updated");
        super.initialize(launcher);
    }
}
