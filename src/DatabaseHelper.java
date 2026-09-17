import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    // --- User Queries ---
    public boolean registerUser(String username, String password, String name, int age, String question, String answer) throws DatabaseConnectionException {
        String sql = "INSERT INTO users (username, password, name, age, security_question, security_answer) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, name);
            ps.setInt(4, age);
            ps.setString(5, question);
            ps.setString(6, answer);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("\n[ERROR] Username already exists! Please choose another.");
                return false;
            }
            throw new DatabaseConnectionException("Database error during registration", e);
        }
    }

    public User loginUser(String username, String password) throws DatabaseConnectionException, InvalidLoginException {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"),
                            rs.getString("name"), rs.getInt("age"), rs.getString("security_question"), rs.getString("security_answer"));
                    return user;
                }
                throw new InvalidLoginException("Invalid username or password.");
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Database error during login", e);
        }
    }

    public String getSecurityQuestion(String username) throws DatabaseConnectionException {
        String sql = "SELECT security_question FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("security_question");
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error fetching security question", e);
        }
        return null;
    }

    public boolean resetPassword(String username, String answer, String newPassword) throws DatabaseConnectionException {
        String checkSql = "SELECT security_answer FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            psCheck.setString(1, username);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next() && rs.getString("security_answer").equalsIgnoreCase(answer.trim())) {
                    String updateSql = "UPDATE users SET password = ? WHERE username = ?";
                    try (PreparedStatement psUp = conn.prepareStatement(updateSql)) {
                        psUp.setString(1, newPassword);
                        psUp.setString(2, username);
                        return psUp.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error resetting password", e);
        }
        return false;
    }

    public boolean updateUsername(int id, String oldUsername, String newUsername) throws DatabaseConnectionException {
        String sql = "UPDATE users SET username = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newUsername);
            ps.setInt(2, id);
            if (ps.executeUpdate() > 0) {
                String cleanOld = oldUsername.replaceAll("[^a-zA-Z0-9_]", "");
                String cleanNew = newUsername.replaceAll("[^a-zA-Z0-9_]", "");
                if (!cleanOld.isEmpty() && !cleanNew.isEmpty() && !cleanOld.equalsIgnoreCase(cleanNew)) {
                    // We no longer rename user-specific tables because they are removed
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("\n[ERROR] Username already exists! Please choose another.");
                return false;
            }
            throw new DatabaseConnectionException("Error updating username", e);
        }
    }

    public boolean updateName(int id, String name) throws DatabaseConnectionException {
        String sql = "UPDATE users SET name = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error updating name", e);
        }
    }

    public boolean updateAge(int id, int age) throws DatabaseConnectionException {
        String sql = "UPDATE users SET age = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, age);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error updating age", e);
        }
    }

    public boolean updatePassword(int id, String password) throws DatabaseConnectionException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, password);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error updating password", e);
        }
    }

    public boolean updateProfile(int id, String name, int age, String password) throws DatabaseConnectionException {
        String sql = "UPDATE users SET name = ?, age = ?, password = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, age);
            ps.setString(3, password);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error updating profile", e);
        }
    }


    public boolean deleteUserAccount(int id) throws DatabaseConnectionException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error deleting account", e);
        }
    }

    public List<User> getAllUsers() throws DatabaseConnectionException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new User(rs.getInt("id"), rs.getString("username"), "", rs.getString("name"),
                        rs.getInt("age"), rs.getString("security_question"), rs.getString("security_answer")));
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error getting users", e);
        }
        return list;
    }

    // --- Admin Queries ---
    public Admin loginAdmin(String username, String password) throws DatabaseConnectionException, InvalidLoginException {
        String sql = "SELECT * FROM admins WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Admin(rs.getInt("id"), rs.getString("username"), rs.getString("name"));
                }
                throw new InvalidLoginException("Invalid admin credentials.");
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Database error during admin login", e);
        }
    }

    // --- Song catalog Queries ---
    public List<Song> getAllSongs() throws DatabaseConnectionException {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT s.id, s.title, ar.name AS artist_name, al.title AS album_title, " +
                "s.genre, s.language, s.duration, s.release_year, s.play_count, s.spotify_uri, s.file_path " +
                "FROM songs s " +
                "LEFT JOIN artists ar ON s.artist_id = ar.id " +
                "LEFT JOIN albums al ON s.album_id = al.id " +
                "ORDER BY s.title";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                songs.add(new Song(rs.getInt("id"), rs.getString("title"), rs.getString("artist_name"),
                        rs.getString("album_title"), rs.getString("genre"), rs.getString("language"),
                        rs.getInt("duration"), rs.getInt("release_year"), rs.getInt("play_count"),
                        rs.getString("spotify_uri"), rs.getString("file_path")));
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error retrieving songs", e);
        }
        return songs;
    }

    public boolean addSong(String title, String artist, String album, String genre, String language, int duration, int year, String spotifyUri, String filePath) throws DatabaseConnectionException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int artistId = getOrCreateArtist(conn, artist);
                int albumId = getOrCreateAlbum(conn, album, artistId);
                String sql = "INSERT INTO songs (title, artist_id, album_id, genre, language, duration, release_year, spotify_uri, file_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, title);
                    ps.setInt(2, artistId);
                    ps.setInt(3, albumId);
                    ps.setString(4, genre);
                    ps.setString(5, language);
                    ps.setInt(6, duration);
                    ps.setInt(7, year);
                    ps.setString(8, spotifyUri);
                    ps.setString(9, filePath);
                    if (ps.executeUpdate() > 0) {
                        conn.commit();
                        return true;
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error adding song", e);
        }
        return false;
    }

    public boolean updateSong(int id, String title, String artist, String album, String genre, String language, int duration, int year, String spotifyUri, String filePath) throws DatabaseConnectionException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int artistId = getOrCreateArtist(conn, artist);
                int albumId = getOrCreateAlbum(conn, album, artistId);
                String sql = "UPDATE songs SET title = ?, artist_id = ?, album_id = ?, genre = ?, language = ?, duration = ?, release_year = ?, spotify_uri = ?, file_path = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, title);
                    ps.setInt(2, artistId);
                    ps.setInt(3, albumId);
                    ps.setString(4, genre);
                    ps.setString(5, language);
                    ps.setInt(6, duration);
                    ps.setInt(7, year);
                    ps.setString(8, spotifyUri);
                    ps.setString(9, filePath);
                    ps.setInt(10, id);
                    if (ps.executeUpdate() > 0) {
                        conn.commit();
                        return true;
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error updating song", e);
        }
        return false;
    }

    public boolean deleteSong(int id) throws DatabaseConnectionException {
        String sql = "DELETE FROM songs WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error deleting song", e);
        }
    }

    public void ensureStoredProcedureExists() {
        String dropSql = "DROP PROCEDURE IF EXISTS sp_record_song_play";
        String procSql = "CREATE PROCEDURE sp_record_song_play("
                       + "  IN p_user_id INT, "
                       + "  IN p_song_id INT, "
                       + "  OUT p_new_play_count INT"
                       + ") "
                       + "BEGIN "
                       + "  UPDATE songs SET play_count = play_count + 1 WHERE id = p_song_id; "
                       + "  SELECT play_count INTO p_new_play_count FROM songs WHERE id = p_song_id; "
                       + "  IF p_user_id IS NOT NULL AND p_user_id > 0 THEN "
                       + "    INSERT INTO history (user_id, song_id) VALUES (p_user_id, p_song_id); "
                       + "  END IF; "
                       + "END";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(dropSql);
            stmt.execute(procSql);
        } catch (Exception e) {
            // System gracefully handles procedure pre-existence
        }
    }

    public int recordSongPlay(int userId, int songId) throws DatabaseConnectionException {
        ensureStoredProcedureExists();
        String sql = "{call sp_record_song_play(?, ?, ?)}";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {
            // 1. Set IN parameters (Position 1 and 2)
            cstmt.setInt(1, userId);
            cstmt.setInt(2, songId);
            // 2. Register OUT parameter (Position 3)
            cstmt.registerOutParameter(3, Types.INTEGER);
            // 3. Execute stored procedure call
            cstmt.execute();
            // 4. Retrieve returned OUT parameter value
            return cstmt.getInt(3);
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error executing stored procedure sp_record_song_play", e);
        }
    }

    public void incrementPlayCount(int songId) {
        try {
            recordSongPlay(0, songId);
        } catch (Exception e) {
            // Suppress error
        }
    }


    private int getOrCreateArtist(Connection conn, String name) throws SQLException {
        String sql = "SELECT id FROM artists WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        String insert = "INSERT INTO artists (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to resolve artist.");
    }

    private int getOrCreateAlbum(Connection conn, String title, int artistId) throws SQLException {
        String sql = "SELECT id FROM albums WHERE title = ? AND artist_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title.trim());
            ps.setInt(2, artistId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        String insert = "INSERT INTO albums (title, artist_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title.trim());
            ps.setInt(2, artistId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to resolve album.");
    }

    // --- Playlist Queries ---
    public boolean createPlaylist(int userId, String name) throws DatabaseConnectionException {
        String sql = "INSERT INTO playlists (user_id, name) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error creating playlist", e);
        }
    }

    public boolean deletePlaylist(int playlistId) throws DatabaseConnectionException {
        String sql = "DELETE FROM playlists WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error deleting playlist", e);
        }
    }

    public boolean renamePlaylist(int playlistId, String newName) throws DatabaseConnectionException {
        String sql = "UPDATE playlists SET name = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setInt(2, playlistId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error renaming playlist", e);
        }
    }

    public boolean addSongToPlaylist(int playlistId, int songId) throws DatabaseConnectionException, DuplicateSongException {
        String checkSql = "SELECT 1 FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            psCheck.setInt(1, playlistId);
            psCheck.setInt(2, songId);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) throw new DuplicateSongException("This song is already present in this playlist.");
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error checking duplicates", e);
        }

        String sql = "INSERT INTO playlist_songs (playlist_id, song_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, songId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error adding song to playlist", e);
        }
    }

    public boolean removeSongFromPlaylist(int playlistId, int songId) throws DatabaseConnectionException {
        String sql = "DELETE FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, songId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error removing song from playlist", e);
        }
    }

    public List<Playlist> getUserPlaylists(int userId) throws DatabaseConnectionException {
        List<Playlist> list = new ArrayList<>();
        String sql = "SELECT * FROM playlists WHERE user_id = ? ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Playlist p = new Playlist(rs.getInt("id"), rs.getString("name"));
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error fetching user playlists", e);
        }

        // Fetch songs for each playlist after the connection for user playlists is closed
        for (Playlist p : list) {
            p.setSongs(getPlaylistSongs(p.getId()));
        }
        return list;
    }

    public List<Song> getPlaylistSongs(int playlistId) throws DatabaseConnectionException {
        List<Song> list = new ArrayList<>();
        String sql = "SELECT s.id, s.title, ar.name AS artist_name, al.title AS album_title, " +
                "s.genre, s.language, s.duration, s.release_year, s.play_count, s.spotify_uri, s.file_path " +
                "FROM playlist_songs ps " +
                "INNER JOIN songs s ON ps.song_id = s.id " +
                "LEFT JOIN artists ar ON s.artist_id = ar.id " +
                "LEFT JOIN albums al ON s.album_id = al.id " +
                "WHERE ps.playlist_id = ? " +
                "ORDER BY s.title";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Song(rs.getInt("id"), rs.getString("title"), rs.getString("artist_name"),
                            rs.getString("album_title"), rs.getString("genre"), rs.getString("language"),
                            rs.getInt("duration"), rs.getInt("release_year"), rs.getInt("play_count"),
                            rs.getString("spotify_uri"), rs.getString("file_path")));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error fetching playlist songs", e);
        }
        return list;
    }

    // --- Favorites Queries ---
    public boolean addFavorite(int userId, int songId) throws DatabaseConnectionException {
        if (isFavorite(userId, songId)) return true;
        String sql = "INSERT INTO favorites (user_id, song_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, songId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error adding favorite", e);
        }
    }

    public boolean removeFavorite(int userId, int songId) throws DatabaseConnectionException {
        String sql = "DELETE FROM favorites WHERE user_id = ? AND song_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, songId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error removing favorite", e);
        }
    }

    public List<Song> getFavorites(int userId) throws DatabaseConnectionException {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT s.id, s.title, ar.name AS artist_name, al.title AS album_title, " +
                "s.genre, s.language, s.duration, s.release_year, s.play_count, s.spotify_uri, s.file_path " +
                "FROM favorites f " +
                "INNER JOIN songs s ON f.song_id = s.id " +
                "LEFT JOIN artists ar ON s.artist_id = ar.id " +
                "LEFT JOIN albums al ON s.album_id = al.id " +
                "WHERE f.user_id = ? " +
                "ORDER BY s.title";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    songs.add(new Song(rs.getInt("id"), rs.getString("title"), rs.getString("artist_name"),
                            rs.getString("album_title"), rs.getString("genre"), rs.getString("language"),
                            rs.getInt("duration"), rs.getInt("release_year"), rs.getInt("play_count"),
                            rs.getString("spotify_uri"), rs.getString("file_path")));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error fetching favorites", e);
        }
        return songs;
    }

    public boolean isFavorite(int userId, int songId) throws DatabaseConnectionException {
        String sql = "SELECT 1 FROM favorites WHERE user_id = ? AND song_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, songId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error checking favorite", e);
        }
    }

    // --- History Queries ---
    public void addHistory(int userId, int songId) {
        String sql = "INSERT INTO history (user_id, song_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, songId);
            ps.executeUpdate();
        } catch (Exception e) {
            // Ignore
        }
    }

    public List<String> getListeningHistory(int userId) throws DatabaseConnectionException {
        List<String> history = new ArrayList<>();
        String sql = "SELECT s.title, ar.name AS artist_name, h.played_at " +
                "FROM history h " +
                "INNER JOIN songs s ON h.song_id = s.id " +
                "LEFT JOIN artists ar ON s.artist_id = ar.id " +
                "WHERE h.user_id = ? " +
                "ORDER BY h.played_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String title = rs.getString("title");
                    String artist = rs.getString("artist_name");
                    Timestamp ts = rs.getTimestamp("played_at");
                    String playedTime = ts != null ? ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A";
                    history.add(String.format("Played: %s by %s at [%s]", title, artist, playedTime));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error loading history", e);
        }
        return history;
    }

    public List<Song> getRecentlyPlayed(int userId, int limit) throws DatabaseConnectionException {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT DISTINCT s.id, s.title, ar.name AS artist_name, al.title AS album_title, " +
                "s.genre, s.language, s.duration, s.release_year, s.play_count, s.spotify_uri, s.file_path " +
                "FROM history h " +
                "INNER JOIN songs s ON h.song_id = s.id " +
                "LEFT JOIN artists ar ON s.artist_id = ar.id " +
                "LEFT JOIN albums al ON s.album_id = al.id " +
                "WHERE h.user_id = ? " +
                "ORDER BY h.id DESC " +
                "LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    songs.add(new Song(rs.getInt("id"), rs.getString("title"), rs.getString("artist_name"),
                            rs.getString("album_title"), rs.getString("genre"), rs.getString("language"),
                            rs.getInt("duration"), rs.getInt("release_year"), rs.getInt("play_count"),
                            rs.getString("spotify_uri"), rs.getString("file_path")));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Error fetching recently played", e);
        }
        return songs;
    }

    public void logUserActivity(String username, String activityType, String details) {
        String createSql = "CREATE TABLE IF NOT EXISTS system_logs ("
                         + "id INT AUTO_INCREMENT PRIMARY KEY, "
                         + "action_type VARCHAR(50) NOT NULL, "
                         + "description VARCHAR(255) NOT NULL, "
                         + "logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                         + ")";
        String insertSql = "INSERT INTO system_logs (action_type, description) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            stmt.execute(createSql);
            ps.setString(1, activityType);
            ps.setString(2, "User " + username + ": " + details);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("[WARNING] Failed to log activity: " + e.getMessage());
        }
    }

    public void syncUserTable(int userId, String username) {
        // Obsolete method: Dynamic user tables have been removed to improve database schema.
    }
}
