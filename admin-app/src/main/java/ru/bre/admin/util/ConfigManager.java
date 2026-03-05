package ru.bre.admin.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_DIR = "BugReportAdmin";
    private static final String CONFIG_FILE = "admin-app.properties";
    private static final String HOST_KEY = "last.host";
    private static final String SECRET_KEY = "secret";
    private static final String REPORT_HOST_KEY = "report.host";

    private static Path getConfigPath() {
        String documents = System.getProperty("user.home") + File.separator + "Documents";
        Path dir = Path.of(documents, CONFIG_DIR);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return dir.resolve(CONFIG_FILE);
    }

    public static void saveConfig(String host, String secret, String reportHost) {
        Properties props = new Properties();
        props.setProperty(HOST_KEY, host);
        props.setProperty(SECRET_KEY, secret);
        props.setProperty(REPORT_HOST_KEY, reportHost);

        try (OutputStream out = Files.newOutputStream(getConfigPath())) {
            props.store(out, "Admin App Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String loadHost() {
        return loadProperty(HOST_KEY);
    }

    public static String loadSecret() {
        return loadProperty(SECRET_KEY);
    }

    public static String loadReportHost() {
        return loadProperty(REPORT_HOST_KEY);
    }

    private static String loadProperty(String key) {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            return "";
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
            return props.getProperty(key, "");
        } catch (IOException e) {
            return "";
        }
    }
}
