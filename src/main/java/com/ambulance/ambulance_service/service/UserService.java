package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.User;
import java.util.Optional;

public interface UserService {
    /**
     * Find a user by their username
     * @param username the username to search for
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by their email
     * @param email the email to search for
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Save a user to the database
     * @param user the user to save
     * @return the saved user
     */
    User save(User user);
}
