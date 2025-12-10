package com.imp3.Backend.list;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListSongRepository extends JpaRepository<ListSong, Integer> {
    boolean existsByList_ListIdAndSongId(Integer listId, Integer songId);
    void deleteByList_ListIdAndSongId(Integer listId, Integer songId);
    List<ListSong> findAllByList_ListId(Integer listId);


    void deleteAllByList_ListId(Integer listId);
}
