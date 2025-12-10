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
@Table(name= "artists")
public class Artist {

    // ============================================================
    // Internal Table Data
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ============================================================
    // Required Artist Details
    // ============================================================

    // artist's name, string
    @Column(name = "name", nullable = false, unique = false)
    private String name;

    @Column(unique = true)
    private String spotifyId;

    // album owns this relation via the join table in Album.java
    @ManyToMany(mappedBy = "artists")
    private Set<Album> albums = new HashSet<>();

    // track owns this relation via the join table in Track.java
    @ManyToMany(mappedBy = "artists")
    private Set<Track> tracks = new HashSet<>();

    // ============================================================
    // Optional Metadata (objective)
    // ============================================================

    // photo of artist
    @Column(name = "picture", nullable = true, unique = false, columnDefinition = "LONGBLOB")
    private byte[] picture;

    //photo URL from Spotify
    @Column
    private String pictureUrl;

    // biography
    @Column(name = "bio", nullable = true, unique = false, columnDefinition = "TEXT")
    private String bio;

    // years active
    @ElementCollection
    @CollectionTable(
            name = "artist_years",
            joinColumns = @JoinColumn(name = "artist_id")
    )
    @Column(name = "years")
    private List<Integer> years;

    @ManyToMany
    @JoinTable(
            name = "artist_tags",
            joinColumns = @JoinColumn(name = "artist_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    // ============================================================
    // Constructor
    // ============================================================

    public Artist(String name, Set<Album> albums, Set<Track> tracks,
                  byte[] picture, String bio, List<Integer> years) {

        this.name = name;
        this.albums = albums;
        this.tracks = tracks;

        this.picture = picture;
        this.bio = bio;
        this.years = years;
    }

    // ============================================================
    // Helper Functions
    // ============================================================

    public void addAlbum(Album album) {
        this.albums.add(album);
        album.getArtists().add(this);
    }

    public void removeAlbum(Album album) {
        this.albums.remove(album);
        album.getArtists().remove(this);
    }

    public void addTrack(Track track) {
        this.tracks.add(track);
        track.getArtists().add(this);
    }

    public void removeTrack(Track track) {
        this.tracks.remove(track);
        track.getArtists().remove(this);
    }

    public void addTag(Tag tag){
        this.tags.add(tag);
    }


}
