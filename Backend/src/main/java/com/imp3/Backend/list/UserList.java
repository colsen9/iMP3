package com.imp3.Backend.list;

import com.imp3.Backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "UserLists")
public class UserList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer listId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "coverImage", nullable = true, unique = false, columnDefinition = "MEDIUMBLOB")
    private byte[] coverImage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Privacy privacy = Privacy.PUBLIC; //public by default

    @PreUpdate
    public void onUpdate(){
        
        this.updatedAt = LocalDateTime.now();
    }

    public enum Privacy {
        PUBLIC,
        PRIVATE
    }

}
