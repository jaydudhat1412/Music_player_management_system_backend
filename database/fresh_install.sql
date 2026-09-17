-- ==========================================
-- MUSIC PLAYER MANAGEMENT SYSTEM - DATABASE
-- CLEAN INSTALLATION SCRIPT
-- ==========================================

DROP DATABASE IF EXISTS music_player_db;
CREATE DATABASE music_player_db;
USE music_player_db;

-- 1. Artists Table
CREATE TABLE artists (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- 2. Albums Table
CREATE TABLE albums (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    artist_id INT,
    FOREIGN KEY (artist_id) REFERENCES artists(id) ON DELETE CASCADE
);

-- 3. Songs Table
CREATE TABLE songs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    artist_id INT,
    album_id INT,
    genre VARCHAR(50) NOT NULL,
    language VARCHAR(50) NOT NULL,
    duration INT NOT NULL, -- in seconds
    release_year INT NOT NULL,
    play_count INT DEFAULT 0,
    spotify_uri VARCHAR(255),
    file_path VARCHAR(255),
    FOREIGN KEY (artist_id) REFERENCES artists(id) ON DELETE SET NULL,
    FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE SET NULL
);

-- 4. Users Table
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    age INT,
    security_question VARCHAR(150) NOT NULL,
    security_answer VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Admins Table
CREATE TABLE admins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL
);

-- 6. Playlists Table
CREATE TABLE playlists (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 7. PlaylistSongs Table (Junction Table)
CREATE TABLE playlist_songs (
    playlist_id INT NOT NULL,
    song_id INT NOT NULL,
    PRIMARY KEY (playlist_id, song_id),
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
);

-- 8. Favorites Table (Junction Table)
CREATE TABLE favorites (
    user_id INT NOT NULL,
    song_id INT NOT NULL,
    PRIMARY KEY (user_id, song_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
);

-- 9. History Table
CREATE TABLE history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    song_id INT NOT NULL,
    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
);

-- 10. System Logs Table
CREATE TABLE system_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    action_type VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- DBMS TRIGGERS
-- ==========================================

DELIMITER //
CREATE TRIGGER after_user_insert
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    INSERT INTO system_logs (action_type, description)
    VALUES ('USER_REGISTRATION', CONCAT('New user registered: ', NEW.username));
END;
//
DELIMITER ;

-- ==========================================
-- DBMS STORED PROCEDURES
-- ==========================================

DELIMITER //
CREATE PROCEDURE sp_record_song_play(
    IN p_user_id INT,
    IN p_song_id INT,
    OUT p_new_play_count INT
)
BEGIN
    -- 1. Increment play count for the song
    UPDATE songs SET play_count = play_count + 1 WHERE id = p_song_id;

    -- 2. Fetch updated play count into OUT parameter
    SELECT play_count INTO p_new_play_count FROM songs WHERE id = p_song_id;

    -- 3. Log playback into history if user is valid
    IF p_user_id IS NOT NULL AND p_user_id > 0 THEN
        INSERT INTO history (user_id, song_id) VALUES (p_user_id, p_song_id);
    END IF;
END //
DELIMITER ;

-- ==========================================
-- INSERT SAMPLE DATA
-- ==========================================

-- Insert Admins
INSERT INTO admins (username, password, name) 
VALUES ('admin', 'admin123', 'System Administrator');

-- Insert Users
INSERT INTO users (username, password, name, age, security_question, security_answer)
VALUES 
('john_doe', 'password123', 'John Doe', 20, 'What is your favorite color?', 'blue'),
('jane_smith', 'pass456', 'Jane Smith', 22, 'What is your mother''s maiden name?', 'taylor');

-- Insert Artists
INSERT INTO artists (name) VALUES 
('The Beatles'),
('A.R. Rahman'),
('Taylor Swift'),
('Ed Sheeran'),
('Diljit Dosanjh'),
('Aditya Gadhvi');

-- Insert Albums
INSERT INTO albums (title, artist_id) VALUES 
('Abbey Road', (SELECT id FROM artists WHERE name = 'The Beatles')),
('Rockstar', (SELECT id FROM artists WHERE name = 'A.R. Rahman')),
('1989', (SELECT id FROM artists WHERE name = 'Taylor Swift')),
('Divide', (SELECT id FROM artists WHERE name = 'Ed Sheeran')),
('G.O.A.T.', (SELECT id FROM artists WHERE name = 'Diljit Dosanjh')),
('Coke Studio Bharat', (SELECT id FROM artists WHERE name = 'Aditya Gadhvi'));

-- Insert Songs
INSERT INTO songs (title, artist_id, album_id, genre, language, duration, release_year, play_count, file_path, spotify_uri) VALUES
('Let It Be', 
 (SELECT id FROM artists WHERE name = 'The Beatles'), 
 (SELECT id FROM albums WHERE title = 'Abbey Road'), 
 'Rock', 'English', 230, 1970, 15, 'dummy.mp3', NULL),

('Here Comes the Sun', 
 (SELECT id FROM artists WHERE name = 'The Beatles'), 
 (SELECT id FROM albums WHERE title = 'Abbey Road'), 
 'Rock', 'English', 185, 1969, 25, 'dummy.mp3', NULL),

('Kun Faya Kun', 
 (SELECT id FROM artists WHERE name = 'A.R. Rahman'), 
 (SELECT id FROM albums WHERE title = 'Rockstar'), 
 'Sufi', 'Hindi', 470, 2011, 50, 'dummy.mp3', NULL),

('Nadaan Parindey', 
 (SELECT id FROM artists WHERE name = 'A.R. Rahman'), 
 (SELECT id FROM albums WHERE title = 'Rockstar'), 
 'Rock', 'Hindi', 386, 2011, 35, 'dummy.mp3', NULL),

('Blank Space', 
 (SELECT id FROM artists WHERE name = 'Taylor Swift'), 
 (SELECT id FROM albums WHERE title = '1989'), 
 'Pop', 'English', 231, 2014, 42, 'dummy.mp3', NULL),

('Shake It Off', 
 (SELECT id FROM artists WHERE name = 'Taylor Swift'), 
 (SELECT id FROM albums WHERE title = '1989'), 
 'Pop', 'English', 219, 2014, 30, 'dummy.mp3', NULL),

('Shape of You', 
 (SELECT id FROM artists WHERE name = 'Ed Sheeran'), 
 (SELECT id FROM albums WHERE title = 'Divide'), 
 'Pop', 'English', 233, 2017, 60, 'dummy.mp3', NULL),

('Perfect', 
 (SELECT id FROM artists WHERE name = 'Ed Sheeran'), 
 (SELECT id FROM albums WHERE title = 'Divide'), 
 'Romance', 'English', 263, 2017, 55, 'dummy.mp3', NULL),

('G.O.A.T.', 
 (SELECT id FROM artists WHERE name = 'Diljit Dosanjh'), 
 (SELECT id FROM albums WHERE title = 'G.O.A.T.'), 
 'Punjabi Pop', 'Punjabi', 223, 2020, 40, 'dummy.mp3', NULL),

('Clash', 
 (SELECT id FROM artists WHERE name = 'Diljit Dosanjh'), 
 (SELECT id FROM albums WHERE title = 'G.O.A.T.'), 
 'Punjabi Pop', 'Punjabi', 176, 2020, 28, 'dummy.mp3', NULL),
 
('khalasi', 
 (SELECT id FROM artists WHERE name = 'Aditya Gadhvi'), 
 (SELECT id FROM albums WHERE title = 'Coke Studio Bharat'), 
 'Folk', 'Gujarati', 248, 2023, 100, 'dummy.mp3', 'https://open.spotify.com/track/74pE6nsaOUfOo7nBrFR6Nc?si=b9efe7765069438c');
