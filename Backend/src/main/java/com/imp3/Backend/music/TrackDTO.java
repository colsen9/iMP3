package com.imp3.Backend.music;

import java.util.Set;

public record TrackDTO (
    Integer id,
    String spotifyId,
    String name,
    String genre,
    Integer duration,
    Integer trackNumber,
    String mood,
    Integer bpm,
    Set<Integer> artistIds,
    Set<Integer> albumIds,
    Set<String> tags
) {}
