package com.projectblurple.blurpleloader;

import net.fabricmc.loader.impl.launch.knot.KnotClient;

public class Main {
    public static void main(String[] args) {
        System.setProperty("fabric.skipMcProvider", "");
        KnotClient.main(args);
    }
}