package com.imp3.Backend.tag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Integer> {
    Optional<Tag> findByName(String name);
    Optional<Tag> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<Tag> findByCategory(Tag.TagCategory category);
    List<Tag> findByNameContainingIgnoreCase(String name);
}
