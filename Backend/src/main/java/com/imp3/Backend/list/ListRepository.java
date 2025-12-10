package com.imp3.Backend.list;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ListRepository extends JpaRepository<UserList, Integer>{

    // All lists for a user (via User.id)
    List<UserList> findAllByOwner_Id(Integer id);

    // All lists for a user filtered by privacy enum
    List<UserList> findAllByOwner_IdAndPrivacy(Integer id, UserList.Privacy privacy);

    Optional<UserList> findByListIdAndOwner_Id(Integer listId, Integer id);

    // Title search
    List<UserList> findByTitleContainingIgnoreCase(String keyword);

    // Single list by PK + privacy (PK field is listId)
    Optional<UserList> findByListIdAndPrivacy(Integer listId, UserList.Privacy privacy);

    // Compatibility shim if something still calls findByIdAndPrivacy(...)
    @Query("select u from UserList u where u.listId = :listId and u.privacy = :privacy")
    Optional<UserList> findByIdAndPrivacy(@Param("listId") Integer listId,
                                          @Param("privacy") UserList.Privacy privacy);

}
