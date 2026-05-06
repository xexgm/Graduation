package com.gm.graduation.netty.server;

import com.gm.graduation.common.utils.WssConfig;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * Builds the server-side SSL context used by the Netty WebSocket pipeline.
 */
public class NettySslContextFactory {

    public static SslContext buildServerSslContext() throws Exception {
        char[] password = WssConfig.keyStorePassword().toCharArray();
        KeyStore keyStore = KeyStore.getInstance(WssConfig.keyStoreType());

        try (InputStream inputStream = openKeyStore(WssConfig.keyStorePath())) {
            keyStore.load(inputStream, password);
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);

        return SslContextBuilder.forServer(keyManagerFactory)
            .protocols("TLSv1.3", "TLSv1.2")
            .build();
    }

    private static InputStream openKeyStore(String keyStorePath) throws Exception {
        if (keyStorePath.startsWith("classpath:")) {
            String resourcePath = keyStorePath.substring("classpath:".length());
            if (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }

            InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new IllegalArgumentException("KeyStore not found in classpath: " + resourcePath);
            }
            return inputStream;
        }

        return new FileInputStream(keyStorePath);
    }
}
