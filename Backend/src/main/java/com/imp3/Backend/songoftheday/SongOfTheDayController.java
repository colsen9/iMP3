package com.imp3.Backend.songoftheday;

import com.imp3.Backend.music.Track;
import com.imp3.Backend.music.TrackRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("sotd")
public class SongOfTheDayController {

    // ============================================================
    // Repos
    // ============================================================

    @Autowired
    SongOfTheDayRepository sotdrepository;
    @Autowired
    TrackRepository trackrepository;

    // ============================================================
    // GET Methods
    // ============================================================

    // get today's SotD; make one if none exists
    @GetMapping("today")
    public Optional<SongOfTheDay> getToday() {

        LocalDate today = LocalDate.now();

        // make today's song of the day, if one doesn't exist
        if (sotdrepository.findByDate(today).isEmpty()) {
            SongOfTheDay sotd = new SongOfTheDay();

            // get all tracks, pick a random one
            Random rand = new Random();
            List<Track> tracks = trackrepository.findAll();
            Track track = tracks.get(rand.nextInt(tracks.size()));

            // set the values and save them to the repo
            sotd.setTrackId(track.getId());
            sotd.setDate(today);
            sotd.setScore(0);
            sotdrepository.save(sotd);
        }
        return sotdrepository.findByDate(today);
    }

    // get a specific SotD
    @GetMapping("{date}")
    public Optional<SongOfTheDay> findById(@PathVariable LocalDate date) {
        return sotdrepository.findByDate(date);
    }

    // get all SotDs
    @GetMapping("all")
    public List<SongOfTheDay> getAll() {
        return sotdrepository.findAll();
    }

    // ============================================================
    // POST Methods
    // ============================================================

    @PostMapping("/{date}/{trackId}")
    public Map<String, Integer> insertSotD(@PathVariable LocalDate date, @PathVariable Integer trackId, HttpSession session) {

        // only admins can schedule a specific song of the day
        String userType = (String)session.getAttribute("type");
        if (!userType.equals("admin")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User Does Not Have Administrative Rights");
        }

        // make the Song of the Day object
        SongOfTheDay sotd = new SongOfTheDay();

        // check if the trackId exists, set in SotD if so
        Optional<Track> track = trackrepository.findById(trackId);
        if (track.isPresent()) {
            sotd.setTrackId(trackId);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found: " + trackId);
        }

        // check if the date is valid (YYYY-MM-DD), set if so
        if (sotdrepository.findByDate(date).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Date Already Assigned");
        }
        sotd.setDate(date);

        // start the score at 0
        sotd.setScore(0);

        // save the SotD
        sotdrepository.save(sotd);

        // if we made it here, we succeeded
        return Map.of("status", 200, "trackId", sotd.getId());
    }

    // ============================================================
    // PUT Methods
    // ============================================================

    // user upvotes song of the day
    @PutMapping("/upvote")
    public Map<String, Integer> upvote(HttpSession session) {

        // get current userId from session
        Integer userId = (Integer) session.getAttribute("uid");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not Logged In");
        }

        // cast the vote via helper function
        castVote(userId, 1);

        // if we made it here, we succeeded
        return Map.of("status", 200);
    }

    // user downvotes song of the day
    @PutMapping("/downvote")
    public Map<String, Integer> downvote(HttpSession session) {

        // get current userId from session
        Integer userId = (Integer) session.getAttribute("uid");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not Logged In");
        }

        // cast the vote via helper function
        castVote(userId, -1);

        // if we made it here, we succeeded
        return Map.of("status", 200);
    }

    // helper function to cast a vote
    public void castVote(Integer userId, Integer score) {

        // get today's Song of the Day, throw an error if it doesn't exist
        LocalDate today = LocalDate.now();
        if (sotdrepository.findByDate(today).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Song of the Day Today");
        }
        SongOfTheDay sotd = sotdrepository.findByDate(today).get();

        // check if user has already voted
        if (sotd.getVoted() == null) {
            sotd.setVoted(new ArrayList<>());
        }
        if (sotd.getVoted().contains(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User Already Voted");
        }

        // add user to the voting roll
        sotd.getVoted().add(userId);

        // cast their vote
        sotd.setScore(sotd.getScore() + score);

        // save the result
        sotdrepository.save(sotd);

    }

    // ============================================================
    // DELETE Methods
    // ============================================================

    @DeleteMapping("{date}")
    public Map<String, Integer> delete(@PathVariable LocalDate date, HttpSession session) {

        // only admins can delete a song of the day entry
        String userType = (String)session.getAttribute("type");
        if (!userType.equals("admin")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User Does Not Have Administrative Rights");
        }

        // check if song of the day exists for this date
        if (sotdrepository.findByDate(date).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Song of the Day On This Day");
        }
        SongOfTheDay sotd = sotdrepository.findByDate(date).get();

        // delete this entry
        sotdrepository.delete(sotd);

        // if we made it here, we succeeded
        return Map.of("status", 200);
    }
}
