/** @author Cayden Olsen **/

package com.example.androidexample;

import java.util.HashSet;
import java.util.Set;

public class Track {
    private Integer id;
    private String name;
    private String genre;
    private Integer duration;
    private Integer trackNumber;
    private String mood;
    private Integer bpm;
    private Set<Integer> artistIds = new HashSet<>();
    private Set<Integer> albumIds = new HashSet<>();

    public Track() {}

    public Track(Integer id, String name, String genre, Integer duration, Integer trackNumber,
                 String mood, Integer bpm, Set<Integer> artistIds, Set<Integer> albumIds) {
        this.id = id;
        this.name = name;
        this.genre = genre;
        this.duration = duration;
        this.trackNumber = trackNumber;
        this.mood = mood;
        this.bpm = bpm;
        this.artistIds = artistIds;
        this.albumIds = albumIds;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public Integer getTrackNumber() { return trackNumber; }
    public void setTrackNumber(Integer trackNumber) { this.trackNumber = trackNumber; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public Integer getBpm() { return bpm; }
    public void setBpm(Integer bpm) { this.bpm = bpm; }

    public Set<Integer> getArtistIds() { return artistIds; }
    public void setArtistIds(Set<Integer> artistIds) { this.artistIds = artistIds; }

    public Set<Integer> getAlbumIds() { return albumIds; }
    public void setAlbumIds(Set<Integer> albumIds) { this.albumIds = albumIds; }
}


