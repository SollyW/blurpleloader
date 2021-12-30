package com.projectblurple.blurpleloader;

import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public class BlurpleGameProvider extends MinecraftGameProvider {
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void launch(ClassLoader loader) {
        Log.info(LogCategory.GAME_PROVIDER, "Blurple game provider loaded");
        super.launch(loader);
    }
}
