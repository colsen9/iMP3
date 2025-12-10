package com.imp3.Backend.music;

import io.micrometer.observation.ObservationFilter;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Integer> {

    // find albums by artist
//    List<Album> findByArtistId(Integer artistId);
    List<Album> findDistinctByArtists_Id(Integer artistId);

    //find by name (exact match)
    Optional<Album> findByName(String name);

    // Find Albums by Name, partial matches accepted
    List<Album> findByNameStartingWith(String name);

    Optional<Album> findBySpotifyId(String spotifyId);

    Optional<Object> findByTags_NameIgnoreCase(String tagName);

}
