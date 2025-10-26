package ru.bre.storage.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ReportFileUtil {
    public static File convertMultipartFileToFile(MultipartFile file) {
        try {
            File convFile = File.createTempFile("temp_" + file.getName(), file.getOriginalFilename().split("\\.")[1]);
            FileOutputStream fos = new FileOutputStream(convFile);
            fos.write(file.getBytes());
            fos.close();
            return convFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
