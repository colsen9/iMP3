package com.imp3.Backend.music;

import com.imp3.Backend.music.TrackDTO;
import static com.imp3.Backend.music.MusicMapper.toTrackDTO;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("music/")
public class TrackController {

    // ============================================================
    // Repos and DTOs
    // ============================================================

    @Autowired
    ArtistRepository artistrepository;
    @Autowired
    AlbumRepository albumrepository;
    @Autowired
    TrackRepository trackrepository;

    // ============================================================
    // GET Methods
    // ============================================================

    // get a huge list of all tracks
    @GetMapping("tracks")
    public List<TrackDTO> getAll() {
        return trackrepository.findAll().stream().map(MusicMapper::toTrackDTO).toList();
    }

    // get all singles by one artist
    @GetMapping("artists/{artistId}/tracks")
    public List<TrackDTO> getSingles(@PathVariable Integer artistId) {
        return trackrepository.findDistinctByArtists_Id(artistId).stream().map(MusicMapper::toTrackDTO).toList();
    }

    // get all tracks in one album
    @GetMapping("albums/{albumId}/tracks")
    public List<TrackDTO> getTracks(@PathVariable Integer albumId) {
        return trackrepository.findDistinctByAlbums_Id(albumId).stream().map(MusicMapper::toTrackDTO).toList();
    }

    // get a specific track
    @GetMapping("tracks/{trackId}")
    public TrackDTO getSingle(@PathVariable Integer trackId) {
        TrackDTO dto = trackrepository.findById(trackId).map(MusicMapper::toTrackDTO)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found: " + trackId));
        return dto;
    }

    // search for a track
    @GetMapping("search/tracks/{term}")
    public List<TrackDTO> searchTracks(@PathVariable String term) {
        return trackrepository.findByNameStartingWith(term).stream().map(MusicMapper::toTrackDTO).toList();
    }

    // ============================================================
    // POST Methods
    // ============================================================

    // create a new track, must attach to album
    @PostMapping("tracks/new")
    @Transactional
    public Map<String, Integer> postTrack(@RequestBody TrackDTO dto) {

        // build a new track
        Track track = new Track();

        // pull artists out of the DTO (required)
        Set<Artist> trackArtist = new java.util.HashSet<>();
        for (Integer id : Optional.ofNullable(dto.artistIds()).orElse(Set.of())) {
            if (id == null) continue;
            Artist a = artistrepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artist Not Found"));
            trackArtist.add(a);
        }

        // i said required!!
        if (trackArtist.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one artist is required");
        }

        // pull albums out of the DTO (optional, if null: is a single)
        Set<Album> trackAlbum = new java.util.HashSet<>();
        for (Integer id : Optional.ofNullable(dto.albumIds()).orElse(Set.of())) {
            if (id == null) continue;
            Album a = albumrepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album Not Found"));
            trackAlbum.add(a);
        }

        // attach artist to the new track
        track.setArtists(trackArtist);

        // attach albums to the new track, if there are any
        if (!trackAlbum.isEmpty()) {
            track.setAlbums(trackAlbum);
        }

        // set the track number
        track.setName(dto.name());
        track.setTrackNumber(dto.trackNumber());
        track.setDuration(dto.duration());
        track.setGenre(dto.genre());
        track.setMood(dto.mood());
        track.setBpm(dto.bpm());

        // save the track
        trackrepository.save(track);

        // if we made it here, we succeeded
        return Map.of("status", 200, "trackId", track.getId());
    }

    // ============================================================
    // PUT Methods
    // ============================================================

    @PutMapping("tracks/edit")
    @Transactional
    public Map<String, Integer> editTrack(@RequestBody TrackDTO dto) {

        // make sure the track exists
        Track track = trackrepository.findById(dto.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track " + dto.id() + " not found"));

        // pull artists out of the DTO (required)
        Set<Artist> trackArtist = new java.util.HashSet<>();
        for (Integer id : Optional.ofNullable(dto.artistIds()).orElse(Set.of())) {
            if (id == null) continue;
            Artist a = artistrepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artist Not Found"));
            trackArtist.add(a);
        }

        // i said required!!
        if (trackArtist.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one artist is required");
        }

        // pull albums out of the DTO (optional, if null: is a single)
        Set<Album> trackAlbum = new java.util.HashSet<>();
        for (Integer id : Optional.ofNullable(dto.albumIds()).orElse(Set.of())) {
            if (id == null) continue;
            Album a = albumrepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album Not Found"));
            trackAlbum.add(a);
        }

        // attach artist to the new track
        track.setArtists(trackArtist);

        // attach albums to the new track, if there are any
        if (!trackAlbum.isEmpty()) {
            track.setAlbums(trackAlbum);
        }

        // assign the rest of the values values
        track.setId(dto.id());
        track.setName(dto.name());
        track.setTrackNumber(dto.trackNumber());
        track.setDuration(dto.duration());
        track.setGenre(dto.genre());
        track.setMood(dto.mood());
        track.setBpm(dto.bpm());

        // if we made it here, return success
        return Map.of("status", 200, "trackId", track.getId());
    }

    // ============================================================
    // DELETE Methods
    // ============================================================

    // delete a track, removing it from the artists and albums it once belonged to
    @DeleteMapping("tracks/delete/{id}")
    @Transactional
    public Map<String, Integer> deleteTrack(@PathVariable Integer id, HttpSession session) {

        // make sure the track exists
        Track track = trackrepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found"));

        // only admins can remove music from the catalog
        String privilege = (String)session.getAttribute("type");
        if (!privilege.equals("admin")) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"); }

        // remove track from its albums
        for (Album album : new HashSet<>(track.getAlbums())) {
            album.getTracks().remove(track);
        }

        // remove the track from its artists
        for (Artist artist : new HashSet<>(track.getArtists())) {
            artist.getTracks().remove(track);
        }

        // springboot things
        track.getAlbums().clear();
        track.getArtists().clear();

        // delete the track
        trackrepository.delete(track);

        // if we made it here, we succeeded
        return Map.of("status", 200);
    }

    /**
     * GET - Search tracks, albums, and artists by tag
     * @param tagName to search for
     * @return map containing all results
     */
    @GetMapping("/search/tag/{tagName}")
    public Map<String, Object> searchByTag(@PathVariable String tagName) {
        return Map.of(
                "tracks", trackrepository.findByTags_NameIgnoreCase(tagName).stream().map(t -> MusicMapper.toTrackDTO((Track) t)).toList(),
                "albums", albumrepository.findByTags_NameIgnoreCase(tagName).stream().map(a -> MusicMapper.toAlbumDTO((Album) a)).toList(),
                "artists", artistrepository.findByTags_NameIgnoreCase(tagName).stream().map(a -> MusicMapper.toArtistDTO((Artist) a)).toList()

        );
    }
}
