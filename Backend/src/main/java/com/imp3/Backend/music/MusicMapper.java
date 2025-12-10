package com.imp3.Backend.music;

import com.imp3.Backend.music.AlbumDTO;
import com.imp3.Backend.music.ArtistDTO;
import com.imp3.Backend.music.TrackDTO;
import com.imp3.Backend.tag.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.stream.Collectors;

public class MusicMapper {

    public MusicMapper() {}

    // ============================================================
    // Object -> DTO (one way!)
    // ============================================================

    public static ArtistDTO toArtistDTO(Artist artist) {
        Set<Integer> albumIds = artist.getAlbums().stream().map(Album::getId).collect(Collectors.toSet());
        Set<Integer> trackIds = artist.getTracks().stream().map(Track::getId).collect(Collectors.toSet());
        Set<String> tags = artist.getTags() != null
                ? artist.getTags().stream().map(Tag::getName).collect(Collectors.toSet())
                : Set.of();

        return new ArtistDTO(artist.getId(), artist.getSpotifyId(), artist.getName(), artist.getBio(),
                artist.getPicture(), artist.getPictureUrl(), artist.getYears(), albumIds, trackIds, tags);
    }

    public static AlbumDTO toAlbumDTO(Album album) {
        Set<Integer> artistIds = album.getArtists().stream().map(Artist::getId).collect(Collectors.toSet());
        Set<Integer> trackIds = album.getTracks().stream().map(Track::getId).collect(Collectors.toSet());
        Set<String> tags = album.getTags() != null
                ? album.getTags().stream().map(Tag::getName).collect(Collectors.toSet())
                : Set.of();

        return new AlbumDTO(album.getId(), album.getSpotifyId(), album.getName(), album.getAlbumArt(), album.getAlbumArtUrl(),
                album.getDuration(), album.getReleaseDate(), artistIds, trackIds, tags);
    }

    public static TrackDTO toTrackDTO(Track track) {
        Set<Integer> artistIds = track.getArtists().stream().map(Artist::getId).collect(Collectors.toSet());
        Set<Integer> albumIds = track.getAlbums().stream().map(Album::getId).collect(Collectors.toSet());
        Set<String> tags = track.getTags() != null
                ? track.getTags().stream().map(Tag::getName).collect(Collectors.toSet())
                : Set.of();

        return new TrackDTO(track.getId(), track.getSpotifyId(), track.getName(), track.getGenre(),
                track.getDuration(), track.getTrackNumber(), track.getMood(), track.getBpm(),
                artistIds, albumIds, tags);
    }

    // ============================================================
    // Helper Functions
    // ============================================================

    static Set<Artist> getArtistsById(Set<Integer> artistIds, ArtistRepository artistrepository) {

        // nothing in, nothing out
        if (artistIds == null) {
            return Collections.emptySet();
        }

        // make and populate the set of artists that will be returned
        Set<Artist> artistSet = new HashSet<>(Set.of());
        for (Integer id : artistIds) {
            Artist artist = artistrepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arist not found: " + id));
            artistSet.add(artist);
        }

        return artistSet;
    }

    static Set<Album> getAlbumsById(Set<Integer> albumIds, AlbumRepository albumrepository) {

        // nothing in, nothing out
        if (albumIds == null) {
            return Collections.emptySet();
        }

        // make and populate the set of albums that will be returned
        Set<Album> albumSet = new HashSet<>(Set.of());
        for (Integer id : albumIds) {
            Album album = albumrepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found: " + id));
            albumSet.add(album);
        }

        return albumSet;

    }

}
