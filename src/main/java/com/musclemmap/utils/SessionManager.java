package com.musclemmap.utils;

import com.musclemmap.models.User;
import java.time.LocalDateTime;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private LocalDateTime loginTime;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
        this.loginTime = LocalDateTime.now();
        user.setLastLogin(loginTime);
        System.out.println("✅ User logged in: " + user.getUsername());
    }

    public void logout() {
        if (currentUser != null) {
            System.out.println("👋 User logged out: " + currentUser.getUsername());
        }
        this.currentUser = null;
        this.loginTime = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public long getSessionDurationMinutes() {
        if (loginTime == null) {
            return 0;
        }
        return java.time.Duration.between(loginTime, LocalDateTime.now()).toMinutes();
    }
}
