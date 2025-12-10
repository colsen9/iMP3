package com.imp3.Backend.reviews;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // find reviews by various IDs
//    Review findById(Integer id);
    List<Review> findByUserId(Integer userId);
    List<Review> findByAlbumId(Integer albumId);
    List<Review> findBySongId(Integer songId);

    // find reviews by multiple IDs
    List<Review> findByAlbumIdAndUserId(Integer albumId, Integer userId);
    List<Review> findBySongIdAndUserId(Integer songId, Integer userId);

    // return all reviews by either highest or lowest rating
    List<Review> findAllByOrderByRatingDesc();
    List<Review> findAllByOrderByRatingAsc();


}
