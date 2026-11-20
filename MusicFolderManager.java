
package io.mylyricx.playlist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public final class MusicFolderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("MyLyricX");

    public static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("mp3", "ogg", "wav", "mp4");

    private final Path musicDir;
    private final Path lyricsDir;

    public MusicFolderManager(Path gameDir) {
        this.musicDir = gameDir.resolve("myMusics");
        this.lyricsDir = this.musicDir.resolve("Lyrics");
    }

    public Path getMusicDir() {
        return musicDir;
    }

    public Path getLyricsDir() {
        return lyricsDir;
    }

    public void ensureDirectories() throws IOException {
        Files.createDirectories(musicDir);
        Files.createDirectories(lyricsDir);
    }

    public List<TrackEntry> scanTracks() {
        try {
            ensureDirectories();
        } catch (IOException e) {
            LOGGER.error("Failed to create myMusics directories: {}", e.getMessage());
            return List.of();
        }

        List<TrackEntry> tracks = new ArrayList<>();

        try (Stream<Path> stream = Files.list(musicDir)) {
            stream.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(
                            p -> p.getFileName().toString().toLowerCase(Locale.ROOT)
                    ))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        int dotIdx = fileName.lastIndexOf('.');

                        if (dotIdx <= 0 || dotIdx == fileName.length() - 1) {
                            return;
                        }

                        String baseName = fileName.substring(0, dotIdx);
                        String ext = fileName.substring(dotIdx + 1)
                                .toLowerCase(Locale.ROOT);

                        if (!SUPPORTED_EXTENSIONS.contains(ext)) {
                            return;
                        }

                        Path candidateLrc = lyricsDir.resolve(baseName + ".lrc");
                        Path resolvedLrc = Files.isRegularFile(candidateLrc)
                                ? candidateLrc
                                : null;

                        tracks.add(new TrackEntry(baseName, path, resolvedLrc, ext));
                    });

        } catch (IOException e) {
            LOGGER.error("Failed to scan myMusics folder: {}", e.getMessage());
            return List.of();
        }

        return java.util.Collections.unmodifiableList(tracks);
    }
}
