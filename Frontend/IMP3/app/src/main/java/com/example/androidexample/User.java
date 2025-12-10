/** @author Cayden Olsen **/

package com.example.androidexample;

public class User {
    private String username;
    private int id;
    private byte[] picture;
    private String type;

    public User(String username, int id, byte[] picture, String type) {
        this.username = username;
        this.id = id;
        this.picture = picture;
        this.type = type;
    }

    public String getUsername() { return username; }
    public int getId() { return id; }
    public byte[] getPicture() { return picture; }
    public String getType() { return type; }
}

