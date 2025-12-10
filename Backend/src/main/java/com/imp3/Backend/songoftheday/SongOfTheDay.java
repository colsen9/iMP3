package com.imp3.Backend.songoftheday;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name= "songoftheday")

// ============================================================
// Song of the Day objects are a track, a score, and a list of users who have voted
// ============================================================

public class SongOfTheDay {

    // ============================================================
    // Internal Table Data
    // ============================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ============================================================
    // Required SotD Details
    // ============================================================

    // ID of track
    @Column(name = "trackId", nullable = false, unique = false)
    private Integer trackId;

    // day of the song of the day, one per day
    @Column(name = "date", nullable = false, unique = true)
    private LocalDate date;

    // score
    @Column(name = "score", nullable = false, unique = false)
    private Integer score;

    // list of userIds who have voted
    // converter automatically handles storing the List<> in MySQL, stored as JSON string
    @Convert(converter = ListOfIntegerConverter.class)
    @Column(columnDefinition = "JSON", nullable = true, unique = false)
    private List<Integer> voted = new ArrayList<>();

}
