package com.imp3.Backend.music;

import com.imp3.Backend.music.ArtistDTO;
import static com.imp3.Backend.music.MusicMapper.toArtistDTO;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.transaction.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("music/")
public class ArtistController {

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

    // get a list of all artists
    @GetMapping("artists/")
    public List<ArtistDTO> getAll() {
        return artistrepository.findAll().stream().map(MusicMapper::toArtistDTO).toList();
    }

    // get a specific artist's information
    @GetMapping("artists/{artistId}")
    public ArtistDTO getArtist(@PathVariable Integer artistId) {
        ArtistDTO dto = artistrepository.findById(artistId).map(MusicMapper::toArtistDTO).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Artist not found: " + artistId));
        return dto;
    }

    // search for an artist
    @GetMapping("search/artists/{term}")
    public List<ArtistDTO> searchArtists(@PathVariable String term) {
        return artistrepository.findByNameStartingWith(term).stream().map(MusicMapper::toArtistDTO).toList();
    }

    // ============================================================
    // POST Methods
    // ============================================================

    // create a new artist
    @PostMapping("artists/new")
    @Transactional
    public Map<String, Integer> postArtist(@RequestBody ArtistDTO dto) {

        // create new artist
        Artist artist = new Artist();
        artist.setName(dto.name());
        artist.setBio(dto.bio());
        artist.setPicture(dto.picture());
        artist.setYears(dto.years());
        // albums and singles are not created here, those are added to artists via dedicated endpoints

        // record the artist in the table
        artistrepository.save(artist);

        // get new artist's ID
        Integer newId = artist.getId();

        // if we made it here, we succeeded, send the new artist's ID too
        return Map.of("status", 200, "artistId", newId);
    }

    // ============================================================
    // PUT Methods
    // ============================================================

    // update an artist's information
    @PutMapping("artists/edit")
    public Map<String, Integer> editArtist(@RequestBody ArtistDTO dto) {

        // check if the artist already exists
        Artist artist = artistrepository.findById(dto.id()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Artist not found"));

        // albums and tracks are attached to an artist, not the other way around, so we don't care about that here

        // direct artist fields
        artist.setId(dto.id());
        artist.setName(dto.name());
        artist.setBio(dto.bio());
        artist.setPicture(dto.picture());
        artist.setYears(dto.years());

        // record the artist in the table
        artistrepository.save(artist);

        // if we made it here, we succeeded, send the new artist's ID too
        return Map.of("status", 200, "artistId", artist.getId());
    }

    // ============================================================
    // DELETE Methods
    // ============================================================

    // delete an artist and their music (remember to consider collaborations)
    @DeleteMapping("artists/delete/{id}")
    public Map<String, Integer> deleteArtist(@PathVariable Integer id, HttpSession session) {

        // check if the artist already exists
        Artist artist = artistrepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Artist not found"));

        // only admins can remove music from the catalog
        String privilege = (String)session.getAttribute("type");
        if (!privilege.equals("admin")) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"); }

        // remove artist from albums
        for (Album album : new HashSet<>(artist.getAlbums())) {
            album.getArtists().remove(artist);
        }

        // remove artist from tracks
        for (Track track : new HashSet<>(artist.getTracks())) {
            track.getArtists().remove(artist);
        }

        // springboot things
        artist.getAlbums().clear();
        artist.getTracks().clear();

        // delete the artist
        artistrepository.delete(artist);

        // if we made it here, we succeeded, send the new artist's ID too
        return Map.of("status", 200, "artistId", artist.getId());
    }


}
