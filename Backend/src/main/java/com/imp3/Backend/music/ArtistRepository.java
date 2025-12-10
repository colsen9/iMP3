package com.imp3.Backend.music;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Integer> {

    // Find Artists by Name, partial matches accepted
    List<Artist> findByNameStartingWith(String name);

    // Find by exact match
    Optional<Artist> findByName(String name);

    Optional<Artist> findBySpotifyId(String spotifyId);

    Optional<Object> findByTags_NameIgnoreCase(String tagName);
}
