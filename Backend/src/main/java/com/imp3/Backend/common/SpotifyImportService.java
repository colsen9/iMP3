package com.imp3.Backend.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.imp3.Backend.music.*;
import com.imp3.Backend.tag.Tag;
import com.imp3.Backend.tag.TagRepository;
import com.imp3.Backend.tag.TagService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyImportService {

    private final ArtistRepository artistrepository;
    private final AlbumRepository albumrepository;
    private final TrackRepository trackrepository;
    private final TagRepository tagrepository;
    private final GeminiService geminiservice;
    private final TagService tagservice;

    /**
     * Gets or creates an artist from Spotify data
     * @param spotifyId of the artist
     * @param name of artist
     * @return artist
     */
    @Transactional
    public Artist getOrCreateArtist(String spotifyId, String name) {

        //check if already exists
        return artistrepository.findBySpotifyId(spotifyId)
                .orElseGet(() -> {
                    // Fallback: check by exact name
                    return artistrepository.findByName(name).map(existing -> {
                        //update w/ spotifyId if missing
                        if(existing.getSpotifyId() == null) {
                            existing.setSpotifyId(spotifyId);
                            return artistrepository.save(existing);
                        }
                        return existing;
                    }).orElseGet(() -> {
                        //create new
                        Artist artist = new Artist();
                        artist.setSpotifyId(spotifyId);
                        artist.setName(name);
                        return artistrepository.save(artist);
                    });
                });
    }

    /**
     *  Gets or creates an album from Spotify data
     * @param spotifyAlbum to create or get
     * @return album
     */
    @Transactional
    public Album getOrCreateAlbum(JsonNode spotifyAlbum ){
        String spotifyId = spotifyAlbum.get("id").asText();
        String name = spotifyAlbum.get("name").asText();

        return albumrepository.findBySpotifyId(spotifyId)
                .orElseGet(() -> {
                    return albumrepository.findByName(name).map(existing -> {
                        if(existing.getSpotifyId() == null){
                            existing.setSpotifyId(spotifyId);
                            return albumrepository.save(existing);
                        }
                        return existing;
                    }).orElseGet(() -> {

                    Album album = new Album();
                    album.setSpotifyId(spotifyId);
                    album.setName(spotifyAlbum.get("name").asText());
                    String releaseDateString = spotifyAlbum.path("release_date").asText("");
                    if(!releaseDateString.isEmpty()){
                        album.setReleaseDate(Integer.valueOf(releaseDateString.substring(0, 4)));
                    }

                    // Get album art URL (Spotify gives URLs, not bytes)
                    JsonNode images = spotifyAlbum.get("images");
                    if (images != null && images.isArray() && !images.isEmpty()) {
                        String url = images.get(0).get("url").asText();

                        album.setAlbumArtUrl(url);
                        album.setAlbumArt(fetchImageBytes(url));
                    }

                    // Create/link artists
                    JsonNode artists = spotifyAlbum.get("artists");
                    if (artists != null) {
                        for (JsonNode a : artists) {
                            Artist artist = getOrCreateArtist(
                                    a.get("id").asText(),
                                    a.get("name").asText()
                            );
                            album.addArtist(artist);
                        }
                    }

                    Album saved =  albumrepository.save(album);
                    autoTagAlbum(saved, spotifyAlbum);
                    return albumrepository.save(saved);
                });
                });
    }


    /**
     * Gets or creates a new track from Spotify data
     * @param spotifyTrack to add to music catalogue or to get
     * @return track
     */
    @Transactional
    public Track getOrCreateTrack(JsonNode spotifyTrack ){
        String spotifyId = spotifyTrack.get("id").asText();
        String name = spotifyTrack.get("name").asText();

        return (Track) trackrepository.findBySpotifyId(spotifyId)
                .orElseGet(() -> {
                    return trackrepository.findByName(name).map(existing -> {
                        if (existing.getSpotifyId() == null) {
                            existing.setSpotifyId(spotifyId);
                                    return trackrepository.save(existing);
                        }
                        return existing;
                    }).orElseGet(() -> {

                    Track track = new Track();
                    track.setSpotifyId(spotifyId);
                    track.setName(spotifyTrack.get("name").asText());
                    track.setDuration(spotifyTrack.path("duration_ms").asInt(0));


                    //link to album (creates if needed)
                    JsonNode albumNode = spotifyTrack.get("album");
                    if(albumNode != null){
                        Album album = getOrCreateAlbum(albumNode);
                        track.addAlbum(album);
                    }

                    //create/link artists
                    JsonNode artists = spotifyTrack.get("artists");
                    if(artists != null) {
                        for(JsonNode a : artists){
                            Artist artist = getOrCreateArtist(
                                    a.get("id").asText(),
                                    a.get("name").asText()
                            );
                            track.addArtist(artist);
                        }
                    }
                    Track saved = trackrepository.save(track);
                    autoTagTrack(saved, spotifyTrack);
                    return trackrepository.save(saved);
                    });
                });
    }

    /**
     * Auto-generate tags for a track using Gemini
     * @param track to add
     * @param spotifyTrack data
     */
    private void autoTagTrack(Track track, JsonNode spotifyTrack){
        try {
            String trackName = spotifyTrack.get("name").asText();

            //get artist names
            List<String> artistNames = new ArrayList<>();
            JsonNode artists = spotifyTrack.get("artists");
            if(artists != null) {
                for(JsonNode a : artists){
                    artistNames.add(a.get("name").asText());
                }
            }

            String prompt = """
            For the song "%s" by %s, generate 3-5 short tags describing the vibe/mood.
            Tags should be 1-3 words, lowercase.
            Examples: "chill", "upbeat", "late night", "workout", "sad", "romantic"
            Return ONLY a JSON array of strings, nothing else.
            """.formatted(trackName, String.join(", ", artistNames));

            String response = geminiservice.generate(prompt);
            List<String> tagNames = tagservice.parseTagsFromResponse(response);

            for (String tagName : tagNames) {
                Tag tag = tagrepository.findByName(tagName)
                        .orElseGet(() -> tagrepository.save(new Tag(tagName, Tag.TagCategory.MOOD)));
                track.addTag(tag);
            }


        } catch (Exception e){
            //don't fail the import if tagging fails
            log.warn("Auto-tagging failed for track : {}", e.getMessage());
        }
    }

    /**
     *  Auto-generate tags for an album using Gemini
     * @param album to add
     * @param spotifyAlbum data
     */
    private void autoTagAlbum(Album album, JsonNode spotifyAlbum){
        try {
            String albumName = spotifyAlbum.get("name").asText();

            List<String> artistNames = new ArrayList<>();
            JsonNode artists = spotifyAlbum.get("artists");
            if (artists != null) {
                for (JsonNode a : artists) {
                    artistNames.add(a.get("name").asText());
                }
            }

            String prompt = """
            For the album "%s" by %s, generate 3-5 short tags describing the vibe/mood.
            Tags should be 1-3 words, lowercase.
            Examples: "chill", "upbeat", "late night", "workout", "sad", "romantic"
            Return ONLY a JSON array of strings, nothing else.
            """.formatted(albumName, String.join(", ", artistNames));

            String response = geminiservice.generate(prompt);
            List<String> tagNames = tagservice.parseTagsFromResponse(response);

            for (String tagName : tagNames) {
                Tag tag = tagrepository.findByName(tagName)
                        .orElseGet(() -> tagrepository.save(new Tag(tagName, Tag.TagCategory.MOOD)));
                album.addTag(tag);
            }
        } catch (Exception e) {
            log.warn("Auto-tagging failed for album: {}", e.getMessage());
        }

    }

    /**
     *  fetch image bytes from Spotify url
     * @param url to fetch bytes from
     * @return bytes if fetched
     */
    private byte[] fetchImageBytes(String url){
        if(url == null || url.isBlank()){
            return null;
        }

        try {
            return new org.springframework.web.client.RestTemplate()
                    .getForObject(url, byte[].class);
        } catch (Exception e){
            log.warn("Failed to fetch image bytes from URL {}: {}", url, e.getMessage());
            return null;
        }
    }




}
