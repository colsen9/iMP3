package coms309.user;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.UUID;

/**
 * UserController for demo 1
 * @author Sara Theriault
 */

@RestController
public class UserController {

    HashMap<String, User> userList = new HashMap<>();

    /**
     * Creates a new user -- uses POST
     * @return new user (id) saved.
     */
    @PostMapping("/users")
    public String createUser(@RequestBody User user) {
        System.out.println(user);
        //generates a random UUID string
        String newID = UUID.randomUUID().toString();
        user.setId(newID);
        userList.put(newID, user);
        return "New user " + user.getId() + " added.";
    }

    /**
     * Read/gets user by ID -- uses GET
     * @param id
     * @return information of the profile with the
     * corresponding ID
     */
    @GetMapping("/users/{id}")
    public User getUserById(@PathVariable String id) {
        return this.userList.get(id);
    }

    /**
     * Read/gets user by username -- uses GET
     * @param username
     * @return information of the profile with the
     * corresponding username
     */
    @GetMapping("/users")
    public User getUserByUsername(@RequestParam(required = false) String username) {
        if(username == null) return null;
        for(User u: userList.values()){
            if(username.equalsIgnoreCase(u.getUserName())){
                return u;
            }
        }
        return null;
    }

    /**
     * Updates the user information -- uses PUT
     * Only update fields that are provided as query params.
     * @param id user ID (UUID) from path
     * @param username (optional)
     * @param email (optional)
     * @param spotifyId (optional)
     * @param number (optional)
     * @param verified (optional)
     * @return updated user, or null if ID not found
     */
    @PutMapping("/users/{id}")
    public User updateUser(@PathVariable String id,
                           @RequestParam(required = false) String username,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String spotifyId,
                           @RequestParam(required = false) String number,
                           @RequestParam(required = false) Boolean verified){
        User u = userList.get(id);
        if(u == null){return null;}

        if(username != null)  u.setUsername(username);
        if(email != null) u.setEmail(email);
        if(spotifyId != null) u.setSpotifyId(spotifyId);
        if(number != null) u.setNumber(number);
        if(verified != null) u.setVerified(verified);

        return u;
    }

    /**
     * Deletes the user by ID number
     * @param id
     */
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable String id){
        userList.remove(id);
    }

    /**
     * Lists all users -- uses GET
     * @return all users in the userList
     */
    @GetMapping("/users/all")
    public List<User> getAllUsers(){
        return new ArrayList<>(userList.values());
    }

}