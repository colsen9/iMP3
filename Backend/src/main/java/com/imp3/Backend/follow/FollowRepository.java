package com.imp3.Backend.follow;

import org.springframework.data.jpa.repository.JpaRepository;
import com.imp3.Backend.user.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Integer> {
    List<Follow> findByFollowed_Id(Integer followedId);
    List<Follow> findByFollower_Id(Integer followerId);

    boolean existsByFollower_IdAndFollowed_Id(Integer followerId, Integer followedId);

    Optional<Follow> findByFollower_IdAndFollowed_Id(Integer followerId, Integer followedId);

    void deleteByFollower_IdAndFollowed_Id(Integer followerId, Integer followedId);

    long countByFollower_Id(Integer followerId);
    long countByFollowed_Id(Integer followedId);

    //helper methods

    /**
     * Returns list of followers by id
     * @param id of the user
     * @return list of followers
     */
    default List <User> getFollowersOf(Integer id){
        List<Follow> follows = findByFollowed_Id(id);
        List<User> followers = new ArrayList<>();

        for(Follow f : follows){
            followers.add(f.getFollower());
        }
        return followers;
    }

    /**
     * Returns list of following by id
     * @param id of the user
     * @return list of following
     */
    default List <User> getFollowingOf(Integer id) {
        List<Follow> follows = findByFollower_Id(id);
        List<User> following = new ArrayList<>();

        for(Follow f : follows){
            following.add(f.getFollowed());
        }
        return following;
    }

}
