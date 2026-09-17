import java.util.Comparator;
import java.util.List;

public class SongSort {
    public static void sortByReleaseYear(List<Song> songs) {
        songs.sort(Comparator.comparingInt(Song::getReleaseYear));
    }

    public static void sortByDuration(List<Song> songs) {
        songs.sort(Comparator.comparingInt(Song::getDuration));
    }
}
