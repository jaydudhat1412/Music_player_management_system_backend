import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class MenuController {
    Scanner scanner = new Scanner(System.in);
    DatabaseHelper dbHelper = new DatabaseHelper();
    PlayQueue playQueue = new PlayQueue();
    RecentStack recentStack = new RecentStack();
    MusicPlayerService playerService = new MusicPlayerService();
    ReportGenerator reportGenerator = new ReportGenerator();

    User currentUser = null;
    Admin currentAdmin = null;

    public void startApp() throws Exception {
        LoggerUtility.logInfo("Music Player Application Started.");
        boolean exit = false;
        while (!exit) {
            if (currentUser == null && currentAdmin == null) {
                printPreLoginMenu();
                String choice = readInput("Choice: ");
                switch (choice) {
                    case "1":
                        handleUserRegistration();
                        break;
                    case "2":
                        handleUserLogin();
                        break;
                    case "3":
                        exit = true;
                        playerService.stop();
                        System.out.println("\nThank you for using Music Player Management System. Good bye!");
                        break;
                    default:
                        System.out.println("[ERROR] Invalid choice!");
                }
            } else if (currentAdmin != null) {
                handleAdminMenu();
            } else {
                printMainMenu();
                String choice = readInput("Enter your choice: ");
                if (!playerService.isPlaying() && choice.equals("8")) {
                    System.out.println("[INFO] Logging out...");
                    if (currentUser != null) {
                        dbHelper.logUserActivity(currentUser.getUsername(), "LOGOUT", "User logged out.");
                    }
                    currentUser = null;
                    currentAdmin = null;
                    playQueue.clear();
                } else if (playerService.isPlaying() && choice.equals("9")) {
                    System.out.println("[INFO] Logging out...");
                    if (currentUser != null) {
                        dbHelper.logUserActivity(currentUser.getUsername(), "LOGOUT", "User logged out.");
                    }
                    currentUser = null;
                    currentAdmin = null;
                    playQueue.clear();
                } else {
                    switch (choice) {
                        case "1":
                            handleUserProfileMenu();
                            break;
                        case "2":
                            handlePlaylistMenu();
                            break;
                        case "3":
                            handleSongsMenu();
                            break;
                        case "4":
                            handleQueueMenu();
                            break;
                        case "5":
                            handleFavoritesMenu();
                            break;
                        case "6":
                            handleHistoryMenu();
                            break;
                        case "7":
                            handleReportsMenu();
                            break;
                        case "8":
                            if (playerService.isPlaying()) {
                                handlePlayerControls();
                            } else {
                                System.out.println("[ERROR] Invalid choice!");
                            }
                            break;
                        default:
                            System.out.println("[ERROR] Invalid choice!");
                    }
                }
            }
        }
    }

    void printPreLoginMenu() {
        System.out.println("\n=============================================");
        System.out.println("        MUSIC PLAYER MANAGEMENT SYSTEM        ");
        System.out.println("=============================================");
        System.out.println("  1. Register");
        System.out.println("  2. Login");
        System.out.println("  3. Exit");
        System.out.println("=============================================");
    }

    void printMainMenu() {
        System.out.println("\n=============================================");
        System.out.println("        MUSIC PLAYER MANAGEMENT SYSTEM        ");
        System.out.println("=============================================");
        if (currentUser != null) {
            System.out.println("  [Logged in as User: " + currentUser.getName() + " (" + currentUser.getUsername() + ")]");
        } else if (currentAdmin != null) {
            System.out.println("  [Logged in as Admin: " + currentAdmin.getName() + "]");
        }
        System.out.println("---------------------------------------------");
        System.out.println("  1. User Profile Menu");
        System.out.println("  2. Playlists Menu");
        System.out.println("  3. Music Catalog Menu (Play, Search, Sort)");
        System.out.println("  4. Play Queue Menu");
        System.out.println("  5. Favorites Menu");
        System.out.println("  6. Listening History Menu");
        System.out.println("  7. Reports Module");
        if (playerService.isPlaying()) {
            System.out.println("  8. Player Controls (Pause/Stop/Skip)");
            System.out.println("  9. Logout");
        } else {
            System.out.println("  8. Logout");
        }
        System.out.println("=============================================");
    }

    void handleUserRegistration() {
        System.out.println("\n--- USER REGISTRATION ---");
        String username = readInput("Enter Username: ");
        if (username == null || username.length() < 2) {
            System.out.println("[ERROR] Username must be at least 2 characters long.");
            return;
        }
        if (!Character.isLetter(username.charAt(0))) {
            System.out.println("[ERROR] Username must start with an alphabet character.");
            return;
        }
        String password = readAndValidatePassword();
        String name = readInput("Enter Full Name: ");
        int age = readIntInput("Enter Age: ");
        if (age <= 15) {
            System.out.println("[ERROR] Age must be greater than 15 to register.");
            return;
        }
        String question = readInput("Security Question: ");
        String answer = readInput("Answer: ");
        try {
            if (dbHelper.registerUser(username, password, name, age, question, answer)) {
                System.out.println("[SUCCESS] Registration completed! Logging you in automatically...");
                dbHelper.logUserActivity(username, "REGISTRATION", "User registered successfully with name: " + name + ", age: " + age);

                // Auto-login logic
                try {
                    currentUser = dbHelper.loginUser(username, password);
                    currentAdmin = null;
                    System.out.println("[SUCCESS] Welcome, " + currentUser.getName() + "!");
                    dbHelper.logUserActivity(currentUser.getUsername(), "LOGIN", "Successfully logged into the system after registration.");
                    dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                    recentStack.clear(); // Fresh user, no history to load yet
                } catch (Exception loginEx) {
                    System.out.println("[ERROR] Auto-login failed: " + loginEx.getMessage() + ". Please login manually.");
                }
            } else {
                System.out.println("[ERROR] Registration failed. Username might exist.");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    void handleUserLogin() {
        System.out.println("\n--- USER LOGIN ---");
        String username = readInput("Username: ");
        String password = readPasswordInput("Password: ");
        try {
            currentAdmin = dbHelper.loginAdmin(username, password);
            currentUser = null;
            System.out.println("[SUCCESS] Welcome Admin, " + currentAdmin.getName() + "!");
            return;
        } catch (InvalidLoginException e) {
            // Not an admin, proceed to try user
        } catch (Exception e) {
            System.out.println("[ERROR] Database error: " + e.getMessage());
            return;
        }

        try {
            currentUser = dbHelper.loginUser(username, password);
            currentAdmin = null;
            System.out.println("[SUCCESS] Welcome, " + currentUser.getName() + "!");
            dbHelper.logUserActivity(currentUser.getUsername(), "LOGIN", "Successfully logged into the system.");
            dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
            List<Song> hist = dbHelper.getRecentlyPlayed(currentUser.getId(), 5);
            recentStack.clear();
            for (int i = hist.size() - 1; i >= 0; i--) recentStack.push(hist.get(i));
        } catch (Exception e) {
            System.out.println("[ERROR] Login failed: " + e.getMessage());
            System.out.print("Forgot password? Recover using security question? (y/n): ");
            if (readInput("").equalsIgnoreCase("y")) {
                handleForgotPassword(username);
            }
        }
    }

    void handleForgotPassword(String username) {
        try {
            String question = dbHelper.getSecurityQuestion(username);
            if (question == null) {
                System.out.println("[ERROR] User does not exist or has no security question.");
                return;
            }
            System.out.println("Security Question: " + question);
            String answer = readInput("Your Answer: ");
            String newPassword = readAndValidatePassword();
            if (dbHelper.resetPassword(username, answer, newPassword)) {

                System.out.println("[SUCCESS] Password reset successfully. Log in using your new password.");
            } else {
                System.out.println("[ERROR] Recovery failed. Incorrect answer.");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    boolean checkUserSession() {
        if (currentUser == null) {
            System.out.println("\n[ACCESS DENIED] Please login as User first.");
            return false;
        }
        return true;
    }

    void handleUserProfileMenu() {
        if (!checkUserSession()) return;
        boolean back = false;
        while (!back) {
            System.out.println("\n--- USER PROFILE MENU ---");
            System.out.println("1. View Profile Details");
            System.out.println("2. Update Profile Details");
            System.out.println("3. Delete Account");
            System.out.println("4. View Complete User Summary (Profile, Favorites, Playlists, History)");
            System.out.println("5. Export Complete User Data Report to File");
            System.out.println("6. Back");
            String choice = readInput("Choice: ");
            switch (choice) {
                case "1":
                    System.out.println("\nProfile Information:");
                    System.out.println("Username: " + currentUser.getUsername());
                    System.out.println("Name: " + currentUser.getName());
                    System.out.println("Age: " + currentUser.getAge());
                    break;
                case "2":
                    handleUpdateProfileMenu();
                    break;
                case "3":
                    System.out.print("[WARNING] Are you sure you want to delete your account? (y/n): ");
                    if (readInput("").equalsIgnoreCase("y")) {
                        try {
                            String uname = currentUser.getUsername();
                            if (dbHelper.deleteUserAccount(currentUser.getId())) {
                                System.out.println("[SUCCESS] Account deleted.");
                                dbHelper.logUserActivity(uname, "DELETE_ACCOUNT", "Account deleted.");
                                currentUser = null;
                                playQueue.clear();
                                back = true;
                            }
                        } catch (Exception e) {
                            System.out.println("[ERROR] Delete failed: " + e.getMessage());
                        }
                    }
                    break;
                case "4":
                    displayCompleteUserDataOverview();
                    break;
                case "5":
                    exportCompleteUserDataReport();
                    break;
                case "6":
                    back = true;
                    break;
                default:
                    System.out.println("[ERROR] Invalid choice!");
            }

        }
    }

    void displayCompleteUserDataOverview() {
        if (!checkUserSession()) return;
        try {
            System.out.println("\n==================================================");
            System.out.println("         COMPLETE USER SUMMARY & DATA DASHBOARD    ");
            System.out.println("==================================================");

            // 1. Profile Information
            System.out.println("\n[1. PROFILE INFORMATION]");
            System.out.println("  User ID  : " + currentUser.getId());
            System.out.println("  Username : " + currentUser.getUsername());
            System.out.println("  Full Name: " + currentUser.getName());
            System.out.println("  Age      : " + currentUser.getAge());

            // 2. Favorites
            List<Song> favorites = dbHelper.getFavorites(currentUser.getId());
            System.out.println("\n[2. FAVORITE SONGS (Total: " + favorites.size() + ")]");
            if (favorites.isEmpty()) {
                System.out.println("  No favorite songs added yet.");
            } else {
                int idx = 1;
                for (Song s : favorites) {
                    System.out.println("  " + (idx++) + ". " + s);
                }
            }

            // 3. Playlists & Songs inside playlists
            List<Playlist> playlists = dbHelper.getUserPlaylists(currentUser.getId());
            System.out.println("\n[3. USER PLAYLISTS (Total: " + playlists.size() + ")]");
            if (playlists.isEmpty()) {
                System.out.println("  No playlists created yet.");
            } else {
                for (Playlist p : playlists) {
                    System.out.println("  Playlist: \"" + p.getName() + "\" [ID: " + p.getId() + "]");
                    List<Song> pSongs = p.getSongs();
                    if (pSongs == null || pSongs.isEmpty()) {
                        System.out.println("     (Empty playlist)");
                    } else {
                        int sIdx = 1;
                        for (Song s : pSongs) {
                            System.out.println("     " + (sIdx++) + ". " + s);
                        }
                    }
                }
            }

            // 4. Listening History
            List<String> history = dbHelper.getListeningHistory(currentUser.getId());
            System.out.println("\n[4. RECENT LISTENING HISTORY (Total: " + history.size() + ")]");
            if (history.isEmpty()) {
                System.out.println("  No listening history recorded.");
            } else {
                int hIdx = 1;
                for (String h : history) {
                    System.out.println("  " + (hIdx++) + ". " + h);
                }
            }

            System.out.println("==================================================\n");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to fetch complete user data: " + e.getMessage());
        }
    }

    void exportCompleteUserDataReport() {
        if (!checkUserSession()) return;
        try {
            List<Song> favorites = dbHelper.getFavorites(currentUser.getId());
            List<Playlist> playlists = dbHelper.getUserPlaylists(currentUser.getId());
            List<String> history = dbHelper.getListeningHistory(currentUser.getId());

            if (FileExporter.exportFullUserDataReport(currentUser, favorites, playlists, history)) {
                String fileName = currentUser.getUsername() + "_complete_report.txt";
                System.out.println("[SUCCESS] Complete user data report exported successfully to file: " + fileName);
                dbHelper.logUserActivity(currentUser.getUsername(), "EXPORT_FULL_REPORT", "Exported complete user summary report.");
            } else {
                System.out.println("[ERROR] Failed to export complete user report file.");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to export report: " + e.getMessage());
        }
    }


    void handleUpdateProfileMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- UPDATE PROFILE DETAILS ---");
            System.out.println("1. Update Username");
            System.out.println("2. Update Name");
            System.out.println("3. Update Age");
            System.out.println("4. Update Password");
            System.out.println("5. Back");
            String choice = readInput("Choice: ");
            switch (choice) {
                case "1":
                    String newUsername = readInput("Enter New Username: ");
                    if (newUsername == null || newUsername.trim().isEmpty()) {
                        System.out.println("[INFO] Username update cancelled.");
                        break;
                    }
                    if (newUsername.trim().length() < 2) {
                        System.out.println("[ERROR] Username must be at least 2 characters long.");
                        break;
                    }
                    if (!Character.isLetter(newUsername.trim().charAt(0))) {
                        System.out.println("[ERROR] Username must start with an alphabet character.");
                        break;
                    }
                    try {
                        String oldUname = currentUser.getUsername();
                        if (dbHelper.updateUsername(currentUser.getId(), oldUname, newUsername.trim())) {
                            currentUser.setUsername(newUsername.trim());
                            System.out.println("[SUCCESS] Username updated successfully to: " + newUsername.trim());
                            dbHelper.logUserActivity(currentUser.getUsername(), "UPDATE_USERNAME", "Updated username from " + oldUname + " to " + newUsername.trim());
                        }
                    } catch (Exception e) {
                        System.out.println("[ERROR] Username update failed: " + e.getMessage());
                    }
                    break;
                case "2":
                    String newName = readInput("Enter New Name: ");
                    if (newName == null || newName.trim().isEmpty()) {
                        System.out.println("[INFO] Name update cancelled.");
                        break;
                    }
                    try {
                        if (dbHelper.updateName(currentUser.getId(), newName.trim())) {
                            currentUser.setName(newName.trim());
                            System.out.println("[SUCCESS] Name updated successfully to: " + newName.trim());
                            dbHelper.logUserActivity(currentUser.getUsername(), "UPDATE_NAME", "Updated name to: " + newName.trim());
                        }
                    } catch (Exception e) {
                        System.out.println("[ERROR] Name update failed: " + e.getMessage());
                    }
                    break;
                case "3":
                    int newAge = readIntInput("Enter New Age: ");
                    if (newAge <= 10) {
                        System.out.println("[ERROR] Age must be greater than 10.");
                        break;
                    }
                    try {
                        if (dbHelper.updateAge(currentUser.getId(), newAge)) {
                            currentUser.setAge(newAge);
                            System.out.println("[SUCCESS] Age updated successfully to: " + newAge);
                            dbHelper.logUserActivity(currentUser.getUsername(), "UPDATE_AGE", "Updated age to: " + newAge);
                        }
                    } catch (Exception e) {
                        System.out.println("[ERROR] Age update failed: " + e.getMessage());
                    }
                    break;
                case "4":
                    String newPass = readAndValidatePassword();
                    try {
                        if (dbHelper.updatePassword(currentUser.getId(), newPass)) {
                            currentUser.setPassword(newPass);
                            System.out.println("[SUCCESS] Password updated successfully!");
                            dbHelper.logUserActivity(currentUser.getUsername(), "UPDATE_PASSWORD", "Updated password successfully.");
                        }
                    } catch (Exception e) {
                        System.out.println("[ERROR] Password update failed: " + e.getMessage());
                    }
                    break;
                case "5":
                    back = true;
                    break;
                default:
                    System.out.println("[ERROR] Invalid choice!");
            }
        }
    }


    void handlePlaylistMenu() {
        if (!checkUserSession()) return;
        boolean back = false;
        while (!back) {
            System.out.println("\n--- PLAYLISTS MENU ---");
            System.out.println("1. Create New Playlist");
            System.out.println("2. Display All My Playlists");
            System.out.println("3. Rename Playlist");
            System.out.println("4. Add Song to Playlist");
            System.out.println("5. Remove Song from Playlist");
            System.out.println("6. Export Playlist to File");
            System.out.println("7. Delete Playlist");
            if (playerService.isPlaying()) {
                System.out.println("8. Player Controls (Pause/Stop/Skip)");
                System.out.println("9. Back");
            } else {
                System.out.println("8. Back");
            }
            String choice = readInput("Choice: ");
            try {
                if (!playerService.isPlaying() && choice.equals("8")) {
                    back = true;
                } else if (playerService.isPlaying() && choice.equals("9")) {
                    back = true;
                } else {
                    switch (choice) {
                        case "1":
                            String name = readInput("Enter playlist name: ");
                            if (dbHelper.createPlaylist(currentUser.getId(), name)) {
                                System.out.println("[SUCCESS] Playlist created!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "CREATE_PLAYLIST", "Created playlist: " + name);
                                dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                            }
                            break;
                        case "2":
                            displayPlaylists();
                            break;
                        case "3":
                            displayPlaylists();
                            int renameId = readIntInput("Enter Playlist ID to rename: ");
                            String newName = readInput("Enter new name: ");
                            if (dbHelper.renamePlaylist(renameId, newName)) {
                                System.out.println("[SUCCESS] Playlist renamed!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "RENAME_PLAYLIST", "Renamed playlist ID: " + renameId + " to: " + newName);
                                dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                            }
                            break;
                        case "4":
                            displayPlaylists();
                            int pId = readIntInput("Enter Playlist ID: ");
                            displayCatalog();
                            int sId = readIntInput("Enter Song ID to add: ");
                            if (dbHelper.addSongToPlaylist(pId, sId)) {
                                System.out.println("[SUCCESS] Song added!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "PLAYLIST_ADD_SONG", "Added song ID " + sId + " to playlist ID " + pId);
                                dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                            }
                            break;
                        case "5":
                            displayPlaylists();
                            int pIdRem = readIntInput("Enter Playlist ID: ");
                            displayPlaylistSongs(pIdRem);
                            int sIdRem = readIntInput("Enter Song ID to remove: ");
                            if (dbHelper.removeSongFromPlaylist(pIdRem, sIdRem)) {
                                System.out.println("[SUCCESS] Song removed!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "PLAYLIST_REMOVE_SONG", "Removed song ID " + sIdRem + " from playlist ID " + pIdRem);
                                dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                            }
                            break;
                        case "6":
                            displayPlaylists();
                            int pIdExp = readIntInput("Enter Playlist ID to export: ");
                            String pName = readInput("Playlist name: ");
                            List<Song> songs = dbHelper.getPlaylistSongs(pIdExp);
                            if (FileExporter.exportPlaylist(pName, songs)) {
                                System.out.println("[SUCCESS] Playlist exported!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "EXPORT_PLAYLIST", "Exported playlist: " + pName);
                            }
                            break;
                        case "7":
                            displayPlaylists();
                            int deleteId = readIntInput("Enter Playlist ID to delete: ");
                            if (dbHelper.deletePlaylist(deleteId)) {
                                System.out.println("[SUCCESS] Playlist deleted!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "DELETE_PLAYLIST", "Deleted playlist ID: " + deleteId);
                                dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                            }
                            break;
                        case "8":
                            if (playerService.isPlaying()) {
                                handlePlayerControls();
                            } else {
                                System.out.println("[ERROR] Invalid choice!");
                            }
                            break;
                        default:
                            System.out.println("[ERROR] Invalid choice!");
                    }
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Operation failed: " + e.getMessage());
            }
        }
    }

    void displayPlaylists() throws Exception {
        List<Playlist> list = dbHelper.getUserPlaylists(currentUser.getId());
        System.out.println("\nYour Playlists:");
        if (list.isEmpty()) System.out.println("No playlists found.");
        else {
            for (Playlist p : list) System.out.println("ID: " + p.getId() + " | " + p);
        }
    }

    void displayPlaylistSongs(int pId) throws Exception {
        List<Song> songs = dbHelper.getPlaylistSongs(pId);
        System.out.println("\nPlaylist Songs:");
        if (songs.isEmpty()) System.out.println("Empty playlist.");
        else {
            for (Song s : songs) System.out.println("Song ID: " + s.getId() + " | " + s);
        }
    }

    void handleSongsMenu() {
        if (!checkUserSession()) return;
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MUSIC CATALOG MENU ---");
            System.out.println("1. View All Songs (Alphabetical)");
            System.out.println("2. Sort Songs (Selection/Insertion Sort)");
            System.out.println("3. Search Songs");
            System.out.println("4. Play Song (Simulate)");
            if (playerService.isPlaying()) {
                System.out.println("5. Player Controls (Pause/Stop/Skip)");
                System.out.println("6. Back");
            } else {
                System.out.println("5. Back");
            }
            String choice = readInput("Choice: ");
            try {
                if (!playerService.isPlaying() && choice.equals("5")) {
                    back = true;
                } else if (playerService.isPlaying() && choice.equals("6")) {
                    back = true;
                } else {
                    switch (choice) {
                        case "1":
                            displayCatalog();
                            break;
                        case "2":
                            handleSortingSubmenu();
                            break;
                        case "3":
                            handleSearchSubmenu();
                            break;
                        case "4":
                            displayCatalog();
                            int sId = readIntInput("Enter Song ID to Play: ");
                            Song songToPlay = getSongFromCatalog(sId);
                            if (songToPlay != null) {
                                playSong(songToPlay, true);
                            } else {
                                System.out.println("[ERROR] Invalid Song ID.");
                            }
                            break;
                        case "5":
                            if (playerService.isPlaying()) {
                                handlePlayerControls();
                            } else {
                                System.out.println("[ERROR] Invalid choice!");
                            }
                            break;
                        default:
                            System.out.println("[ERROR] Invalid choice!");
                    }
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    void offerPlayOrQueue(Song song) {
        System.out.println("Options: [1] Play immediately | [2] Add to Play Queue | [3] Add to Favorites | [4] Do nothing");
        String opt = readInput("Enter option: ");
        try {
            if (opt.equals("1")) {
                playSong(song, true);
            } else if (opt.equals("2")) {
                playQueue.enqueue(song);
                System.out.println("Song queued.");
                dbHelper.logUserActivity(currentUser.getUsername(), "QUEUE_ADD", "Queued song: " + song.getTitle());
            } else if (opt.equals("3")) {
                if (dbHelper.addFavorite(currentUser.getId(), song.getId())) {
                    System.out.println("Added to favorites.");
                    dbHelper.logUserActivity(currentUser.getUsername(), "ADD_FAVORITE", "Added song to favorites: " + song.getTitle());
                    dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    void displayCatalog() throws Exception {
        List<Song> catalog = dbHelper.getAllSongs();
        Collections.sort(catalog);
        System.out.println("\nAll Songs Catalog:");
        if (catalog.isEmpty()) System.out.println("No songs available.");
        else {
            for (Song s : catalog) System.out.println("ID: " + s.getId() + " | " + s);
        }
    }

    Song getSongFromCatalog(int id) throws Exception {
        for (Song s : dbHelper.getAllSongs()) {
            if (s.getId() == id) return s;
        }
        return null;
    }

    void handleSortingSubmenu() throws Exception {
        System.out.println("\nSort Catalog by:");
        System.out.println("1. Duration");
        System.out.println("2. Release Year");
        String ch = readInput("Choice: ");
        List<Song> sorted = dbHelper.getAllSongs();
        if (ch.equals("1")) {
            SongSort.sortByDuration(sorted);
            System.out.println("\nSorted by Duration:");
        } else if (ch.equals("2")) {
            SongSort.sortByReleaseYear(sorted);
            System.out.println("\nSorted by Release Year:");
        } else {
            System.out.println("Invalid choice.");
            return;
        }
        for (Song s : sorted) System.out.println(s);
    }

    void handleSearchSubmenu() throws Exception {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- SEARCH SONG ---");
            System.out.println("1. Search using Name");
            System.out.println("2. Search using ID");
            System.out.println("3. Back");
            String choice = readInput("Choice: ");
            switch (choice) {
                case "1":
                    String query = readInput("Enter song name to search: ");
                    List<Song> results = SongSearch.linearSearch(dbHelper.getAllSongs(), query, "title");
                    System.out.println("\nSearch Results (" + results.size() + " matching):");
                    if (results.isEmpty()) {
                        System.out.println("No matching songs found.");
                    } else {
                        for (Song s : results) {
                            System.out.println("ID: " + s.getId() + " | " + s);
                        }
                        int selectId = readIntInput("Enter Song ID to select (or 0 to cancel): ");
                        if (selectId > 0) {
                            Song selected = getSongFromCatalog(selectId);
                            if (selected != null) offerPlayOrQueue(selected);
                        }
                    }
                    break;
                case "2":
                    int selectId = readIntInput("Enter Song ID: ");
                    if (selectId > 0) {
                        Song selected = getSongFromCatalog(selectId);
                        if (selected != null) {
                            System.out.println("\n[FOUND] " + selected);
                            offerPlayOrQueue(selected);
                        } else {
                            System.out.println("Song not found.");
                        }
                    }
                    break;
                case "3":
                    back = true;
                    break;
                default:
                    System.out.println("[ERROR] Invalid choice!");
            }
        }
    }

    void handleQueueMenu() {
        if (!checkUserSession()) return;
        boolean back = false;
        while (!back) {
            System.out.println("\n--- PLAY QUEUE MENU ---");
            System.out.println("1. Add Song to Queue");
            System.out.println("2. Play Next (Insert song at front)");
            System.out.println("3. View Current Queue & Upcoming");
            System.out.println("4. Remove Song from Queue");
            System.out.println("5. Clear Queue");
            System.out.println("6. Shuffle Queue");
            if (playerService.isPlaying()) {
                System.out.println("7. Player Controls (Pause/Stop/Skip)");
                System.out.println("8. Back");
            } else {
                System.out.println("7. Back");
            }
            String choice = readInput("Choice: ");
            try {
                if (!playerService.isPlaying() && choice.equals("7")) {
                    back = true;
                } else if (playerService.isPlaying() && choice.equals("8")) {
                    back = true;
                } else {
                    switch (choice) {
                        case "1":
                            displayCatalog();
                            int addId = readIntInput("Enter Song ID to queue: ");
                            Song addSong = getSongFromCatalog(addId);
                            if (addSong != null) {
                                playQueue.enqueue(addSong);
                                System.out.println("[SUCCESS] Song added to queue.");
                                dbHelper.logUserActivity(currentUser.getUsername(), "QUEUE_ADD", "Queued song: " + addSong.getTitle());
                                displayQueueStatus();
                            }
                            break;
                        case "2":
                            displayCatalog();
                            int nextId = readIntInput("Enter Song ID to play next: ");
                            Song nextSong = getSongFromCatalog(nextId);
                            if (nextSong != null) {
                                playQueue.playNext(nextSong);
                                System.out.println("[SUCCESS] Song will play next.");
                                dbHelper.logUserActivity(currentUser.getUsername(), "QUEUE_PLAY_NEXT", "Set song to play next: " + nextSong.getTitle());
                                displayQueueStatus();
                            }
                            break;
                        case "3":
                            displayQueueStatus();
                            break;
                        case "4":
                            List<Song> qList = playQueue.getAllSongs();
                            for (int i = 0; i < qList.size(); i++) {
                                System.out.println("[" + i + "] " + qList.get(i));
                            }
                            int remIdx = readIntInput("Enter position index to remove: ");
                            if (playQueue.remove(remIdx)) {
                                System.out.println("[SUCCESS] Removed!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "QUEUE_REMOVE", "Removed song at index: " + remIdx);
                                displayQueueStatus();
                            } else System.out.println("[ERROR] Invalid queue index.");
                            break;
                        case "5":
                            playQueue.clear();
                            System.out.println("Queue cleared.");
                            dbHelper.logUserActivity(currentUser.getUsername(), "QUEUE_CLEAR", "Cleared the play queue.");
                            displayQueueStatus();
                            break;
                        case "6":
                            playQueue.shuffle();
                            System.out.println("Queue shuffled.");
                            dbHelper.logUserActivity(currentUser.getUsername(), "QUEUE_SHUFFLE", "Shuffled the play queue.");
                            displayQueueStatus();
                            break;
                        case "7":
                            if (playerService.isPlaying()) {
                                handlePlayerControls();
                            } else {
                                System.out.println("[ERROR] Invalid choice!");
                            }
                            break;
                        default:
                            System.out.println("[ERROR] Invalid choice!");
                    }
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    void handleFavoritesMenu() {
        if (!checkUserSession()) return;
        boolean back = false;
        while (!back) {
            System.out.println("\n--- FAVORITES MENU ---");
            System.out.println("1. Add Song to Favorites");
            System.out.println("2. Remove Song from Favorites");
            System.out.println("3. View My Favorites");
            if (playerService.isPlaying()) {
                System.out.println("4. Player Controls (Pause/Stop/Skip)");
                System.out.println("5. Back");
            } else {
                System.out.println("4. Back");
            }
            String choice = readInput("Choice: ");
            try {
                if (!playerService.isPlaying() && choice.equals("4")) {
                    back = true;
                } else if (playerService.isPlaying() && choice.equals("5")) {
                    back = true;
                } else {
                    switch (choice) {
                        case "1":
                            displayCatalog();
                            int favId = readIntInput("Enter Song ID to favorite: ");
                            if (dbHelper.addFavorite(currentUser.getId(), favId)) {
                                System.out.println("[SUCCESS] Song added!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "ADD_FAVORITE", "Added song ID " + favId + " to favorites");
                                dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                            }
                            break;
                        case "2":
                            displayFavorites();
                            int remId = readIntInput("Enter Song ID to unfavorite: ");
                            if (dbHelper.removeFavorite(currentUser.getId(), remId)) {
                                System.out.println("[SUCCESS] Song removed!");
                                dbHelper.logUserActivity(currentUser.getUsername(), "REMOVE_FAVORITE", "Removed song ID " + remId + " from favorites");
                                dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
                            }
                            break;
                        case "3":
                            displayFavorites();
                            break;
                        case "4":
                            if (playerService.isPlaying()) {
                                handlePlayerControls();
                            } else {
                                System.out.println("[ERROR] Invalid choice!");
                            }
                            break;
                        default:
                            System.out.println("[ERROR] Invalid choice!");
                    }
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    void displayFavorites() throws Exception {
        List<Song> favs = dbHelper.getFavorites(currentUser.getId());
        System.out.println("\nYour Favorites:");
        if (favs.isEmpty()) System.out.println("No favorite songs.");
        else {
            for (Song s : favs) System.out.println("ID: " + s.getId() + " | " + s);
        }
    }

    void handleHistoryMenu() {
        if (!checkUserSession()) return;
        boolean back = false;
        while (!back) {
            System.out.println("\n--- LISTENING HISTORY MENU ---");
            System.out.println("1. View Recently Played ");
            System.out.println("2. View Full History Log ");
            System.out.println("3. Export History Log to File");
            if (playerService.isPlaying()) {
                System.out.println("4. Player Controls (Pause/Stop/Skip)");
                System.out.println("5. Back");
            } else {
                System.out.println("4. Back");
            }
            String choice = readInput("Choice: ");
            try {
                if (!playerService.isPlaying() && choice.equals("4")) {
                    back = true;
                } else if (playerService.isPlaying() && choice.equals("5")) {
                    back = true;
                } else {
                    switch (choice) {
                        case "1":
                            System.out.println("\nRecently Played (Memory Stack - last 5 plays):");
                            List<Song> memoryRecent = recentStack.getRecent(5);
                            if (memoryRecent.isEmpty()) System.out.println("No songs played recently in this session.");
                            else {
                                for (Song s : memoryRecent) System.out.println(s);
                            }
                            break;
                        case "2":
                            System.out.println("\nFull Listening History (From Database):");
                            List<String> hist = dbHelper.getListeningHistory(currentUser.getId());
                            if (hist.isEmpty()) System.out.println("No history logs.");
                            else {
                                for (String h : hist) System.out.println(h);
                            }
                            break;
                        case "3":
                            List<String> records = dbHelper.getListeningHistory(currentUser.getId());
                            if (FileExporter.exportListeningHistory(currentUser.getUsername(), records)) {
                                System.out.println("[SUCCESS] History exported to " + currentUser.getUsername() + "_listening_history.txt");
                                dbHelper.logUserActivity(currentUser.getUsername(), "EXPORT_HISTORY", "Exported history log to file");
                            }
                            break;
                        case "4":
                            if (playerService.isPlaying()) {
                                handlePlayerControls();
                            } else {
                                System.out.println("[ERROR] Invalid choice!");
                            }
                            break;
                        default:
                            System.out.println("[ERROR] Invalid choice!");
                    }
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    void handleAdminMenu() {
        if (currentAdmin == null) {
            System.out.println("\n--- ADMIN PORTAL LOGIN ---");
            String username = readInput("Admin Username: ");
            String password = readPasswordInput("Admin Password: ");
            try {
                currentAdmin = dbHelper.loginAdmin(username, password);
                currentUser = null; // Clear user session
                System.out.println("[SUCCESS] Welcome, Admin " + currentAdmin.getName() + "!");
            } catch (Exception e) {
                System.out.println("[ERROR] Admin authentication failed: " + e.getMessage());
                return;
            }
        }
        boolean back = false;
        while (!back) {
            System.out.println("\n=================================");
            System.out.println("           ADMIN PORTAL          ");
            System.out.println("=================================");
            System.out.println("1. Add Song to Catalog");
            System.out.println("2. Update Song Details");
            System.out.println("3. Delete Song");
            System.out.println("4. View All Registered Users");
            System.out.println("5. View Database Statistics");
            System.out.println("6. View User History");
            System.out.println("7. Logout Admin");
            System.out.println("8. Back");
            String choice = readInput("Choice: ");
            try {
                switch (choice) {
                    case "1":
                        String title = readInput("Song Title: ");
                        String artist = readInput("Artist: ");
                        String album = readInput("Album: ");
                        String genre = readInput("Genre: ");
                        String lang = readInput("Language: ");
                        int duration = readIntInput("Duration (seconds): ");
                        int year = readIntInput("Release Year: ");
                        String spotifyUri = readInput("Spotify URI (or leave blank): ");
                        String filePath = readInput("Local File Path (or leave blank): ");
                        if (dbHelper.addSong(title, artist, album, genre, lang, duration, year, spotifyUri, filePath)) {
                            System.out.println("[SUCCESS] Song successfully added to catalog!");
                        }
                        break;
                    case "2":
                        displayCatalog();
                        int upId = readIntInput("Enter Song ID to update: ");
                        String upTitle = readInput("Song Title: ");
                        String upArtist = readInput("Artist: ");
                        String upAlbum = readInput("Album: ");
                        String upGenre = readInput("Genre: ");
                        String upLang = readInput("Language: ");
                        int upDur = readIntInput("Duration: ");
                        int upYear = readIntInput("Release Year: ");
                        String upSpotifyUri = readInput("Spotify URI (or leave blank): ");
                        String upFilePath = readInput("Local File Path (or leave blank): ");
                        if (dbHelper.updateSong(upId, upTitle, upArtist, upAlbum, upGenre, upLang, upDur, upYear, upSpotifyUri, upFilePath)) {
                            System.out.println("[SUCCESS] Song updated!");
                        }
                        break;
                    case "3":
                        displayCatalog();
                        int delId = readIntInput("Enter Song ID to delete: ");
                        if (dbHelper.deleteSong(delId)) System.out.println("[SUCCESS] Song removed!");
                        break;
                    case "4":
                        System.out.println("\nRegistered Users:");
                        for (User u : dbHelper.getAllUsers()) System.out.println(u);
                        break;
                    case "5":
                        System.out.println("\nDatabase Statistics:");
                        System.out.println("Total Users: " + dbHelper.getAllUsers().size());
                        System.out.println("Total Songs: " + dbHelper.getAllSongs().size());
                        break;
                    case "6":
                        System.out.println("\nRegistered Users:");
                        for (User u : dbHelper.getAllUsers()) System.out.println(u);
                        int histUserId = readIntInput("Enter User ID to view history: ");
                        try {
                            List<Song> histSongs = dbHelper.getRecentlyPlayed(histUserId, 100);
                            System.out.println("\n--- Listening History ---");
                            if (histSongs.isEmpty()) {
                                System.out.println("No listening history found for this user.");
                            } else {
                                for (Song s : histSongs) {
                                    System.out.println("Song ID: " + s.getId() + " - " + s.getTitle() + " by " + s.getArtistName());
                                }
                            }
                        } catch (DatabaseConnectionException e) {
                            System.out.println("Failed to fetch history: " + e.getMessage());
                        }
                        break;
                    case "7":
                        System.out.println("Admin logged out.");
                        currentAdmin = null;
                        back = true;
                        break;
                    case "8":
                        back = true;
                        break;
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    void handleReportsMenu() {
        if (currentUser == null && currentAdmin == null) {
            System.out.println("[ACCESS DENIED] Please login first to view reports.");
            return;
        }
        boolean back = false;
        while (!back) {
            System.out.println("\n--- REPORTS MODULE ---");
            System.out.println("1. Top 10 Most Played Songs");
            System.out.println("2. Favorite Songs Statistics (Most Liked)");
            System.out.println("3. Playlist Statistics (Summary)");
            if (currentAdmin != null) {
                System.out.println("4. Most Active Users (Admin Only)");
                System.out.println("5. Back");
            } else {
                System.out.println("4. Back");
            }
            String choice = readInput("Choice: ");
            if (currentAdmin != null) {
                switch (choice) {
                    case "1":
                        reportGenerator.displayTop10Songs();
                        break;
                    case "2":
                        if (currentUser != null) {
                            try {
                                displayFavorites();
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        } else System.out.println("Login as a user to view favorites.");
                        break;
                    case "3":
                        reportGenerator.displayPlaylistStats();
                        break;
                    case "4":
                        reportGenerator.displayActiveUsers();
                        break;
                    case "5":
                        back = true;
                        break;
                    default:
                        System.out.println("[ERROR] Invalid choice!");
                }
            } else {
                switch (choice) {
                    case "1":
                        reportGenerator.displayTop10Songs();
                        break;
                    case "2":
                        if (currentUser != null) {
                            try {
                                displayFavorites();
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        } else System.out.println("Login as a user to view favorites.");
                        break;
                    case "3":
                        reportGenerator.displayPlaylistStats();
                        break;
                    case "4":
                        back = true;
                        break;
                    default:
                        System.out.println("[ERROR] Invalid choice!");
                }
            }
        }
    }


    void playSong(Song song, boolean showControls) {
        if (song == null) return;
        
        Runnable onFinish = () -> {
            Song next = playQueue.getNextSong();
            if (next != null) {
                playSong(next, false);
            }
        };

        playerService.play(song, currentUser != null ? currentUser.getId() : -1, onFinish);
        recentStack.push(song);
        if (currentUser != null) {
            dbHelper.logUserActivity(currentUser.getUsername(), "PLAY_SONG", "Started playing song: " + song.getTitle() + " by " + song.getArtistName());
            dbHelper.syncUserTable(currentUser.getId(), currentUser.getUsername());
        }
        
        if (showControls) {
            handlePlayerControls();
        } else {
            System.out.println("\n[INFO] Playing in background. Use 'Player Controls' from any menu to pause/skip.");
        }
    }

    void handlePlayerControls() {
        if (!playerService.isPlaying()) {
            System.out.println("[INFO] No song is currently playing.");
            return;
        }
        boolean back = false;
        while (!back && playerService.isPlaying()) {
            System.out.println("\n=============================================");
            System.out.println("  1. Pause / Continue");
            System.out.println("  2. Stop Playback");
            System.out.println("  3. Skip to Next");
            System.out.println("  4. Skip to Previous");
            System.out.println("  5. Back (Play in Background)");
            System.out.println("=============================================");
            String choice = readInput("");
            switch (choice) {
                case "1":
                    if (playerService.isPaused()) {
                        playerService.resume();
                    } else {
                        playerService.pause();
                    }
                    break;
                case "2":
                    playerService.stop();
                    if (currentUser != null) {
                        dbHelper.logUserActivity(currentUser.getUsername(), "STOP_PLAYBACK", "Stopped playback via controls");
                    }
                    back = true;
                    break;
                case "3":
                    Song next = playQueue.getNextSong();
                    if (next != null) {
                        playSong(next, false);
                    } else {
                        System.out.println("[INFO] End of queue reached. Stopping.");
                        playerService.stop();
                        back = true;
                    }
                    break;
                case "4":
                    Song prev = playQueue.getPreviousSong();
                    if (prev != null) {
                        playSong(prev, false);
                    } else {
                        System.out.println("[INFO] Beginning of queue reached. Stopping.");
                        playerService.stop();
                        back = true;
                    }
                    break;
                case "5":
                    back = true;
                    break;
                default:
                    System.out.println("[ERROR] Invalid choice!");
            }
        }
    }

    void displayQueueStatus() {
        System.out.println("\nPlay Queue Status (Size: " + playQueue.size() + "):");
        Song current = playQueue.getCurrentSong();
        System.out.println("Currently Active: " + (current != null ? current : "None"));
        List<Song> upcoming = playQueue.getUpcomingSongs();
        System.out.println("\nUpcoming Songs in ArrayDeque:");
        if (upcoming.isEmpty()) System.out.println("  No upcoming songs.");
        else {
            int idx = 1;
            for (Song s : upcoming) System.out.println("  " + (idx++) + ". " + s);
        }
    }

    // --- Inputs Helpers ---
    String readInput(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            return "12"; // Graceful default exit choice (Exit option)
        }
    
    }

    int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return -1; // Default/sentinel value indicating exit/empty
            }
            String input = scanner.nextLine();
            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid number format! Please enter an integer.");
            }
        }
    }

    String readPasswordInput(String prompt) {
        // Fallback for IDE environments like IntelliJ that don't support true consoles
        return readInput(prompt);
    }

    String readAndValidatePassword() {
        System.out.println("\n--- Password Requirements ---");
        System.out.println("- At least 8 characters long");
        System.out.println("- Contains at least one uppercase letter");
        System.out.println("- Contains at least one number");
        System.out.println("- Contains at least one special character (@, #, $, !, etc.)");
        System.out.println("-----------------------------");

        while (true) {
            String password = readPasswordInput("Enter Password: ");

            if (!isValidPassword(password)) {
                System.out.println("\n[ERROR] Password does not meet the requirements. Please try again.");
                continue;
            }

            String confirm = readPasswordInput("Re-enter Password to Confirm: ");

            if (password.equals(confirm)) {
                return password;
            } else {
                System.out.println("\n[ERROR] Passwords do not match. Please start over.");
            }
        }
    }

    boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        return hasUpper && hasDigit && hasSpecial;
    }
}
