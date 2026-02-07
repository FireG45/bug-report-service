package ru.bre.admin.util;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "admin-app.properties";
    private static final String HOST_KEY = "last.host";
    private static final String SECRET_KEY = "secret";
    private static final String REPORT_HOST_KEY = "report.host";

    public static void saveConfig(String host, String secret, String reportHost) {
        Properties props = new Properties();
        props.setProperty(HOST_KEY, host);
        props.setProperty(SECRET_KEY, secret);
        props.setProperty(REPORT_HOST_KEY, reportHost);

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Admin App Configuration");
        } catch (IOException e) {
            // Игнорируем ошибки сохранения
            e.printStackTrace();
        }
    }

    public static String loadHost() {
        Properties props = new Properties();
        
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            return props.getProperty(HOST_KEY, "");
        } catch (IOException e) {
            return "";
        }
    }

    public static String loadSecret() {
        Properties props = new Properties();

        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            return props.getProperty(SECRET_KEY, "");
        } catch (IOException e) {
            return "";
        }
    }

    public static String loadReportHost() {
        Properties props = new Properties();

        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            return props.getProperty(REPORT_HOST_KEY, "");
        } catch (IOException e) {
            return "";
        }
    }
}
