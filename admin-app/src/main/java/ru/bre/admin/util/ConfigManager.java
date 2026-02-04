package ru.bre.admin.util;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "admin-app.properties";
    private static final String HOST_KEY = "last.host";

    public static void saveHost(String host) {
        Properties props = new Properties();
        props.setProperty(HOST_KEY, host);
        
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
            // Если файл не существует, возвращаем пустую строку
            return "";
        }
    }
}
