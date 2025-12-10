package com.imp3.Backend.list;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListAlbumRepository extends JpaRepository<ListAlbum,Integer> {
    boolean existsByList_ListIdAndAlbumId(Integer listId, Integer albumId);
    void deleteByList_ListIdAndAlbumId(Integer listId, Integer albumId);
    List<ListAlbum> findAllByList_ListId(Integer listId);

    void deleteAllByList_ListId(Integer listId);
}
