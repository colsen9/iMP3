package coms309.people;

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
public class PeopleController {

    // Note that there is only ONE instance of PeopleController in 
    // Springboot system.
    HashMap<Integer, Person> peopleList = new  HashMap<>();

    //CRUDL (create/read/update/delete/list)
    // use POST, GET, PUT, DELETE, GET methods for CRUDL

    // THIS IS THE LIST OPERATION
    // gets all the people in the list and returns it in JSON format
    // This controller takes no input. 
    // Springboot automatically converts the list to JSON format 
    // in this case because of @ResponseBody
    // Note: To LIST, we use the GET method
    @GetMapping("/people")
    public  HashMap<Integer,Person> getAllPersons() {
        return peopleList;
    }

    // THIS IS THE CREATE OPERATION
    // springboot automatically converts JSON input into a person object and 
    // the method below enters it into the list.
    // It returns a string message in THIS example.
    // Note: To CREATE we use POST method
    @PostMapping("/people")
    public String createPerson(@RequestBody Person person) {
        person.setId(new Random().nextInt(9999));

        System.out.println(person + ", id: " + person.getId());

        peopleList.put(person.getId(), person);
        String s = "New person "+ person.getFirstName() + " Saved";
        return s;
    }

    // THIS IS THE READ OPERATION
    // Springboot gets the PATHVARIABLE from the URL
    // We extract the person from the HashMap.
    // springboot automatically converts Person to JSON format when we return it
    // Note: To READ we use GET method
    @GetMapping("/people/{id}")
    public Person getPerson(@PathVariable Integer id) {
        Person p = peopleList.get(id);
        return p;
    }

    // THIS IS A GET METHOD
    // RequestParam is expected from the request under the key "name"
    // returns all names that contains value passed to the key "name"
    @GetMapping("/people/contains")
    public List<Person> getPersonByParam(@RequestParam("name") String name) {
        List<Person> res = new ArrayList<>(); 
        for (Person p : peopleList.values()) {
            if (p.getFirstName().contains(name) || p.getLastName().contains(name))
                res.add(p);
        }
        return res;
    }

    // THIS IS THE UPDATE OPERATION
    // We extract the person from the HashMap and modify it.
    // Springboot automatically converts the Person to JSON format
    // Springboot gets the PATHVARIABLE from the URL
    // Here we are returning what we sent to the method
    // Note: To UPDATE we use PUT method
    @PutMapping("/people/{id}")
    public Person updatePerson(@PathVariable Integer id, @RequestBody Person p) {
        peopleList.replace(id, p);
        return peopleList.get(id);
    }

    // THIS IS THE DELETE OPERATION
    // Springboot gets the PATHVARIABLE from the URL
    // We return the entire list -- converted to JSON
    // Note: To DELETE we use delete method
    
    @DeleteMapping("/people/{id}")
    public HashMap<Integer, Person> deletePerson(@PathVariable Integer id) {
        peopleList.remove(id);
        return peopleList;
    }

    // THIS IS THE ADD FRIEND OPERATION
    // adds a friend, specified in JSON, to person id
    @PostMapping("/people/{id}/{friend}")
    public Integer addFriend(@PathVariable Integer id, @PathVariable Integer friend) {
        Person p = peopleList.get(id);
        p.addFriend(friend);

        return friend;
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

