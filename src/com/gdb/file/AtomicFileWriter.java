package com.gdb.file;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

/** Writes a complete replacement beside its target and swaps it into place. */
public final class AtomicFileWriter {
    private AtomicFileWriter() { }

    public static void write(String path, List<String> lines) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(lines, "lines");
        Path target = Paths.get(path).toAbsolutePath();
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = target.resolveSibling(target.getFileName().toString() + ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                for (String line : lines) {
                    writer.write(line == null ? "" : line);
                    writer.newLine();
                }
                writer.flush();
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicFailure) {
                try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
                catch (IOException fallbackFailure) { fallbackFailure.addSuppressed(atomicFailure); throw fallbackFailure; }
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
