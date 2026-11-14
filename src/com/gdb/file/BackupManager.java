package com.gdb.file;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Creates timestamped copies of data files and prunes backups to a retention limit. */
public final class BackupManager {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final AtomicLong SUFFIX = new AtomicLong();
    private BackupManager() { }

    public static void backup(String sourceFile, int maxBackups) throws IOException {
        if (maxBackups < 0) throw new IllegalArgumentException("maxBackups cannot be negative");
        Path source = Paths.get(sourceFile).toAbsolutePath();
        if (!Files.exists(source)) return;
        Path parent = source.getParent();
        String prefix = source.getFileName().toString() + ".bak.";
        if (maxBackups > 0) {
            String stamp = LocalDateTime.now().format(STAMP) + "_" + SUFFIX.incrementAndGet();
            Files.copy(source, parent.resolve(prefix + stamp));
        }
        List<Path> backups = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent, prefix + "*")) {
            for (Path candidate : stream) if (Files.isRegularFile(candidate)) backups.add(candidate);
        }
        backups.sort(Comparator.comparing(p -> p.getFileName().toString()));
        while (backups.size() > maxBackups) Files.deleteIfExists(backups.remove(0));
    }
}
