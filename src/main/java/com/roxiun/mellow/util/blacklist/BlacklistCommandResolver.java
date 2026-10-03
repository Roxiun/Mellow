package com.roxiun.mellow.util.blacklist;

import net.fabricmc.loader.api.FabricLoader;

public final class BlacklistCommandResolver {

    private static final String SERAPH_MOD_ID = "seraph";

    private BlacklistCommandResolver() {}

    public static String getBaseCommand() {
        return isSeraphLoaded() ? "mblacklist" : "blacklist";
    }

    public static String getCommandPrefix() {
        return "/" + getBaseCommand();
    }

    public static boolean isSeraphLoaded() {
        return FabricLoader.getInstance().isModLoaded(SERAPH_MOD_ID);
    }
}
