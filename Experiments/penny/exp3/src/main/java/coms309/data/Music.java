package coms309.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides the Definition/Structure for the people row
 *
 * @author Vivek Bengre
 */
@Getter // Lombok Shortcut for generating getter methods (Matches variable names set ie firstName -> getFirstName)
@Setter // Similarly for setters as well
@NoArgsConstructor // Default constructor
public class Music {

    private int id;
    private String artist;
    private String album;
    private int numSongs;
    List<String> songs = new ArrayList<>();

    public Music(int id, String artist, String album, int numSongs, List<String> songs) {
        this.id = id;
        this.artist = artist;
        this.album = album;
        this.numSongs = numSongs;
        for (String song : songs) {
            this.songs.add(song);
        }
    }

    @Override
    public String toString() { return album + " by " + artist + " [" + id + "]"; }

    public List<String> getSongs() { return songs; }
}
