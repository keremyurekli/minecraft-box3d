package com.keremyurekli.minecraftBox3d;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Box3DLoader {

    private static boolean loaded = false;

    public static synchronized void load() {
        if (loaded) return;
        try {
            String resourcePath = "/natives/windows-x64/box3d.dll";
            InputStream in = Box3DLoader.class.getResourceAsStream(resourcePath);

            if (in == null) {
                System.loadLibrary("box3d");
                loaded = true;
                return;
            }
            File tempFile = File.createTempFile("box3d", ".dll");
            tempFile.deleteOnExit();
            Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.load(tempFile.getAbsolutePath());
            loaded = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Box3D native library", e);
        }
    }


}