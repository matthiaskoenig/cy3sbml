package org.cy3sbml.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The directories COMBINE archives are unpacked into: one new directory per import below
 * a root, so that the SBML files of an archive can reference each other by their relative
 * paths. The directory of an import is deleted when the import is done, the root when the
 * app stops.
 */
public final class ArchiveDirectories {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveDirectories.class);

    private final Path root;

    /**
     * @param root the directory below which the archives are unpacked; created when the
     *     first archive is unpacked
     */
    public ArchiveDirectories(Path root) {
        this.root = root;
    }

    /** The directories below a root in the temporary directory of the system, one per process. */
    public static ArchiveDirectories temporary() {
        return new ArchiveDirectories(Path.of(
                System.getProperty("java.io.tmpdir"),
                "cy3sbml-archives-" + ProcessHandle.current().pid()));
    }

    /** A new, empty directory for the archive with the given file name. */
    public synchronized Path newDirectory(String archiveName) throws IOException {
        Files.createDirectories(root);
        String prefix = archiveName.replaceAll("[^A-Za-z0-9._-]", "_") + "-";
        return Files.createTempDirectory(root, prefix);
    }

    /** Deletes the directory of an archive with its files. */
    public synchronized void delete(Path directory) {
        deleteRecursively(directory);
    }

    /** Deletes the root with all unpacked archives. */
    public synchronized void deleteAll() {
        deleteRecursively(root);
    }

    private static void deleteRecursively(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            logger.warn("The unpacked archive in {} could not be deleted: {}", directory, e.getMessage());
        }
    }
}
