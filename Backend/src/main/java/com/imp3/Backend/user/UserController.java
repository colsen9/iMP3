package com.imp3.Backend.user;

import com.imp3.Backend.common.AbstractController;
import com.imp3.Backend.follow.Follow;
import com.imp3.Backend.follow.FollowRepository;
import com.imp3.Backend.notification.NotificationService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController extends AbstractController {

    @Autowired
    UserRepository userrepository;

    @Autowired
    FollowRepository followrepository;

    @Autowired
    NotificationService notificationservice;

    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    /**
     * Creates a new user (uses POST)
     * @param user to be added to the website
     * @return user that was just created
     */
    @PostMapping("/signup")
    public User signUp(@RequestBody User user, HttpSession session){
        //Checks that email has not been used
        if(userrepository.findByEmail(user.getEmail()).isPresent()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        //hashes the password before saving
        user.setPassword(passwordEncoder().encode(user.getPassword()));

        userrepository.save(user);

        session.setAttribute("uid", user.getId());
        session.setAttribute("type", user.getType());

        return user;
    }
    
    // Basic support for logging a user in (uses POST)
    // note: no thought given toward security, basic functionality only
    @PostMapping("/login")
    public Map<String, Integer> login(@RequestBody Map<String, String> authentication, HttpSession session) {

        System.out.println("logging in");

        String email = authentication.get("email");
        String password = authentication.get("password");
        
        // if the user exists...
        if(userrepository.findByEmail(email).isPresent()) {

            // get user as a user object
            User user = userrepository.findByEmail(email).get();

            // banned users cannot log in
            if (user.getType().equals("banned")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No Login, User Banned");
            }
           
            // and if the password matches the stored hash...
            if (passwordEncoder().matches(password, user.getPassword())) {

                // get current user ID
                Integer userID = user.getId();

                // store current user ID and user type in session
                session.setAttribute("uid", user.getId());
                session.setAttribute("type", user.getType());

                // then return a success message
                return Map.of("status", 200, "user", userID);
            }
        }

        // else, return a failure message
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "failure");
    }

    /**
     * Deletes a user that has already been created (uses DELETE)
     * @param id of the user
     * @return user that was just deleted
     */
    @DeleteMapping("/{id}")
    public User deleteUser(@PathVariable Integer id, HttpSession session) {
        //fetch user by ID number or 404 if not found
        User toDelete = userrepository.findById(id).orElseThrow(()-> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"User not found"));

        // determine if the request is from an admin or the user themself
        Integer sessionId = getSessionUid(session);
        String privilege = (String)session.getAttribute("type");
        if (!sessionId.equals(id) && !privilege.equals("admin")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }

        userrepository.delete(toDelete);

        //returns the deleted user (password is hidden)
        return toDelete;
    }

    /**
     * Returns a list of all users
     * @return list of all users on the website
     */

    @GetMapping("/all")
    public List<User> getAllUsers(){
        return userrepository.findAll();
    }

    /**
     * Follow a user
     * @param id of the user they want to follow
     * @return user that they now follow
     */
    @PostMapping("/{id}/follow")
    public User followUser(@PathVariable Integer id, HttpSession session){

        //confirm the user who is doing the following
        Integer currentId = (Integer)session.getAttribute("uid");
        if(currentId == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        //ensures the user cannot follow themselves
        if(currentId.equals(id)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot follow yourself");
        }

        //fetch user by ID number or 404 if not found
        User toFollow = userrepository.findById(id).orElseThrow(()-> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        //confirm the user doesn't already follow user toFollow
        if(followrepository.existsByFollower_IdAndFollowed_Id(currentId, id)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already following");
        }

        Follow following = new Follow();

        User follower = userrepository.findById(currentId).orElseThrow(()-> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        following.setFollower(follower);
        following.setFollowed(toFollow);
        followrepository.save(following);

        notificationservice.notifyUserFollow(follower, toFollow);

        return toFollow;
    }


    /**
     * Unfollow a user
     * @param id of the user they want to unfollow
     * @return user that was just unfollowed
     */
    @DeleteMapping("/{id}/follow")
    public User unfollowUser(@PathVariable Integer id, HttpSession session){
        //confirm user doing the unfollowing
        Integer currentId = getSessionUid(session);

        //fetch by user ID or 404 if not found
        User toUnfollow = userrepository.findById(id).orElseThrow(()-> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found"));

        var f = followrepository.findByFollower_IdAndFollowed_Id(currentId, id)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not following"));

        followrepository.delete(f);

        return toUnfollow;
    }

    /**
     * Returns a list of followers for the given user (by ID)
     * @param id of the user
     * @return followers for that user (in a list)
     */
    @GetMapping("/{id}/followers")
    public List<User> getFollowerS(@PathVariable Integer id){
        //fetch by user ID, or 404 if not found
        userrepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return followrepository.getFollowersOf(id);
    }

    /**
     * Returns list of users someone is following (by given ID)
     * @param id of the user
     * @return followed users (in a list)
     */
    @GetMapping("/{id}/following")
    public List<User> getFollowing(@PathVariable Integer id){
        //fetch by user id, or 404 if not found
        userrepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return followrepository.getFollowingOf(id);
    }

    /**
     * Returns list of user's mutuals (they are following each other)
     * @param id of the user
     * @return mutuals of the user
     */
    @GetMapping("/{id}/mutuals")
    public List<User> getMutuals(@PathVariable Integer id){
        //fetch by user id, or 404 if not found
        userrepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<User> following = followrepository.getFollowingOf(id);
        List<User> followers = followrepository.getFollowersOf(id);
        List<User> mutuals = new ArrayList<>();

        for(User follower : followers){
            for(User followed : following){
                if(follower != null && followed != null && follower.getId().equals(followed.getId())
                                && !follower.getId().equals(id)){
                    mutuals.add(follower);
                }
            }
        }

        return mutuals;
    }

    /**
     *  Updates the user's information (minus password) - updated by Sara on 11/24/2025
     * @param id of the user
     * @param body changes to update
     * @param session containing "uid"
     * @return 200 ok if updated correctly
     */
    @PutMapping("/{id}")
    public Map<String, Integer> changeUserData(@PathVariable Integer id, @RequestBody Map<String, String> body, HttpSession session) {

        // determine if the session is allowed to edit this information
        Integer sessionId = getSessionUid(session);
        String privilege = (String)session.getAttribute("type");
        if (!sessionId.equals(id) && !privilege.equals("admin")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }
        User user = userrepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        //ONLY UPDATE THE PRESENT FIELDS INSTEAD OF OVERWRITING THE FIELDS THAT ARE NOT PROVIDED TO BE NULL

//        System.out.println("session type: " + session.getAttribute("type"));

        if(body.containsKey("email")){
            user.setEmail(body.get("email"));
        }

        if(body.containsKey("username")){
            user.setUsername(body.get("username"));
        }

        if(body.containsKey("type") && session.getAttribute("type").equals("admin")){
            user.setType(body.get("type"));
        }

        if(body.containsKey("firstname")){
            user.setFirstname(body.get("firstname"));
        }

        if(body.containsKey("lastname")){
            user.setLastname(body.get("lastname"));
        }

        if (body.containsKey("bio")) {
            user.setBio(body.get("bio"));
        }

        //picture handling -- ADDED CHECKS TO PREVENT 500s
        if(body.containsKey("picture")){
            String pictureBase64 = body.get("picture");

            if(pictureBase64 != null && !pictureBase64.isBlank()){
                byte[] bytes;
                try {
                    bytes = Base64.getDecoder().decode(pictureBase64);
                } catch(IllegalArgumentException e){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid picture data (not base64)");
                }
                if(bytes.length > 5_000_000){
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Profile picture is too large");
                }
                user.setPicture(bytes);
            }
            //IF NULL/BLANK, DO NOTHING, KEEP EXISTING PICTURE
        }

        userrepository.save(user);

        return Map.of("status", 200);
    }


    // Change user's password
    @PutMapping("{id}/password")
    public Map<String, Integer> changePassword(@PathVariable Integer id, @RequestBody Map<String, String> authentication, HttpSession session) {
        String password_old = authentication.get("password_old");
        String password_new = authentication.get("password_new");

//        // get current user from session's ID number
//        Integer id = (Integer)session.getAttribute("uid");
//        if (id == null) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"); }
//        User user = userrepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // get relevant user from url
        User user = userrepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // if password_old matches user's stored password, set their new password
        if (passwordEncoder().matches(password_old, user.getPassword())) {
            user.setPassword(passwordEncoder().encode(password_new));
            userrepository.save(user);
        }

        // if we made it here, we succeeded
        return Map.of("status", 200);
    }

    // get user's profile picture
    @GetMapping("{id}/picture")
    public byte[] getProfilePic(@PathVariable Integer id) {

        User user = userrepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return user.getPicture();
    }

    // List all of User's information (except password)
    @GetMapping("/{id}")
    public User getUserInfo(@PathVariable Integer id) {

        // Find user by ID, with an error catch for java reasons
        User user = userrepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // return the user, get automatically formatted as JSON
        return user;
    }


    // search by partial name match
    @GetMapping("search/{name}")
    public List<User> searchNames(@PathVariable String name) {

        // start with an array list
        List<User> matches = new ArrayList<>();

        // add all matching results
        matches.addAll(userrepository.findByFirstnameStartingWith(name));
        matches.addAll(userrepository.findByLastnameStartingWith(name));
        matches.addAll(userrepository.findByUsernameStartingWith(name));

        // remove duplicates by converting to a hashset and back
        List<User> clean = new ArrayList<>(new HashSet<>(matches));

        // return the clean list
        return clean;
    }

    // ban a user
    @PutMapping("ban/{userId}")
    public Map<String, Integer> banUser(@PathVariable Integer userId, HttpSession session) {

        // someone's in trouble
        User user = userrepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // only admins can proceed
        String privilege = (String)session.getAttribute("type");
        if (!privilege.equals("admin")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }

        // change user type to blocked
        user.setType("banned");
        user.setFirstname("Banned");
        user.setLastname("User");

        userrepository.save(user);

        // if we made it here, we succeeded
        return Map.of("status", 200);
    }

    // unban a user
    @PutMapping("unban/{userId}")
    public Map<String, Integer> unbanUser(@PathVariable Integer userId, HttpSession session) {

        // someone's in trouble
        User user = userrepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // only admins can proceed
        String privilege = (String)session.getAttribute("type");
        if (!privilege.equals("admin")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }

        // change user type to blocked
        user.setType("user");
        user.setFirstname("Unbanned");
        user.setLastname("User");

        userrepository.save(user);

        // if we made it here, we succeeded
        return Map.of("status", 200);
    }

}
