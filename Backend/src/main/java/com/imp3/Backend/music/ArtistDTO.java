package com.imp3.Backend.music;

import java.util.List;
import java.util.Set;

public record ArtistDTO (
    Integer id,
    String spotifyId,
    String name,
    String bio,
    byte[] picture,
    String pictureUrl,
    List<Integer> years,
    Set<Integer> albumIds,
    Set<Integer> trackIds,
    Set<String> tags
) {}
