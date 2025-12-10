package coms309.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.util.ArrayList;

/**
 * Provides the Definition/Structure for the people row
 *
 * @author Vivek Bengre
 */
@Getter // Lombok Shortcut for generating getter methods (Matches variable names set ie firstName -> getFirstName)
@Setter // Similarly for setters as well
@NoArgsConstructor // Default constructor
public class Person {

    private String firstName;
    private String lastName;
    private String address;
    private String telephone;
    private int id;
    List<Integer> friends = new ArrayList<>();
    List<Integer> music = new ArrayList<>();

    public Person(String firstName, String lastName, String address, String telephone, int id) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.telephone = telephone;
        this.id = id;
    }

    @Override
    public String toString() {
        return firstName + " " 
               + lastName + " "
               + address + " "
               + telephone + " "
                + id;
    }

    public List<Integer> getFriends() {
        return friends;
    }

    public List<Integer> getMusic() { return music; }

    public void addFriend(int id) {
        this.friends.add(id);
    }

    public void addMusic(int id) { this.music.add(id); }
}
