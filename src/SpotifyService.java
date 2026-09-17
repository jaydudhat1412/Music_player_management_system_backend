import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyService {
    // Fill these in from the Spotify Developer Dashboard
    private static final String CLIENT_ID = "0d6c1f9190264909a8c4e2b2886f4689";
    private static final String CLIENT_SECRET = "971ac88ef7ca44e7b7f165550aeb8e82";
    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";

    private String accessToken = null;
    private final HttpClient httpClient;

    public SpotifyService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private synchronized boolean authenticate() {
        if (accessToken != null)
            return true;
        if (CLIENT_ID.equals("YOUR_CLIENT_ID") || CLIENT_SECRET.equals("YOUR_CLIENT_SECRET")) {
            System.out.println("[SPOTIFY ERROR] You must set your CLIENT_ID and CLIENT_SECRET in SpotifyService.java");
            System.out.println("[SPOTIFY ERROR] Get these from https://developer.spotify.com/dashboard/");
            return false;
        }

        HttpServer server = null;
        try {
            CountDownLatch latch = new CountDownLatch(1);
            String[] authCode = new String[1];

            server = HttpServer.create(new InetSocketAddress(8888), 0);
            server.createContext("/callback", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.contains("code=")) {
                        authCode[0] = query.split("code=")[1].split("&")[0];
                        String response = "<html><body><h2>Authentication Successful!</h2><p>You can close this tab and return to the application.</p><script>window.close();</script></body></html>";
                        exchange.sendResponseHeaders(200, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    } else {
                        String response = "Authentication failed. No code found.";
                        exchange.sendResponseHeaders(400, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                    latch.countDown();
                }
            });
            server.setExecutor(null);
            server.start();

            String authUrl = "https://accounts.spotify.com/authorize?response_type=code" +
                    "&client_id=" + CLIENT_ID +
                    "&scope=user-modify-playback-state" +
                    "&redirect_uri=" + REDIRECT_URI;

            System.out.println("[SPOTIFY] Opening browser for authentication...");
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new URI(authUrl));
            } else {
                System.out.println("Please open this URL in your browser: " + authUrl);
            }

            // Wait for user to authenticate with a 60 second timeout
            boolean authenticated = latch.await(60, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!authenticated || authCode[0] == null) {
                System.out.println("[SPOTIFY ERROR] Failed to get authorization code (timed out or null).");
                return false;
            }

            // Exchange code for token
            String authHeader = "Basic "
                    + Base64.getEncoder().encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes());
            String body = "grant_type=authorization_code&code=" + authCode[0] + "&redirect_uri=" + REDIRECT_URI;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://accounts.spotify.com/api/token"))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse access_token from JSON using regex to avoid external libraries
                Matcher matcher = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
                if (matcher.find()) {
                    accessToken = matcher.group(1);
                    System.out.println("[SPOTIFY] Authentication successful!");
                    return true;
                }
            } else {
                System.out.println("[SPOTIFY ERROR] Token exchange failed: " + response.body());
            }

        } catch (Exception e) {
            System.out.println("[SPOTIFY ERROR] Authentication process failed: " + e.getMessage());
        } finally {
            if (server != null) {
                server.stop(0);
            }
        }
        return false;
    }

    private String getFirstAvailableDevice() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.spotify.com/v1/me/player/devices"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    public boolean playSong(String spotifyUri) {
        if (spotifyUri == null || spotifyUri.isEmpty())
            return false;

        if (!authenticate()) {
            return false;
        }

        try {
            // Check if it's a regular url vs a spotify URI (e.g. spotify:track:...)
            String uriToPlay = spotifyUri;
            if (spotifyUri.contains("open.spotify.com/track/")) {
                String[] parts = spotifyUri.split("track/");
                String trackId = parts[1].split("\\?")[0];
                uriToPlay = "spotify:track:" + trackId;
            }

            String jsonBody = "{\"uris\": [\"" + uriToPlay + "\"]}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.spotify.com/v1/me/player/play"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204 || response.statusCode() == 200) {
                System.out.println("[SPOTIFY] Playback started on active device.");
                return true;
            } else if (response.statusCode() == 404) {
                System.out.println("[SPOTIFY] No active device found. Attempting to wake up available device...");
                String deviceId = getFirstAvailableDevice();
                if (deviceId != null) {
                    HttpRequest retryRequest = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.spotify.com/v1/me/player/play?device_id=" + deviceId))
                            .header("Authorization", "Bearer " + accessToken)
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();
                    HttpResponse<String> retryResponse = httpClient.send(retryRequest, HttpResponse.BodyHandlers.ofString());
                    if (retryResponse.statusCode() == 204 || retryResponse.statusCode() == 200) {
                        System.out.println("[SPOTIFY] Playback started on device: " + deviceId);
                        return true;
                    }
                }
                System.out.println("[SPOTIFY ERROR] Could not find any available devices. Please open Spotify on your phone or PC.");
                return false;
            } else if (response.statusCode() == 403) {
                System.out.println("[SPOTIFY ERROR] 403 Forbidden. Note: Controlling playback via Spotify Web API requires a Spotify Premium account.");
                return false;
            } else {
                System.out.println("[SPOTIFY ERROR] API Error: " + response.statusCode() + " - " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.out.println("[SPOTIFY ERROR] Failed to play: " + e.getMessage());
            return false;
        }
    }

    public void pauseSong() {
        if (!authenticate()) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.spotify.com/v1/me/player/pause"))
                    .header("Authorization", "Bearer " + accessToken)
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 204 && response.statusCode() != 403) {
                System.out.println("[SPOTIFY] Failed to pause. Status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.out.println("[SPOTIFY] Failed to pause: " + e.getMessage());
        }
    }

    public void resumeSong() {
        if (!authenticate()) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.spotify.com/v1/me/player/play"))
                    .header("Authorization", "Bearer " + accessToken)
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 204 && response.statusCode() != 403) {
                System.out.println("[SPOTIFY] Failed to resume. Status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.out.println("[SPOTIFY] Failed to resume: " + e.getMessage());
        }
    }
}
