package com.imp3.Backend.tag;

import com.imp3.Backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserTagRepository extends JpaRepository<UserTag, Integer> {
    List<UserTag> findByUser_Id(Integer uid);
    void deleteByTag_TagId(Integer tagId);
    boolean existsByUser_IdAndTag_TagId(Integer uid, Integer tagId);
    boolean existsByUserAndTag(User user, Tag tag);
}
