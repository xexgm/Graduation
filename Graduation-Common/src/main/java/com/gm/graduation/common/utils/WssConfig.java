package com.gm.graduation.common.utils;

/**
 * WSS configuration for the Netty WebSocket server.
 */
public class WssConfig {

    private static final String DEFAULT_KEY_STORE = "classpath:certs/local-wss.p12";
    private static final String DEFAULT_KEY_STORE_PASSWORD = "changeit";
    private static final String DEFAULT_KEY_STORE_TYPE = "PKCS12";

    public static boolean enabled() {
        return Boolean.parseBoolean(getValue("NETTY_SSL_ENABLED", "netty.ssl.enabled", "false"));
    }

    public static String keyStorePath() {
        return getValue("NETTY_SSL_KEY_STORE", "netty.ssl.key-store", DEFAULT_KEY_STORE);
    }

    public static String keyStorePassword() {
        return getValue("NETTY_SSL_KEY_STORE_PASSWORD", "netty.ssl.key-store-password", DEFAULT_KEY_STORE_PASSWORD);
    }

    public static String keyStoreType() {
        return getValue("NETTY_SSL_KEY_STORE_TYPE", "netty.ssl.key-store-type", DEFAULT_KEY_STORE_TYPE);
    }

    private static String getValue(String envKey, String propertyKey, String defaultValue) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return defaultValue;
    }
}
