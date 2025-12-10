/** @author Cayden Olsen **/

package com.example.androidexample;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Artist {
    private Integer id;
    private String spotifyId;
    private String name;
    private String bio;
    private byte[] picture;
    private List<Integer> years = new ArrayList<>();
    private Set<Integer> albumIds = new HashSet<>();
    private Set<Integer> trackIds = new HashSet<>();

    public Artist() {}

    public Artist(Integer id, String spotifyId, String name, String bio, byte[] picture,
                  List<Integer> years, Set<Integer> albumIds, Set<Integer> trackIds) {
        this.id = id;
        this.spotifyId = spotifyId;
        this.name = name;
        this.bio = bio;
        this.picture = picture;
        this.years = years;
        this.albumIds = albumIds;
        this.trackIds = trackIds;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getSpotifyId() { return spotifyId; }
    public void setSpotifyId(String spotifyId) { this.spotifyId = spotifyId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public byte[] getPicture() { return picture; }
    public void setPicture(byte[] picture) { this.picture = picture; }

    public List<Integer> getYears() { return years; }
    public void setYears(List<Integer> years) { this.years = years; }

    public Set<Integer> getAlbumIds() { return albumIds; }
    public void setAlbumIds(Set<Integer> albumIds) { this.albumIds = albumIds; }

    public Set<Integer> getTrackIds() { return trackIds; }
    public void setTrackIds(Set<Integer> trackIds) { this.trackIds = trackIds; }
}

