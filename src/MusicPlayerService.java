import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class MusicPlayerService {
    private final DatabaseHelper dbHelper = new DatabaseHelper();
    private final SpotifyService spotifyService = new SpotifyService();
    private PlaybackThread playbackThread;
    private Song currentSong;
    private Runnable onFinish;

    synchronized void play(Song song, int userId) {
        play(song, userId, null);
    }

    synchronized void play(Song song, int userId, Runnable onFinish) {
        if (song == null) return;
        if (isPlaying()) stop();
        
        boolean useLocal = false;
        boolean useSpotify = false;
        
        String filePath = song.getFilePath();
        String spotifyUri = song.getSpotifyUri();
        
        boolean hasLocal = (filePath != null && !filePath.trim().isEmpty());
        boolean hasSpotify = (spotifyUri != null && !spotifyUri.trim().isEmpty());

        if (!hasLocal && !hasSpotify) {
            System.out.println("\n[INFO] No audio file or Spotify URI found for this song. A default dummy track will be played.");
            hasLocal = true;
            song.setFilePath("dummy.mp3");
        }

        if (hasLocal && hasSpotify) {
            System.out.print("\n[INFO] Song has both formats. Play via (1) Local Audio (2) Spotify? Choice: ");
            Scanner scanner = new Scanner(System.in);
            String choice = scanner.nextLine();
            if (choice.trim().equals("2")) {
                useSpotify = true;
            } else {
                useLocal = true;
            }
        } else if (hasLocal) {
            useLocal = true;
        } else if (hasSpotify) {
            useSpotify = true;
        }

        this.currentSong = song;
        this.onFinish = onFinish;
        playbackThread = new PlaybackThread(song, useLocal, useSpotify, onFinish);
        playbackThread.start();

        try {
            int newCount = dbHelper.recordSongPlay(userId, song.getId());
            LoggerUtility.logInfo("Recorded song play via CallableStatement for song ID: " + song.getId() + ". New Play Count: " + newCount);
        } catch (Exception e) {
            LoggerUtility.logError("Failed to record song play via CallableStatement: " + e.getMessage());
        }
    }

    synchronized void stop() {
        if (playbackThread != null) {
            playbackThread.stopPlayback();
            try {
                playbackThread.join(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            playbackThread = null;
            System.out.println("\n[PLAYER] Stopped playing: \"" + (currentSong != null ? currentSong.getTitle() : "") + "\"");
            currentSong = null;
        } else {
            System.out.println("\n[PLAYER] No song is playing to stop.");
        }
    }

    synchronized void pause() {
        if (playbackThread != null) {
            playbackThread.pausePlayback();
            System.out.println("\n[PLAYER] Paused: \"" + (currentSong != null ? currentSong.getTitle() : "") + "\"");
        }
    }

    synchronized void resume() {
        if (playbackThread != null) {
            playbackThread.resumePlayback();
            System.out.println("\n[PLAYER] Resumed: \"" + (currentSong != null ? currentSong.getTitle() : "") + "\"");
        }
    }

    boolean isPlaying() {
        return playbackThread != null && playbackThread.isAlive();
    }

    boolean isPaused() {
        return playbackThread != null && playbackThread.paused;
    }

    class PlaybackThread extends Thread {
        private final Song song;
        private final boolean useLocal;
        private final boolean useSpotify;
        private final Runnable onFinish;
        
        private int elapsedSeconds = 0;
        private boolean running = true;
        boolean paused = false;
        
        private Clip audioClip;

        PlaybackThread(Song song, boolean useLocal, boolean useSpotify, Runnable onFinish) {
            super("Playback-" + song.getTitle());
            this.song = song;
            this.useLocal = useLocal;
            this.useSpotify = useSpotify;
            this.onFinish = onFinish;
        }

        void pausePlayback() {
            this.paused = true;
            if (useLocal && audioClip != null && audioClip.isRunning()) {
                audioClip.stop();
            } else if (useSpotify) {
                spotifyService.pauseSong();
            }
        }

        void resumePlayback() {
            this.paused = false;
            if (useLocal && audioClip != null && !audioClip.isRunning()) {
                audioClip.loop(Clip.LOOP_CONTINUOUSLY);
            } else if (useSpotify) {
                spotifyService.resumeSong();
            }
        }

        void stopPlayback() {
            this.running = false;
            if (useLocal && audioClip != null) {
                audioClip.stop();
                audioClip.close();
            } else if (useSpotify) {
                spotifyService.pauseSong();
            }
            this.interrupt();
        }

        @Override
        public void run() {
            int total = song.getDuration();
            System.out.println("\n[PLAYER] Now Playing: \"" + song.getTitle() + "\" by " + song.getArtistName());
            
            if (useLocal) {
                try {
                    File audioFile = new File(song.getFilePath());
                    if (!audioFile.exists() && song.getFilePath().equals("dummy.mp3")) {
                        System.out.println("[WARNING] Dummy local audio file not found at: " + song.getFilePath());
                    } else if (!audioFile.exists()) {
                        System.out.println("[WARNING] Local audio file not found at: " + song.getFilePath() + ". Falling back to dummy track.");
                        audioFile = new File("dummy.mp3");
                    }
                    if (audioFile.exists()) {
                        AudioInputStream in = AudioSystem.getAudioInputStream(audioFile);
                        AudioFormat baseFormat = in.getFormat();
                        AudioInputStream decodedIn = in;
                        
                        if (baseFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED && baseFormat.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {
                            AudioFormat decodedFormat = new AudioFormat(
                                    AudioFormat.Encoding.PCM_SIGNED,
                                    baseFormat.getSampleRate(),
                                    16,
                                    baseFormat.getChannels(),
                                    baseFormat.getChannels() * 2,
                                    baseFormat.getSampleRate(),
                                    false);
                            decodedIn = AudioSystem.getAudioInputStream(decodedFormat, in);
                        }

                        audioClip = AudioSystem.getClip();
                        audioClip.open(decodedIn);
                        audioClip.loop(Clip.LOOP_CONTINUOUSLY);
                    } else {
                        System.out.println("[WARNING] Local audio file not found at: " + song.getFilePath());
                    }
                } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                    System.out.println("[ERROR] Failed to play local audio: " + e.getMessage());
                }
            } else if (useSpotify) {
                spotifyService.playSong(song.getSpotifyUri());
            } else {
                System.out.println("[INFO] No actual audio source found for this song. Simulating playback.");
            }

            System.out.print("[PLAYER] Progress: ");
            while (running && elapsedSeconds < total) {
                try {
                    Thread.sleep(1000); // Wait 1 second
                    if (!paused) {
                        elapsedSeconds++;
                    }
                    printProgressBar(elapsedSeconds, total);
                } catch (InterruptedException e) {
                    if (!running) break;
                }
            }
            if (running && elapsedSeconds >= total) {
                System.out.println("\n[PLAYER] Finished playing: \"" + song.getTitle() + "\"");
                if (useLocal && audioClip != null) {
                    audioClip.close();
                }
                if (onFinish != null) {
                    new Thread(onFinish).start();
                }
            }
        }

        void printProgressBar(int elapsed, int total) {
            int width = 20;
            int progress = (int) (((double) elapsed / total) * width);
            StringBuilder sb = new StringBuilder("\r[PLAYER] ");
            for (int i = 0; i < width; i++) {
                if (i < progress) sb.append("=");
                else if (i == progress) sb.append(">");
                else sb.append(" ");
            }
            if (paused) {
                sb.append(String.format(" [%02d:%02d/%02d:%02d] (PAUSED) ",
                        elapsed / 60, elapsed % 60, total / 60, total % 60));
            } else {
                sb.append(String.format(" [%02d:%02d/%02d:%02d] ",
                        elapsed / 60, elapsed % 60, total / 60, total % 60));
            }
            System.out.print(sb.toString());
        }
    }
}
