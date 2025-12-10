package com.imp3.Backend.list;

import com.imp3.Backend.music.MusicMapper;
import com.imp3.Backend.music.TrackDTO;
import com.imp3.Backend.music.TrackRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;

@Service
public class ListService {

    private final ListRepository listrepository;
    private final ListSongRepository listsongrepository;
    private final ListAlbumRepository listalbumrepository;
    private final TrackRepository trackrepository;

    public ListService(ListRepository listrepository,
                       ListSongRepository listsongrepository,
                       ListAlbumRepository listalbumrepository, TrackRepository trackrepository) {
        this.listrepository = listrepository;
        this.listsongrepository = listsongrepository;
        this.listalbumrepository = listalbumrepository;
        this.trackrepository = trackrepository;
    }

    /**
     * Helper method to ensure that the specified list belongs to the given user
     * @param listId id of the list being modified
     * @param userId id of the user attempting the action
     * @return UserList entity if ownership is verified
     * @throws ResponseStatusException if the list is not owned by the user
     */
    private UserList requireOwnedList(Integer listId, Integer userId){
        return listrepository.findByListIdAndOwner_Id(listId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your list"));
    }

    /**
     * Adds a song to the user's list if it does not already exist
     * @param listId the id of the target list
     * @param userId the id of the list owner
     * @param songId the id of the song to add
     */
    @Transactional
    public void addSong(Integer listId, Integer userId, Integer songId){
        UserList list = requireOwnedList(listId, userId);

        //prevent duplicate entries
        if(listsongrepository.existsByList_ListIdAndSongId(listId, songId)) return;

        ListSong toAdd = new ListSong();
        toAdd.setList(list);
        toAdd.setSongId(songId);
        listsongrepository.save(toAdd);
    }

    /**
     * Removes a song from the user's list
     * @param listId the id of the target list
     * @param userId the id of the list owner
     * @param songId the id of the song to remove
     */
    @Transactional
    public void removeSong(Integer listId, Integer userId, Integer songId){
        UserList list = requireOwnedList(listId, userId);
        listsongrepository.deleteByList_ListIdAndSongId(listId, songId);
    }

    /**
     * Adds a song to the user's list if it does not already exist
     * @param listId the id of the target list
     * @param userId the id of the list owner
     * @param albumId the id of the album to add
     */
    @Transactional
    public void addAlbum(Integer listId, Integer userId, Integer albumId){
        UserList list = requireOwnedList(listId, userId);

        //prevent duplicate entries
        if(listalbumrepository.existsByList_ListIdAndAlbumId(listId, albumId)) return;

        ListAlbum toAdd = new ListAlbum();
        toAdd.setList(list);
        toAdd.setAlbumId(albumId);
        listalbumrepository.save(toAdd);
    }

    /**
     *  Removes an album from the user's list
     * @param listId the id of the target list
     * @param userId the id of the list owner
     * @param albumId the id of the album to remove
     */
    @Transactional
    public void removeAlbum(Integer listId, Integer userId, Integer albumId){
        requireOwnedList(listId, userId);
        listalbumrepository.deleteByList_ListIdAndAlbumId(listId, albumId);
    }

    public List<TrackDTO> getSongsForList(Integer listId, Integer requesterId){
        //privacy/ownership check
        UserList list = listrepository.findById(listId).
                orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));

        if(!list.getOwner().getId().equals(requesterId) && list.getPrivacy() == UserList.Privacy.PRIVATE){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this list");
        }

        var entries = listsongrepository.findAllByList_ListId(listId);

        return entries.stream()
                .map(ListSong::getSongId)
                .map(id -> trackrepository.findById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(MusicMapper::toTrackDTO)
                .toList();

    }



}
