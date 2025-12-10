package coms309.data;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Controller used to showcase Create and Read from a LIST
 *
 * @author Vivek Bengre
 */

@RestController
public class DataController {

    // Note that there is only ONE instance of PeopleController in 
    // Springboot system.
    HashMap<Integer, Person> peopleList = new  HashMap<>();
    HashMap<Integer, Music> musicList = new HashMap<>();

    //CRUDL (create/read/update/delete/list)
    // use POST, GET, PUT, DELETE, GET methods for CRUDL

    // hashmaps for users and albums
    @GetMapping("/people")
    public HashMap<Integer,Person> getAllPersons() {
        return peopleList;
    }

    @GetMapping("/music")
    public HashMap<Integer,Music> getAllMusic() { return musicList; }

    // add a person
    @PostMapping("/people")
    public String createPerson(@RequestBody Person person) {
        person.setId(new Random().nextInt(9999));

        System.out.println(person + ", id: " + person.getId());

        peopleList.put(person.getId(), person);
        String s = "New person "+ person.getFirstName() + " Saved";
        return s;
    }

    // add an album
    @PostMapping("/music")
    public String createAlbum(@RequestBody Music music) {
        music.setId(new Random().nextInt(9999));

        System.out.println(music + ", id: " + music.getId());

        musicList.put(music.getId(), music);
        String s = "New album "+ music.getAlbum() + " Saved";
        return s;
    }

    // get a person's info
    @GetMapping("/people/{id}")
    public Person getPerson(@PathVariable Integer id) {
        Person p = peopleList.get(id);
        return p;
    }

    // get an album's info
    @GetMapping("/music/{id}")
    public Music getMusic(@PathVariable Integer id) {
        Music music = musicList.get(id);
        return music;
    }

    // look up music by name
    @GetMapping("/music/contains")
    public List<Music> getPersonByParam(@RequestParam("name") String name) {
        List<Music> res = new ArrayList<>();
        for (Music music : musicList.values()) {
            if (music.getAlbum().contains(name))
                res.add(music);
        }
        return res;
    }

    // update a user's information
    @PutMapping("/people/{id}")
    public Person updatePerson(@PathVariable Integer id, @RequestBody Person p) {
        peopleList.replace(id, p);
        return peopleList.get(id);
    }

    // delete a user
    @DeleteMapping("/people/{id}")
    public HashMap<Integer, Person> deletePerson(@PathVariable Integer id) {
        peopleList.remove(id);
        return peopleList;
    }

    // delete an album
    @DeleteMapping("/music/{id}")
    public HashMap<Integer, Music> deleteMusic(@PathVariable Integer id) {
        musicList.remove(id);
        return musicList;
    }

    // THIS IS THE ADD FRIEND OPERATION
    // adds a friend, specified in JSON, to person id
    @PostMapping("/people/{id}/friends/{friend}")
    public Integer addFriend(@PathVariable Integer id, @PathVariable Integer friend) {
        Person p = peopleList.get(id);
        p.addFriend(friend);

        return friend;
    }

    // add an album to a user's profile
    @PostMapping("/people/{id}/music/{music}")
    public Integer addMusic(@PathVariable Integer id, @PathVariable Integer music) {
        Person p = peopleList.get(id);
        p.addMusic(music);

        return music;
    }

    // view a person's music list
    @GetMapping("/people/{id}/music")
    public List<Integer> viewMusic(@PathVariable Integer id) {

        // the person whose list we're looking at
        Person p = peopleList.get(id);
        return p.getMusic();
    }

    // THIS VIEWS A PERSON'S FRIEND LIST
    @GetMapping("/people/{id}/friends")
    public List<Integer> viewFriends(@PathVariable Integer id) {

        // the person whose list we're looking at
        Person p = peopleList.get(id);
        return p.getFriends();

        /**
        // populate a list with their friends
        List<Person> friendsList = new ArrayList<>();
        for (Integer friendID : p.getFriends()) {
            Person friend = peopleList.get(friendID);
        }

        // this doesn't work...
        return friendsList; */
    }
} // end of people controller

