package ru.voidrp.claims.config;

import java.net.URI;

/**
 * Runtime configuration via JVM system properties. Reuses the auth-bridge's
 * game secret / backend by default so no extra systemd flags are needed:
 *   -Dvoidrp.auth.gameSecret=...  -Dvoidrp.auth.backend=https://api.void-rp.ru
 * Claims-specific overrides: -Dvoidrp.claims.backend / -Dvoidrp.claims.gameSecret.
 */
public final class ClaimsConfig {

    private final URI backendBaseUrl;
    private final String gameAuthSecret;
    private final String serverSlug;
    private final int maxLevel;
    private final String upgradeItemId;
    private final int upgradeItemsPerLevel;

    private ClaimsConfig(URI backendBaseUrl, String gameAuthSecret, String serverSlug,
                         int maxLevel, String upgradeItemId, int upgradeItemsPerLevel) {
        this.backendBaseUrl = backendBaseUrl;
        this.gameAuthSecret = gameAuthSecret;
        this.serverSlug = serverSlug;
        this.maxLevel = maxLevel;
        this.upgradeItemId = upgradeItemId;
        this.upgradeItemsPerLevel = upgradeItemsPerLevel;
    }

    public static ClaimsConfig load() {
        String backend = prop("voidrp.claims.backend", prop("voidrp.auth.backend", "https://api.void-rp.ru"));
        String secret = prop("voidrp.claims.gameSecret", prop("voidrp.auth.gameSecret", ""));
        String slug = prop("voidrp.claims.serverSlug", "");
        int maxLevel = intProp("voidrp.claims.maxLevel", 31);
        String upgradeItem = prop("voidrp.claims.upgradeItem", "minecraft:diamond_block");
        int perLevel = intProp("voidrp.claims.upgradeItemsPerLevel", 1);
        return new ClaimsConfig(URI.create(backend), secret, slug, maxLevel, upgradeItem, perLevel);
    }

    private static String prop(String key, String def) {
        String v = System.getProperty(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    private static int intProp(String key, int def) {
        try {
            String v = System.getProperty(key);
            return (v == null || v.isBlank()) ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    public URI backendBaseUrl() {
        return backendBaseUrl;
    }

    public String gameAuthSecret() {
        return gameAuthSecret;
    }

    public String serverSlug() {
        return serverSlug;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public String upgradeItemId() {
        return upgradeItemId;
    }

    public int upgradeItemsPerLevel() {
        return upgradeItemsPerLevel;
    }
}
