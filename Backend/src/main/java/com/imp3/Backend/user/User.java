package com.imp3.Backend.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

@Getter //Lombok Shortcut for generated getter methods
@Setter //for generated setter methods
@NoArgsConstructor //default constructor
@AllArgsConstructor
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name= "users")
public class User {

    // primary key, internal value
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // required user details
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @JsonProperty(access=JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "type", nullable = false, unique = false)
    private String type;

    //optional user details
    @Column(name = "firstname", nullable = true, unique = false)
    private String firstname;

    @Column(name = "lastname", nullable = true, unique = false)
    private String lastname;

    @Column(name = "picture", nullable = true, unique = false, columnDefinition = "MEDIUMBLOB")
    private byte[] picture;

    @Column(name = "bio", nullable = true, unique = false, columnDefinition = "TEXT")
    private String bio;

    //For linking spotify account (OAuth fields)
    @JsonIgnore
    @Column(name = "spotify_user_id")
    private String spotifyUserId;

    @JsonIgnore
    @Column(name = "spotify_refresh_token", length = 500)
    private String spotifyRefreshToken;

    @JsonIgnore
    @Column(name = "spotify_access_token", length = 500)
    private String spotifyAccessToken;

    @JsonIgnore
    @Column(name = "spotify_access_token_expiry")
    private Instant spotifyAccessTokenExpiry;

    // helper functions
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        User user = (User) object;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}

