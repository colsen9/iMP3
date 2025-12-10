package com.imp3.Backend.music;

import com.imp3.Backend.tag.Tag;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name= "tracks")

// tracks must have a unique combination of Artist, Album, and Song values
public class Track {

    // ============================================================
    // Internal Table Data
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ============================================================
    // Required Track Details
    // ============================================================

    // name of track
    @Column(name = "title", nullable = false, unique = false)
    private String name;

    @Column(unique = true)
    private String spotifyId;

    @ManyToMany
    @JoinTable(name = "track_artist",
               joinColumns = @JoinColumn(name = "track_id"),
               inverseJoinColumns = @JoinColumn(name="artist_id"))
    private Set<Artist> artists = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "track_album",
               joinColumns = @JoinColumn(name = "track_id"),
               inverseJoinColumns = @JoinColumn(name = "album_id"))
    private Set<Album> albums = new HashSet<>();

    // ============================================================
    // Optional Metadata (objective)
    // ============================================================

    @Column(name = "trackNumber", nullable = true, unique = false)
    private Integer trackNumber;

    @Column(name = "duration", nullable = true, unique = false)
    private Integer duration;

    // ============================================================
    // Optional Metadata (subjective)
    // ============================================================

    @Column(name = "genre", nullable = true, unique = false)
    private String genre;

    @Column(name = "mood", nullable = true, unique = false)
    private String mood;

    @Column(name = "bpm", nullable = true, unique = false)
    private Integer bpm;

    @ManyToMany
    @JoinTable(
            name = "track_tags",
            joinColumns = @JoinColumn(name = "track_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    // ============================================================
    // Constructor
    // ============================================================

    public Track(Set<Artist> artists, Set<Album> albums, String name,
                 Integer trackNumber, Integer duration, String genre, String mood, Integer bpm) {

        // required fields
        this.artists = artists;
        this.albums = albums;
        this.name = name;

        // optional fields
        this.trackNumber = trackNumber;
        this.duration = duration;

        // subjective fields
        this.genre = genre;
        this.mood = mood;
        this.bpm = bpm;
    }

    // ============================================================
    // Helper Functions
    // ============================================================

    public void addArtist(Artist artist) {
        this.artists.add(artist);
        artist.getTracks().add(this);
    }

    public void addAlbum(Album album) {
        this.albums.add(album);
        album.getTracks().add(this);
    }

    public void addTag(Tag tag){
        this.tags.add(tag);
    }

}
