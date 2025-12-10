package com.imp3.Backend.user;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User>findByEmail(String email);

    // for user-facing search bar
    List<User> findByFirstnameStartingWith(String firstname);
    List<User> findByLastnameStartingWith(String lastname);
    List<User> findByUsernameStartingWith(String username);
}
