package com.monikoshop.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Makes the datasource URL tolerant of the connection strings that hosting panels
 * (Runflare, Railway, Heroku, ...) hand out, e.g.
 *   postgresql://user:pass@host:5432/dbname
 * which is not a valid JDBC URL. It is converted to
 *   jdbc:postgresql://host:5432/dbname
 * and the embedded credentials are used unless
 * SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD are set explicitly.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        String url = env.getProperty("spring.datasource.url");
        if (url == null) {
            return;
        }
        url = url.trim();
        if (url.startsWith("jdbc:")) {
            return;
        }

        String normalized = url;
        if (normalized.startsWith("postgres://")) {
            normalized = "postgresql://" + normalized.substring("postgres://".length());
        }
        if (!normalized.startsWith("postgresql://")) {
            return;
        }

        try {
            URI uri = URI.create(normalized);
            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
            if (uri.getPort() != -1) {
                jdbc.append(':').append(uri.getPort());
            }
            jdbc.append(uri.getRawPath() == null ? "" : uri.getRawPath());
            if (uri.getRawQuery() != null) {
                jdbc.append('?').append(uri.getRawQuery());
            }

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbc.toString());

            String userInfo = uri.getRawUserInfo();
            if (userInfo != null && !userInfo.isEmpty()) {
                int idx = userInfo.indexOf(':');
                String user = idx >= 0 ? userInfo.substring(0, idx) : userInfo;
                String pass = idx >= 0 ? userInfo.substring(idx + 1) : null;
                if (isBlank(env.getProperty("SPRING_DATASOURCE_USERNAME"))) {
                    props.put("spring.datasource.username", decode(user));
                }
                if (pass != null && isBlank(env.getProperty("SPRING_DATASOURCE_PASSWORD"))) {
                    props.put("spring.datasource.password", decode(pass));
                }
            }
            env.getPropertySources().addFirst(new MapPropertySource("normalizedDatabaseUrl", props));
        } catch (IllegalArgumentException ignored) {
            // leave the original value untouched; Spring will report the real error
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        // after ConfigDataEnvironmentPostProcessor so application.yml / env vars are already loaded
        return Ordered.LOWEST_PRECEDENCE;
    }
}
