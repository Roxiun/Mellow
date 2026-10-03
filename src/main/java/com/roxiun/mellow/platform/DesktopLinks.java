package com.roxiun.mellow.platform;
public final class DesktopLinks {
    public static void open(String url) {
        try { java.awt.Desktop.getDesktop().browse(java.net.URI.create(url)); }
        catch (Exception e) { org.apache.logging.log4j.LogManager.getLogger("Mellow").warn("Could not open link", e); }
    }
}
