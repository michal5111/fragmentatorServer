package com.michal5111.fragmentator_server.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

public class TempFileStore {
    private final List<File> files = new LinkedList<>();

    public File add(File file) {
        files.add(file);
        return file;
    }

    public void clear() throws IOException {
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i) != null && files.get(i).exists()) {
                Files.deleteIfExists(files.get(i).toPath());
                files.remove(files.get(i));
            }
        }
    }
}
