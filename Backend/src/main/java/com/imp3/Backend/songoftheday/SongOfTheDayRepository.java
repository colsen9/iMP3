package com.imp3.Backend.songoftheday;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SongOfTheDayRepository extends JpaRepository<SongOfTheDay, Integer>  {

    // get a specific SotD (by date)
    Optional<SongOfTheDay> findByDate(LocalDate date);

    // get all SotD
    List<SongOfTheDay> findAll();

}