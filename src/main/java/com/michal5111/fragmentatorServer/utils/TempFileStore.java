package com.michal5111.fragmentatorServer.utils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class TempFileStore {
    private List<File> files = new LinkedList<>();

    public File add(File file) {
        files.add(file);
        return file;
    }

    public void clear() {
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i) != null) {
                if (files.get(i).exists()) {
                    files.get(i).delete();
                    files.remove(files.get(i));
                }
            }
        }
    }
}
