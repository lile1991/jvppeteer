package com.ruiyun.jvppeteer.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Objects;

public class FileUtils {

    private static File requireFile(File file, String name) {
        Objects.requireNonNull(file, name);
        if (!file.isFile()) {
            throw new IllegalArgumentException("Parameter '" + name + "' is not a file: " + file);
        } else {
            return file;
        }
    }
    private static void requireCanWrite(File file, String name) {
        Objects.requireNonNull(file, "file");
        if (!file.canWrite()) {
            throw new IllegalArgumentException("File parameter '" + name + " is not writable: '" + file + "'");
        }
    }
    private static File getParentFile(File file) {
        return file == null ? null : file.getParentFile();
    }
    private static File mkdirs(File directory) throws IOException {
        if (directory != null && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Cannot create directory '" + directory + "'.");
        } else {
            return directory;
        }
    }
    public static File createParentDirectories(File file) throws IOException {
        return mkdirs(getParentFile(file));
    }

    public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
        Objects.requireNonNull(file, "file");
        if (file.exists()) {
            requireFile(file, "file");
            requireCanWrite(file, "file");
        } else {
            createParentDirectories(file);
        }

        return new FileOutputStream(file, append);
    }

    public static void writeStringToFile(File file, String data, Charset charset) throws IOException {
        writeStringToFile(file, data, charset, false);
    }
    public static void writeStringToFile(File file, String data, Charset charset, boolean append) throws IOException {
        try(OutputStream out = openOutputStream(file, append)) {
            IOUtils.write(data, out, charset);
        }
    }
}
