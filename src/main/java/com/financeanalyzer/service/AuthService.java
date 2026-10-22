package com.financeanalyzer.service;

import com.financeanalyzer.dao.UserDao;
import com.financeanalyzer.dao.UserFixedCategoryDao;
import com.financeanalyzer.exception.AuthenticationException;
import com.financeanalyzer.model.User;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.Validators;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service orchestrating user authentication, registration, password hashing, and session management.
 * Demonstrates FR-01 and Unit 4 BCrypt security guidelines (no salt column).
 */
public class AuthService {

    private static final Logger LOGGER = AppLogger.getLogger(AuthService.class);
    private final UserDao userDao;
    private final UserFixedCategoryDao userFixedCategoryDao;
    private User currentUser;

    /**
     * Constructor injection of authentication and fixed-category data access dependencies.
     */
    public AuthService(UserDao userDao, UserFixedCategoryDao userFixedCategoryDao) {
        this.userDao = Objects.requireNonNull(userDao, "UserDao must not be null.");
        this.userFixedCategoryDao = Objects.requireNonNull(
                userFixedCategoryDao, "UserFixedCategoryDao must not be null.");
    }

    /**
     * Registers a new user account with BCrypt password hashing.
     *
     * @param username raw username
     * @param password raw password
     * @return the created User
     * @throws AuthenticationException if validation fails or username is already taken
     */
    public User register(String username, String password) throws AuthenticationException {
        if (!Validators.isValidUsername(username)) {
            throw new AuthenticationException("Username must be at least 6 alphanumeric characters.");
        }
        if (!Validators.isValidPassword(password)) {
            throw new AuthenticationException("Password must be at least 6 characters.");
        }

        String cleanUsername = username.trim();
        Optional<User> existing = userDao.findByUsername(cleanUsername);
        if (existing.isPresent()) {
            LOGGER.log(Level.WARNING, "Registration attempt failed: username already taken: {0}", cleanUsername);
            throw new AuthenticationException("Username '" + cleanUsername + "' is already registered.");
        }

        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(10));
        User newUser = new User(cleanUsername, passwordHash);
        User savedUser = userDao.save(newUser);
        this.currentUser = savedUser;

        LOGGER.log(Level.INFO, "User registered successfully: {0} (ID: {1})",
                new Object[]{savedUser.getUsername(), savedUser.getId()});
        return savedUser;
    }

    /**
     * Authenticates an existing user against stored BCrypt password hash.
     *
     * @param username username string
     * @param password raw password string
     * @return authenticated User
     * @throws AuthenticationException if credentials are invalid
     */
    public User login(String username, String password) throws AuthenticationException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException("Username and password are required.");
        }

        String cleanUsername = username.trim();
        Optional<User> userOpt = userDao.findByUsername(cleanUsername);
        if (userOpt.isEmpty()) {
            LOGGER.log(Level.WARNING, "Login failed: User not found: {0}", cleanUsername);
            throw new AuthenticationException("Invalid username or password.");
        }

        User user = userOpt.get();
        boolean valid = BCrypt.checkpw(password, user.getPasswordHash());
        if (!valid) {
            LOGGER.log(Level.WARNING, "Login failed: Incorrect password for user: {0}", cleanUsername);
            throw new AuthenticationException("Invalid username or password.");
        }

        userFixedCategoryDao.initializeDefaultsIfNeeded(user.getId());
        this.currentUser = user;
        LOGGER.log(Level.INFO, "User successfully logged in: {0} (ID: {1})",
                new Object[]{user.getUsername(), user.getId()});
        return user;
    }

    /**
     * Clears current active session.
     */
    public void logout() {
        if (currentUser != null) {
            LOGGER.log(Level.INFO, "User logged out: {0}", currentUser.getUsername());
        }
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Toggles smart heuristic recommendation preference for current user.
     */
    public boolean updateRecommendationsPreference(boolean enabled) {
        if (currentUser == null) return false;
        boolean updated = userDao.updateRecommendationsSetting(currentUser.getId(), enabled);
        if (updated) {
            currentUser.setRecommendationsEnabled(enabled);
            LOGGER.log(Level.INFO, "Updated recommendations setting to {0} for user: {1}",
                    new Object[]{enabled, currentUser.getUsername()});
        }
        return updated;
    }
}
