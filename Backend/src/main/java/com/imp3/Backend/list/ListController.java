package com.imp3.Backend.list;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imp3.Backend.common.AbstractController;
import com.imp3.Backend.common.SpotifyImportService;
import com.imp3.Backend.music.Album;
import com.imp3.Backend.music.Track;
import com.imp3.Backend.music.TrackDTO;
import com.imp3.Backend.notification.NotificationService;
import com.imp3.Backend.user.User;
import com.imp3.Backend.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lists")
public class ListController extends AbstractController {

    @Autowired
    ListRepository listrepository;

    @Autowired
    UserRepository userrepository;

    @Autowired
    NotificationService notificationservice;

    @Autowired
    ListSongRepository listsongrepository;

    @Autowired
    ListAlbumRepository listalbumrepository;

    @Autowired
    private ListService listservice;

    @Autowired
    private SpotifyImportService spotifyimportservice;

    @Autowired
    private ObjectMapper objectmapper;


    /**
     * CREATE (POST) - Creates a new list for a given user
     * @param body (JSON) of ListRequest type
     * @param session HttpSession containing "uid"
     * @return UserList that the user just created
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserList createUserList(@RequestBody ListRequest body, HttpSession session){
        Integer uid = getSessionUid(session);

        //verify owner of list (user)
        User owner = userrepository.findById(uid).orElseThrow(
                ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        //create new list
        UserList list = new UserList();
        list.setOwner(owner);
        list.setTitle(body.getTitle());
        list.setDescription(body.getDescription());
        //list.setCoverImage(body.getCoverImage());
        list.setPrivacy(body.getPrivacy());

        UserList saved = listrepository.save(list);

        notificationservice.notifyFollowersOfNewList(saved);

        return saved;
    }

    /**
     * READ (GET) - Get one UserList
     * @param listId id of the list
     * @param session HttpSession containing "uid"
     * @return list matching "listId"
     */
    @GetMapping("/{listId}")
    public UserList getUserList(@PathVariable Integer listId, HttpSession session){
        Integer uid = getSessionUid(session);

        UserList list = listrepository.findById(listId).orElseThrow(
                ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));

        //403 if user accessing list is not the owner and the list is private
        if(!list.getOwner().getId().equals(uid) && (list.getPrivacy() == UserList.Privacy.PRIVATE)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this list");
        }
        return list;
    }

    /**
     * UPDATE (PUT) - Updates a user's list
     * @param listId of the list to update
     * @param body (JSON) of ListRequest type
     * @return list that was just updated
     */
    @PutMapping("/{listId}")
    public UserList updateUserList(@PathVariable Integer listId, @RequestBody ListRequest body, HttpSession session){
        //validate session user
        Integer uid = getSessionUid(session);

        //verify owner of list (user)
        User owner = userrepository.findById(uid).orElseThrow(
                ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserList list = listrepository.findById(listId).orElseThrow(
                ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));

        if(!list.getOwner().getId().equals(uid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your list");
        }

        //decide which field is updated
        if(body.getTitle() != null){
            list.setTitle(body.getTitle());
        }
        if(body.getDescription() != null){
            list.setDescription(body.getDescription());
        }
        //picture handling
        if(body.getCoverImage() != null){
            String pictureBase64 = String.valueOf(body.getCoverImage());

            if(pictureBase64 != null && !pictureBase64.isBlank()){
                byte[] bytes;
                try {
                    bytes = Base64.getDecoder().decode(pictureBase64);
                } catch(IllegalArgumentException e){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid picture data (not base64)");
                }
                if(bytes.length > 5_000_000){
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Cover image is too large");
                }
                list.setCoverImage(bytes);
            }
            //IF NULL/BLANK, DO NOTHING, KEEP EXISTING PICTURE
        }

        if(body.getPrivacy() != null){
            list.setPrivacy(body.getPrivacy());
        }

        //updated at
        list.setUpdatedAt(LocalDateTime.now());

        //saves new list in repo, and returns newly updated list
        return listrepository.save(list);
    }

    /**
     * DELETE (DELETE) - Deletes a user's list
     * @param listId id of the list to delete
     * @param session HttpSession containing "uid"
     */
    @Transactional
    @DeleteMapping("/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserList(@PathVariable Integer listId, HttpSession session){

        UserList list = listrepository.findById(listId).orElseThrow(
                ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));

        // users can only edit their own lists, unless they are administrators
        Integer userId = getSessionUid(session);
        String userType = (String)session.getAttribute("type");
        if (!list.getOwner().getId().equals(userId) && !userType.equals("admin")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your list");
        }

        listsongrepository.deleteAllByList_ListId(listId);
        listalbumrepository.deleteAllByList_ListId(listId);

        listrepository.delete(list);
    }

    /**
     * LIST (GET) - Get all User's lists (private and public)
     * @param session HttpSession containing "uid"
     * @return list of all user's lists
     */
    @GetMapping
    public List<UserList> getMyLists(HttpSession session){
        Integer uid = getSessionUid(session);

        return listrepository.findAllByOwner_Id(uid);
    }

    /**
     * LIST (GET) - Get all User's list (public only)
     * @param ownerId of user's lists we want to view
     * @return list of all user's public lists
     */
    @GetMapping("/user/{ownerId}/public")
    public List<UserList> getUserPublicLists(@PathVariable Integer ownerId){
        return listrepository.findAllByOwner_IdAndPrivacy(ownerId, UserList.Privacy.PUBLIC);
    }


    /**
     * CREATE (POST) - Adds a song to a user's list
     * @param listId id of the target list
     * @param songId id of the song to add
     * @param body if track is from Spotify
     * @param session HttpSession containing "uid"
     * @return a Map containing the operation status and confirmation details
     */
    @PostMapping("/{listId}/songs/{songId}")
    public Map<String, Object> addSong(@PathVariable Integer listId,
                                       @PathVariable Integer songId,
                                       @RequestBody(required = false) Map<String, String> body,
                                       HttpSession session){
        Integer uid = getSessionUid(session);

        //if Spotify data provided, create/get track first
        if(body != null && body.get("spotifyData") != null){
            try {
                JsonNode spotifyJson = objectmapper.readTree(body.get("spotifyData"));
                Track track = spotifyimportservice.getOrCreateTrack(spotifyJson);
                songId = track.getId();
            } catch (JsonProcessingException e){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Spotify data");
            }
        }

        listservice.addSong(listId, uid, songId);
        return Map.of("status", 200, "message", "Song added", "listId", listId, "songId", songId);
    }

    /**
     * DELETE (DELETE) - Deletes a song from a user's list
     * @param listId id of the target list
     * @param songId id of the song to remove
     * @param session HttpSession containing "uid"
     * @return a Map containing the operation status and confirmation details
     */
    @DeleteMapping("/{listId}/songs/{songId}")
    public Map<String, Object> removeSong(@PathVariable Integer listId, @PathVariable Integer songId, HttpSession session){
        Integer uid = getSessionUid(session);
        listservice.removeSong(listId, uid, songId);
        return Map.of("status", 200, "message", "Song removed successfully", "listId", listId, "songId", songId);
    }

    /**
     * CREATE (POST) - Adds an album to a user's list
     * @param listId id of the target list
     * @param albumId id of the album to add
     * @param body if album is from Spotify
     * @param session HttpSession containing "uid"
     * @return a Map containing the operation status and confirmation details
     */
    @PostMapping("/{listId}/albums/{albumId}")
    public Map<String, Object> addAlbum(@PathVariable Integer listId,
                                        @PathVariable Integer albumId,
                                        @RequestBody(required = false) Map<String, String> body,
                                        HttpSession session){
        Integer uid = getSessionUid(session);

        //if Spotify data provided, create/get track first
        if(body != null && body.get("spotifyData") != null){
            try {
                JsonNode spotifyJson = objectmapper.readTree(body.get("spotifyData"));
                Album album = spotifyimportservice.getOrCreateAlbum(spotifyJson);
                albumId = album.getId();
            } catch (JsonProcessingException e){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Spotify data");
            }
        }

        listservice.addAlbum(listId, uid, albumId);
        return Map.of("status", 200, "message", "Album added", "listId", listId, "albumId", albumId);
    }

    /**
     * DELETE (DELETE) - Deletes an album from a user's list
     * @param listId id of the target list
     * @param albumId id of the album to remove
     * @param session HttpSession containing "uid"
     * @return a Map containing the operation status and confirmation details
     */
    @DeleteMapping("/{listId}/albums/{albumId}")
    public Map<String, Object> removeAlbum(@PathVariable Integer listId, @PathVariable Integer albumId, HttpSession session){
        Integer uid = getSessionUid(session);
        listservice.removeAlbum(listId, uid, albumId);
        return Map.of("status", 200, "message", "Album removed successfully", "listId", listId, "albumId", albumId);
    }

    /**
     *  LIST (GET) - List all songs in a playlist
     * @param listId id of the target list
     * @param session HttpSession containing "uid"
     * @return the list of songs in a list
     */
    @GetMapping("/{listId}/tracks")
    public List<TrackDTO> getSongsInList(@PathVariable Integer listId, HttpSession session){
        Integer uid = getSessionUid(session);

        return listservice.getSongsForList(listId, uid);
    }

}
