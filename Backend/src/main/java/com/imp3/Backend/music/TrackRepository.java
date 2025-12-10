package com.imp3.Backend.music;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrackRepository extends JpaRepository<Track, Integer> {

    // basic functions
    List<Track> findByArtistsId(Integer artistId);
    List<Track> findByAlbumsId(Integer albumId);
    List<Track> findDistinctByArtists_Id(Integer artistId);
    List<Track> findDistinctByAlbums_Id(Integer albumId);

//    // finding singles: find by artist, exclude anything with non-null album
//    List<Track> findByArtistSingle(Integer artistId);

    //find by exact name
    Optional<Track> findByName(String name);

    // Find Tracks by Name, partial matches accepted
    List<Track> findByNameStartingWith(String name);

    <T> Optional<T> findBySpotifyId(String spotifyId);

    Optional<Object> findByTags_NameIgnoreCase(String tagName);
}
