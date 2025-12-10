package com.imp3.Backend.music;

import java.util.Set;

public record AlbumDTO(
    Integer id,
    String spotifyId,
    String name,
    byte[] albumArt,
    String albumArtUrl,
    Integer duration,
    Integer releaseDate,
    Set<Integer> artistIds,
    Set<Integer> trackIds,
    Set<String> tags
) {}
