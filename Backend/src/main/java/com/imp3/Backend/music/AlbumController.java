package com.imp3.Backend.music;

import com.imp3.Backend.music.AlbumDTO;
import static com.imp3.Backend.music.MusicMapper.toAlbumDTO;

import com.imp3.Backend.reviews.Review;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("music/")
public class AlbumController {

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

    // get all albums
    @GetMapping("albums")
    public List<AlbumDTO> getAllAlbums() {
        return albumrepository.findAll().stream().map(MusicMapper::toAlbumDTO).toList();
    }

    // get all albums by an artist
    @GetMapping("artists/{artistId}/albums")
    public List<AlbumDTO> getAllByArtist(@PathVariable Integer artistId) {
        return albumrepository.findDistinctByArtists_Id(artistId).stream().map(MusicMapper::toAlbumDTO).toList();
    }

    // get specific album information
    @GetMapping("albums/{albumId}")
    public AlbumDTO getAlbumByArtist(@PathVariable Integer albumId) {
        AlbumDTO dto = albumrepository.findById(albumId).map(MusicMapper::toAlbumDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found: " + albumId));
        return dto;
    }

    // search for an album
    @GetMapping("search/albums/{term}")
    public List<AlbumDTO> searchAlbums(@PathVariable String term) {
        return albumrepository.findByNameStartingWith(term).stream().map(MusicMapper::toAlbumDTO).toList();
    }

    // ============================================================
    // POST Methods
    // ============================================================

    // create a new album, must attach to artist
    @PostMapping("albums/new")
    @Transactional
    public Map<String, Integer> postAlbum(@RequestBody AlbumDTO dto) {

        // build a new album
        Album album = new Album();
        album.setName(dto.name());
        album.setAlbumArt(dto.albumArt());
        album.setDuration(dto.duration());
        album.setReleaseDate(dto.releaseDate());

        // add artists from the dto
        for (Integer id : Optional.ofNullable(dto.artistIds()).orElse(Set.of())) {
            if (id == null) continue;
            Artist artist = artistrepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artist not found: " + id));
            album.addArtist(artist);
        }

        // tracks will be added when they are created, not now

        // save the album
        albumrepository.save(album);

        // if we made it here, we succeeded
        return Map.of("status", 200, "albumId", album.getId());
    }

    // ============================================================
    // PUT Methods
    // ============================================================

    // create a new album, must attach to artist
    @PutMapping("albums/edit")
    @Transactional
    public Map<String, Integer> editAlbum(@RequestBody AlbumDTO dto) {

        // check if the album already exists
        Album album = albumrepository.findById(dto.id()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found"));

        // build a new album
        album.setId(dto.id());
        album.setName(dto.name());
        album.setAlbumArt(dto.albumArt());
        album.setDuration(dto.duration());
        album.setReleaseDate(dto.releaseDate());

        // add artists from the dto
        for (Integer id : Optional.ofNullable(dto.artistIds()).orElse(Set.of())) {
            if (id == null) continue;
            Artist artist = artistrepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artist not found: " + id));
            album.addArtist(artist);
        }

        // tracks will be added when they are created, not now

        // save the album
        albumrepository.save(album);

        // if we made it here, we succeeded
        return Map.of("status", 200, "albumId", album.getId());

    }

    // ============================================================
    // DELETE Methods
    // ============================================================

    // delete an album
    @DeleteMapping("albums/delete/{id}")
    public Map<String, Integer> deleteAlbum(@PathVariable Integer id, HttpSession session) {

        // check if the album exists
        Album album = albumrepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found"));

        // only admins can remove music from the catalog
        String privilege = (String)session.getAttribute("type");
        if (!privilege.equals("admin")) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"); }

        // remove the album from its artists
        for (Artist artist : new HashSet<>(album.getArtists())) {
            artist.getAlbums().remove(album);
        }

        // remove album from its tracks
        for (Track track : new HashSet<>(album.getTracks())) {
            track.getAlbums().remove(album);
        }

        // springboot things
        album.getArtists().clear();
        album.getTracks().clear();

        // delete the album
        albumrepository.delete(album);

        // if we made it here, we succeeded, send the new album's ID too
        return Map.of("status", 200, "albumId", album.getId());
    }
}
