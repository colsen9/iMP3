package coms309.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User class
 *
 * @author Sara Theriault
 */
@Getter // Lombok Shortcut for generated getter methods
@Setter // Similarly for setters as well
@NoArgsConstructor //Default constructor

public class User{
    private String id;

    private String username;

    private String email;

    private String spotifyId;

    private String number;

    private Boolean verified;


    public User(String id, String username, String email, String number){
        this.id = id;
        this.username = username;
        this.email = email;
        this.number = number;
        this.verified = false;
    }

    public User(String id, String username, String email, String number, String spotifyId){
        this.id = id;
        this.username = username;
        this.email = email;
        this.number = number;
        this.spotifyId = spotifyId;
        this.verified = false;
    }

    public String getId(){return this.id;}

    public String getEmail(){return this.email;}

    public String spotifyId(){return this.spotifyId;}

    public String getUserName(){return this.username;}

    public String getNumber(){return this.number;}

    public Boolean isVerified(){return this.verified;}
}
