package com.financeanalyzer;

import com.financeanalyzer.dao.jdbc.DatabaseManager;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDemoUser {

    @Test
    void testCheckDemoUser() {
        System.out.println("=== DIAGNOSTIC CHECK FOR DEMO USER ===");
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:data/finance.db");
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT id, username, password_hash FROM users")) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    long id = rs.getLong("id");
                    String username = rs.getString("username");
                    String hash = rs.getString("password_hash");
                    boolean matches = BCrypt.checkpw("demo123", hash);
                    System.out.println("User row: ID=" + id + ", USERNAME='" + username + "', HASH='" + hash + "', BCrypt.checkpw('demo123')=" + matches);
                }
                System.out.println("TOTAL USERS IN data/finance.db: " + count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
