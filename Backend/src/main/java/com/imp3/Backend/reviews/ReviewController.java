package com.imp3.Backend.reviews;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imp3.Backend.common.SpotifyImportService;
import com.imp3.Backend.music.Album;
import com.imp3.Backend.music.Track;
import com.imp3.Backend.notification.NotificationService;
import com.imp3.Backend.user.User;
import com.imp3.Backend.user.UserRepository;
import com.imp3.Backend.reviews.Review;
import com.imp3.Backend.reviews.ReviewRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    ReviewRepository reviewrepository;

    @Autowired
    NotificationService notificationservice;

    @Autowired
    private SpotifyImportService spotifyimportservice;

    @Autowired
    private ObjectMapper objectmapper;

    // ============================================================
    // GET Methods
    // ============================================================

    // get reviews of an album (and not the songs in it)
    @GetMapping("/albums/{albumId}")
    public List<Review> reviewAlbumExclusive(@PathVariable Integer albumId) {
        List<Review> reviews = reviewrepository.findByAlbumId(albumId);
        List <Review> albumReviews = new ArrayList<>();

        // for each review in the list, add the ones with unset songId fields
        for (Review review : reviews) {
            if (review.getSongId() == null) {
                albumReviews.add(review);
            }
        }

        return albumReviews;
    }

    // get reviews of an album and all the songs in it
    @GetMapping("/albums/{albumId}/all")
    public List<Review> reviewAlbum(@PathVariable Integer albumId) {
        return reviewrepository.findByAlbumId(albumId);
    }

    // get reviews of a song
    @GetMapping("/tracks/{songId}")
    public List<Review> reviewSong(@PathVariable Integer songId) {
        return reviewrepository.findBySongId(songId);
    }

    // get reviews from a user
    @GetMapping("/users/{userId}")
    public List<Review> reviewUser(@PathVariable Integer userId) {
        return reviewrepository.findByUserId(userId);
    }

    // get reviews of an album by a specific user
    @GetMapping("/albums/{albumId}/{userId}")
    public List<Review> reviewAlbumByUser(@PathVariable Integer albumId, @PathVariable Integer userId) {
        return reviewrepository.findByAlbumIdAndUserId(albumId, userId);
    }

    // get reviews of a track by a specific user
    @GetMapping("/tracks/{songId}/{userId}")
    public List<Review> reviewSongByUser(@PathVariable Integer songId, @PathVariable Integer userId) {
        return reviewrepository.findBySongIdAndUserId(songId, userId);
    }

    // get a specific review
    @GetMapping("/{reviewId}")
    public Optional<Review> review(@PathVariable Integer reviewId) {
        return reviewrepository.findById(reviewId);
    }

    // get highest ratings
    @GetMapping("/best")
    public List<Review> reviewBest() {
        return reviewrepository.findAllByOrderByRatingDesc();
    }

    // get lowest ratings
    @GetMapping("/worst")
    public List<Review> reviewWorst() {
        return reviewrepository.findAllByOrderByRatingAsc();
    }

    // ============================================================
    // POST Methods
    // ============================================================

    // make a new review
    @PostMapping("/new")
    public Map<String, Integer> reviewNew(@RequestBody Review review) {

        //handle Spotify data if provided
        if(review.getSpotifyData() != null){
            try {
                JsonNode spotifyJson = objectmapper.readTree(review.getSpotifyData());
                String type = review.getSpotifyType() != null ? review.getSpotifyType() : "album";

                if("track".equalsIgnoreCase(type)){
                    Track track = spotifyimportservice.getOrCreateTrack(spotifyJson);
                    review.setSongId(track.getId());
                    //also set albumId from the track's album
                    if(!track.getAlbums().isEmpty()){
                        review.setAlbumId(track.getAlbums().iterator().next().getId());
                    }}
                else {
                    Album album = spotifyimportservice.getOrCreateAlbum(spotifyJson);
                    review.setAlbumId(album.getId());
                }
            } catch (JsonProcessingException e){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Spotify data");
            }
        }

        // record the review in the table
        reviewrepository.save(review);

        //sends notification to followers
        notificationservice.notifyFollowersOfReview(review);

        Integer reviewId = review.getId();

        // if we made it here, we succeeded
        return Map.of("status", 200, "reviewId", reviewId);
    }

    // ============================================================
    // PUT Methods
    // ============================================================

    // edit an existing review
    @PutMapping("/edit")
    public Map<String, Integer> reviewEdit(@RequestBody Review review, HttpSession session) {

        // get user session information
        Integer userId = (Integer)session.getAttribute("uid");
        String userType = (String)session.getAttribute("type");

        // ensure users can only edit reviews they posted themselves, unless they are an admin
        if (userId == null) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"); }
        if (!userId.equals(review.getUserId()) && !userType.equals("admin")) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User does not match"); }

        // save the new review, overwriting the old one
        reviewrepository.save(review);

        Integer reviewId = review.getId();

        // if we made it here, we succeeded
        return Map.of("status", 200, "reviewId", reviewId);
    }

    // ============================================================
    // DELETE Methods
    // ============================================================

    // delete a review
    @DeleteMapping("/delete/{id}")
    public Map<String, Integer> reviewDelete(@PathVariable Integer id, HttpSession session) {

        // get userID out of the session information
        Integer userId = (Integer)session.getAttribute("uid");
        String userType = (String)session.getAttribute("type");

        // pull the review out of the id in the url
        Review review = reviewrepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));

        // ensure users can only delete reviews they posted themselves
        if (userId == null) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"); }
        if (!userId.equals(review.getUserId()) && !userType.equals("admin")) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User does not match"); }

        // delete the review
        reviewrepository.delete(review);

        // if we made it here, we succeeded
        return Map.of("status", 200);
    }


}
