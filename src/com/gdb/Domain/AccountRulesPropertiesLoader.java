package com.gdb.Domain;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/** Loads one account rule properties file from the working tree or classpath. */
public final class AccountRulesPropertiesLoader {
    private static final String RESOURCE_DIRECTORY = "config/rules/";
    private final Properties properties = new Properties();

    public AccountRulesPropertiesLoader(String fileName) {
        String resourceName = RESOURCE_DIRECTORY + fileName;
        Path sourcePath = Paths.get("src", "main", "resources", "config", "rules", fileName);
        boolean loaded = false;

        try {
            if (Files.isRegularFile(sourcePath)) {
                try (InputStream input = Files.newInputStream(sourcePath)) {
                    properties.load(input);
                }
                loaded = true;
            } else {
                ClassLoader classLoader = AccountRulesPropertiesLoader.class.getClassLoader();
                try (InputStream input = classLoader.getResourceAsStream(resourceName)) {
                    if (input != null) {
                        properties.load(input);
                        loaded = true;
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load account rules from " + sourcePath, e);
        }

        if (!loaded) {
            throw new IllegalStateException("Account rules file not found: " + sourcePath
                    + " (or classpath resource " + resourceName + ")");
        }
        if ("savings.properties".equals(fileName)) {
            System.out.println("[Config] Loaded rules from " + sourcePath.toString().replace('\\', '/'));
        }
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public double getDouble(String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid decimal value for rule '" + key + "': " + value, e);
        }
    }
}
