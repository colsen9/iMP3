/** @author Cayden Olsen **/

package com.example.androidexample;

import java.util.HashSet;
import java.util.Set;

public class Album {
    private Integer id;
    private String name;
    private byte[] albumArt;
    private Integer duration;
    private Integer releaseDate;
    private Set<Integer> artistIds = new HashSet<>();
    private Set<Integer> trackIds = new HashSet<>();

    public Album() {}

    public Album(Integer id, String name, byte[] albumArt, Integer duration, Integer releaseDate,
                 Set<Integer> artistIds, Set<Integer> trackIds) {
        this.id = id;
        this.name = name;
        this.albumArt = albumArt;
        this.duration = duration;
        this.releaseDate = releaseDate;
        this.artistIds = artistIds;
        this.trackIds = trackIds;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public byte[] getAlbumArt() { return albumArt; }
    public void setAlbumArt(byte[] albumArt) { this.albumArt = albumArt; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public Integer getReleaseDate() { return releaseDate; }
    public void setReleaseDate(Integer releaseDate) { this.releaseDate = releaseDate; }

    public Set<Integer> getArtistIds() { return artistIds; }
    public void setArtistIds(Set<Integer> artistIds) { this.artistIds = artistIds; }

    public Set<Integer> getTrackIds() { return trackIds; }
    public void setTrackIds(Set<Integer> trackIds) { this.trackIds = trackIds; }
}


