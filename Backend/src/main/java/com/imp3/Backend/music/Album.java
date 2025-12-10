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
@Table(name= "albums")

public class Album {

    // ============================================================
    // Internal Table Data
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ============================================================
    // Required Album Details
    // ============================================================

    // album name
    @Column(name = "title", nullable = false, unique = false)
    private String name;

    @Column(unique = true)
    private String spotifyId;

    // album-artist relation
    @ManyToMany
    @JoinTable(name = "album_artist",
               joinColumns = @JoinColumn(name = "album_id"),
               inverseJoinColumns = @JoinColumn(name = "artist_id"))
    private Set<Artist> artists = new HashSet<>();

    // track owns this relation via the join table in Track.java
    @ManyToMany(mappedBy = "albums")
    private Set<Track> tracks = new HashSet<>();

    // ============================================================
    // Optional Metadata (objective)
    // ============================================================

    @Column(name = "albumArt", nullable = true, unique = false, columnDefinition = "LONGBLOB")
    private byte[] albumArt;

    //photo URL from Spotify
    @Column
    private String AlbumArtUrl;

    @Column(name = "duration", nullable = true, unique = false)
    private Integer duration;

    @Column(name = "releaseDate", nullable = true, unique = false)
    private Integer releaseDate;

    @ManyToMany
    @JoinTable(
            name = "album_tags",
            joinColumns = @JoinColumn(name = "album_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    // ============================================================
    // Constructor
    // ============================================================

    public Album(String name, Set<Artist> artists, Set<Track> tracks,
                 byte[] albumArt, Integer duration, Integer releaseDate) {

        this.name = name;
        this.artists = artists;
        this.tracks = tracks;

        this.albumArt = albumArt;
        this.duration = duration;
        this.releaseDate = releaseDate;

    }

    // ============================================================
    // Helper Functions
    // ============================================================

    public void addArtist(Artist artist) {
        this.artists.add(artist);
        artist.getAlbums().add(this);
    }

    public void addTrack(Track track) {
        this.tracks.add(track);
        track.getAlbums().add(this);
    }

    public void addTag(Tag tag){
        this.tags.add(tag);
    }

}
