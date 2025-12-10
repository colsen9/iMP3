package com.imp3.Backend.recommendation;

public record SuggestedTrackResponse(
        String spotifyId,
        String name,
        String artistName,
        Integer durationMs,
        String previewUrl
)
{}
