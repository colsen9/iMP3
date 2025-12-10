package com.imp3.Backend.reviews;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name= "reviews")

public class Review {

    // table columns: id, user uuid, album uuid, song uuid, rating, review text

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "userId", nullable = false, unique = false)
    private Integer userId;

    @Column(name = "albumId", nullable = false, unique = false)
    private Integer albumId;

    @Column(name = "songId", nullable = true, unique = false)
    private Integer songId;

    @Column(name = "rating", nullable = false, unique = false)
    private Integer rating;

    @Column(name = "review", nullable = true, unique = false, columnDefinition = "TEXT")
    private String review;

    @Transient
    private String spotifyData;

    @Transient
    private String spotifyType;

    public Review(Integer userId, Integer albumId, Integer songId, Integer rating, String review) {
        this.userId = userId;
        this.albumId = albumId;
        this.songId = songId;
        this.rating = rating;
        this.review = review;
    }

}
