import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FileExporter {
    public static boolean exportPlaylist(String playlistName, List<Song> songs) {
        String fileName = playlistName + "_playlist.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("Playlist: " + playlistName + "\n");
            writer.write("==============================\n");
            for (Song s : songs) {
                writer.write(s.toString() + "\n");
            }
            return true;
        } catch (IOException e) {
            LoggerUtility.logError("Failed to export playlist: " + e.getMessage());
            return false;
        }
    }

    public static boolean exportListeningHistory(String username, List<String> records) {
        String fileName = username + "_listening_history.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("Listening History for: " + username + "\n");
            writer.write("==============================\n");
            for (String r : records) {
                writer.write(r + "\n");
            }
            return true;
        } catch (IOException e) {
            LoggerUtility.logError("Failed to export listening history: " + e.getMessage());
            return false;
        }
    }

    public static boolean exportFullUserDataReport(User user, List<Song> favorites, List<Playlist> playlists, List<String> history) {
        String fileName = user.getUsername() + "_complete_report.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("==================================================\n");
            writer.write("         COMPLETE USER SUMMARY & DATA REPORT      \n");
            writer.write("==================================================\n\n");
            writer.write("Generated at: " + java.time.LocalDateTime.now() + "\n\n");

            // 1. PROFILE DETAILS
            writer.write("1. USER PROFILE INFORMATION\n");
            writer.write("--------------------------------------------------\n");
            writer.write("User ID: " + user.getId() + "\n");
            writer.write("Username: " + user.getUsername() + "\n");
            writer.write("Full Name: " + user.getName() + "\n");
            writer.write("Age: " + user.getAge() + "\n\n");

            // 2. FAVORITES
            writer.write("2. FAVORITE SONGS (Total: " + favorites.size() + ")\n");
            writer.write("--------------------------------------------------\n");
            if (favorites.isEmpty()) {
                writer.write("  No favorite songs added.\n");
            } else {
                int idx = 1;
                for (Song s : favorites) {
                    writer.write(String.format("  %d. %s\n", idx++, s.toString()));
                }
            }
            writer.write("\n");

            // 3. PLAYLISTS
            writer.write("3. USER PLAYLISTS (Total: " + playlists.size() + ")\n");
            writer.write("--------------------------------------------------\n");
            if (playlists.isEmpty()) {
                writer.write("  No playlists created.\n");
            } else {
                for (Playlist p : playlists) {
                    writer.write("  Playlist: " + p.getName() + " (ID: " + p.getId() + ")\n");
                    List<Song> pSongs = p.getSongs();
                    if (pSongs == null || pSongs.isEmpty()) {
                        writer.write("    (Empty Playlist)\n");
                    } else {
                        int sIdx = 1;
                        for (Song s : pSongs) {
                            writer.write(String.format("    %d. %s\n", sIdx++, s.toString()));
                        }
                    }
                    writer.write("\n");
                }
            }

            // 4. LISTENING HISTORY
            writer.write("4. LISTENING HISTORY (Total: " + history.size() + ")\n");
            writer.write("--------------------------------------------------\n");
            if (history.isEmpty()) {
                writer.write("  No playback history found.\n");
            } else {
                int hIdx = 1;
                for (String h : history) {
                    writer.write(String.format("  %d. %s\n", hIdx++, h));
                }
            }
            writer.write("\n==================================================\n");
            return true;
        } catch (IOException e) {
            LoggerUtility.logError("Failed to export full user report: " + e.getMessage());
            return false;
        }
    }
}

