package coms309.people;

import org.springframework.web.bind.annotation.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller used to showcase Create and Read from a LIST
 *
 * @author Vivek Bengre
 */

@RestController
public class PeopleController {

    // Note that there is only ONE instance of PeopleController in 
    // Springboot system.
    HashMap<String, Person> peopleList = new HashMap<>();

    //CRUDL (create/read/update/delete/list)
    // use POST, GET, PUT, DELETE, GET methods for CRUDL

    /**
     * Creates a person. Springboot automatically converts JSON input
     * into a person object & the method below enters it into the list.
     * @return New person (name) saved.
     */
    @PostMapping("/people")
    public  String createPerson(@RequestBody Person person) {
        System.out.println(person);
        peopleList.put(person.getFirstName(), person);
        return "New person "+ person.getFirstName() + " saved";
    }

    /**
     * List -- uses GET
     * This is the list operation -- it gets all of the people in the
     * list and returns it in JSON format. This controller takes no
     * input.
     * @return peopleList
     */
    @GetMapping("/people")
    public  HashMap<String,Person> getAllPersons(@RequestParam(required = false) String lastName) {
        if(lastName == null){
            return  peopleList;
        }
        else{
            HashMap<String, Person> filteredPeople = new HashMap<>();
            for(Map.Entry<String, Person> entry : peopleList.entrySet()){
                if(entry.getValue().getLastName().equalsIgnoreCase(lastName)){
                    filteredPeople.put(entry.getKey(), entry.getValue());
                }
            }
            return filteredPeople;
        }
    }


    /**
     * Read person by first name -- uses GET
     * @return information on person with given name
     */
    @GetMapping("/people/{firstName}")
    public Person getPerson(@PathVariable String firstName) {
         return peopleList.get(firstName);
    }

    /**
     * Get person by param -- uses GET
     * @param name
     * @return list of persons with name
     */
    @GetMapping("/people/contains")
    public List<Person> getPersonByParam(@RequestParam("name") String name) {
        List<Person> res = new ArrayList<>();
        for (Person p : peopleList.values()) {
            if (p.getFirstName().contains(name) || p.getLastName().contains(name))
                res.add(p);
        }
        return res;
    }

    /**
     * Update -- uses PUT
     * @param firstName (the first name of the person to update)
     * @param lastName (optional) new last name
     * @param address (optional) new address
     * @param telephone (optional) new telephone
     * @param jobTitle (optional) new jobTitle
     * @return the updated person, or null if no match was found
     */
    @PutMapping("/people/{firstName}")
    public Person updatePerson(@PathVariable String firstName,
                               @RequestParam(required=false) String lastName,
                               @RequestParam(required=false) String address,
                               @RequestParam(required = false) String telephone,
                               @RequestParam(required = false) String jobTitle
                               ) {
        Person p = peopleList.get(firstName);
        if(p == null){
            return null;
        }
        if(lastName != null) p.setLastName(lastName);
        if(address != null) p.setAddress(address);
        if(telephone != null) p.setTelephone(telephone);
        if(jobTitle != null) p.setjobTitle(jobTitle);
        return p;
    }

    /**
     * Updates person 2 -- uses PUT
     * @param firstName
     * @param p
     * @return person that was updated
     */
    @PutMapping(
            value="/people",
            params = { "firstName" }
    )
    public Person updatePerson2(@RequestParam("firstName") String firstName, @RequestBody Person p) {
        peopleList.replace(firstName, p);
        return peopleList.get(firstName);
    }

    /**
     * Delete person -- uses DELETE
     * Springboot gets the path variable from the URL.
     * @param firstName of person to delete
     * @return the deleted person, or null if not found
     */
    @DeleteMapping("/people/{firstName}")
    public Person deletePerson(@PathVariable String firstName) {
        System.out.println("Deleting person: " + firstName);
        return peopleList.remove(firstName);
    }

}

