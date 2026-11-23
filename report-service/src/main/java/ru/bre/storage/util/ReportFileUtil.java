package ru.bre.storage.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public final class ReportFileUtil {

    private ReportFileUtil() {
    }

    public static File copyToTempFile(MultipartFile file) throws IOException {
        File tempFile = Files.createTempFile("upload_", ".tmp").toFile();
        try (InputStream in = file.getInputStream();
             OutputStream out = Files.newOutputStream(tempFile.toPath())) {
            in.transferTo(out);
        } catch (IOException e) {
            Files.deleteIfExists(tempFile.toPath());
            throw e;
        }
        return tempFile;
    }
}
