package com.imp3.Backend.tag;

import com.imp3.Backend.common.AbstractController;
import com.imp3.Backend.follow.FollowRepository;
import com.imp3.Backend.user.User;
import com.imp3.Backend.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/usertag")
public class UserTagController extends AbstractController {

    @Autowired
    UserTagRepository usertagrepository;

    @Autowired
    TagRepository tagrepository;

    @Autowired
    UserRepository userrepository;

    @Autowired
    FollowRepository followrepository;

    @Autowired
    TagService tagservice;

    /**
     * LIST (GET) - List all tags for the logged-in user
     * @param session containing "uid"
     * @return list of user's tags
     */
    @GetMapping
    public List<UserTag> getMyTags(HttpSession session){
        Integer uid = getSessionUid(session);

        return usertagrepository.findByUser_Id(uid);
    }

    /**
     * READ (GET) - Get one specific tag for the user
     * @param userTagId of the user tag
     * @param session session containing "uid"
     * @return the user tag
     */
    @GetMapping("/{userTagId}")
    public UserTag getMyTag(@PathVariable Integer userTagId, HttpSession session){
        Integer uid = getSessionUid(session);

        UserTag userTag = usertagrepository.findById(userTagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));

        if(!userTag.getUser().getId().equals(uid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your tag");
        }

        return userTag;
    }

    /**
     * LIST (GET) - List tags for a specific user (respects privacy)
     * @param profileUid of the user whose tags to view
     * @param session containing "uid"
     * @return list of viewable userTags
     */
    @GetMapping("/users/{profileUid}")
    public List<UserTag> getUserTags(@PathVariable Integer profileUid, HttpSession session){
        Integer uid = getSessionUid(session);

        //verify user exists
        userrepository.findById(profileUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        //if viewing own profile, view all
        if(uid.equals(profileUid)){
            return usertagrepository.findByUser_Id(profileUid);
        }

        //check if mutuals
        boolean areMutuals = followrepository.existsByFollower_IdAndFollowed_Id(uid, profileUid)
                && followrepository.existsByFollower_IdAndFollowed_Id(profileUid, uid);

        //filter based on privacy
        return usertagrepository.findByUser_Id(profileUid).stream()
                .filter(ut -> {
                    if(ut.getPrivacy() == UserTag.Privacy.PUBLIC){
                        return true;
                    }
                    if(ut.getPrivacy() == UserTag.Privacy.MUTUALS && areMutuals){
                        return true;
                    }
                    return false;

                }).collect(Collectors.toList());
    }

    /**
     * CREATE (POST) - Add a tag to the logged-in user
     * @param request containing tag details
     * @param session containing "uid"
     * @return the created user tag
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserTag addTagToUser(@RequestBody UserTagRequest request, HttpSession session){
        Integer uid = getSessionUid(session);

        User user = userrepository.findById(uid)
                .orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        //validate tagId is provided
        if(request.getTagId() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag ID is required");
        }

        Tag tag = tagrepository.findById(request.getTagId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));

        //check if user already has this tag
        if(usertagrepository.existsByUser_IdAndTag_TagId(uid, request.getTagId())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have this tag");
        }

        UserTag u = new UserTag();
        u.setUser(user);
        u.setTag(tag);
        u.setTagType(request.getTagType() != null ? request.getTagType() : UserTag.TagType.CUSTOM);
        u.setPrivacy(request.getPrivacy() != null ? request.getPrivacy() : UserTag.Privacy.PUBLIC);

        if(request.getConfidence() != null ){
            u.setConfidence(request.getConfidence());
        }

        if(request.getSource() != null){
            u.setSource(request.getSource());
        }

        return usertagrepository.save(u);
    }

    /**
     *  UPDATE (PUT) - Update a user's tag (privacy, confidence, etc.,
     * @param userTagId of the user tag
     * @param request containing the updated fields
     * @param session containing "uid"
     * @return the updated user tag
     */
    @PutMapping("/{userTagId}")
    public UserTag updateUserTag(@PathVariable Integer userTagId,
                                 @RequestBody UserTagRequest request, HttpSession session ) {

        UserTag u = usertagrepository.findById(userTagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User tag not found"));

        // users can only edit their own tags, unless they are administrators
        Integer userId = (Integer)session.getAttribute("uid");
        String userType = (String)session.getAttribute("type");
        if (!u.getUser().getId().equals(userId) && !userType.equals("admin")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your tag");
        }

        if(request.getPrivacy() != null){
            u.setPrivacy(request.getPrivacy());
        }

        if(request.getConfidence() != null){
            u.setConfidence(request.getConfidence());
        }

        if(request.getSource() != null){
            u.setSource(request.getSource());
        }

        if(request.getTagType() != null){
            u.setTagType(request.getTagType());
        }

        return usertagrepository.save(u);
    }

    /**
     * DELETE (DELETE) - Remove a tag from the logged-in user
     * @param userTagId of the user
     * @param session containing "uid"
     */
    @DeleteMapping("/{userTagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTagFromUser(@PathVariable Integer userTagId, HttpSession session){

        UserTag u = usertagrepository.findById(userTagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));

        // users can only delete their own tags, unless they are administrators
        Integer userId = (Integer)session.getAttribute("uid");
        String userType = (String)session.getAttribute("type");
        if (!u.getUser().getId().equals(userId) && !userType.equals("admin")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your tag");
        }

        usertagrepository.delete(u);
    }

    /**
     * LIST (POST) - Generate tags from Spotify listening history
     * @param session containing "iud"
     * @return list of generated tags
     */
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<UserTag> generateTags(HttpSession session){
        Integer uid = getSessionUid(session);

        User user = userrepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if(user.getSpotifyAccessToken() == null || user.getSpotifyAccessToken().isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Spotify account not linked");
        }

        return tagservice.generateTagsFromSpotify(user);
    }


    /**
     * READ (GET) - Generate a profile summary based on user's tags
     * @param session containing "uid"
     * @return summary string
     */
    @GetMapping("/summary")
    public Map<String, String> getProfileSummary(HttpSession session){
        Integer uid = getSessionUid(session);

        String summary = tagservice.generateProfileSummary(uid);
        return Map.of("summary", summary);
    }

    /**
     * READ (GET) - Get another user's profile summary (public tags only)
     * @param profileUid of the user
     * @param session containing "uid"
     * @return summary string
     */
    @GetMapping("/summary/{profileUid}")
    public Map<String, String> getUserProfileSummary(@PathVariable Integer profileUid, HttpSession session){
        getSessionUid(session);

        userrepository.findById(profileUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String summary = tagservice.generateProfileSummary(profileUid);
        return Map.of("summary", summary);
    }



}
