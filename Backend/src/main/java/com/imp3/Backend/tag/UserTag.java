package com.imp3.Backend.tag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.imp3.Backend.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "user_tags", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "tag_id"}) // prevent duplicate user-tag pairs
})
public class UserTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userTagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"userTags", "password", "spotifyAccessToken", "spotifyRefreshToken"})
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(name = "tag_type", nullable = false)
    private TagType tagType;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "privacy", nullable = false)
    @Enumerated(EnumType.STRING)
    private Privacy privacy = Privacy.PUBLIC;

   @Column(name = "source")
    private String source;

   @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

   @Column(name = "updated_at")
    private LocalDateTime updatedAt;

   @PrePersist
    protected void onCreate(){
       this.createdAt = LocalDateTime.now();
       this.updatedAt = LocalDateTime.now();
   }

   @PreUpdate
    protected void onUpdate(){
       this.updatedAt = LocalDateTime.now();
   }

   public UserTag(User user, Tag tag, TagType tagType){
       this.user = user;
       this.tag = tag;
       this.tagType = tagType;
   }

   public enum TagType {
       AUTO,
       CUSTOM
   }

   public enum Privacy {
       PUBLIC,
       PRIVATE,
       MUTUALS
   }

}
