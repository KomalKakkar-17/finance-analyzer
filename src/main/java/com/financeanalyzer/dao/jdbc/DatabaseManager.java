package com.financeanalyzer.dao.jdbc;

import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.model.enums.Frequency;
import com.financeanalyzer.model.enums.RuleStatus;
import com.financeanalyzer.model.enums.TransactionType;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.ConfigLoader;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages SQLite database connectivity, schema lifecycle, and database seeding.
 * Visibly demonstrates Unit 4 JDBC and SQL schema design with foreign key constraints.
 */
public final class DatabaseManager {

    private static final Logger LOGGER = AppLogger.getLogger(DatabaseManager.class);
    private static String dbUrl;

    static {
        initDbUrl();
    }

    private DatabaseManager() {
        // Utility / Singleton
    }

    private static void initDbUrl() {
        dbUrl = ConfigLoader.getProperty("db.url", "jdbc:sqlite:data/finance.db");
        // Ensure data directory exists
        if (dbUrl.startsWith("jdbc:sqlite:")) {
            String filePath = dbUrl.substring("jdbc:sqlite:".length());
            File dbFile = new File(filePath);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
                LOGGER.info("Created database directory: " + parentDir.getAbsolutePath());
            }
        }
    }

    /**
     * Obtains a new JDBC Connection with foreign key constraints enforced.
     *
     * @return active Connection
     */
    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(dbUrl);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            return conn;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to establish SQLite database connection to: " + dbUrl, e);
            throw new DataAccessException("Unable to connect to database.", e);
        }
    }

    /**
     * Initializes database tables, indexes, system default categories, and demo data.
     */
    public static synchronized void initializeDatabase() {
        LOGGER.info("Initializing database schema...");
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                // 1. Users Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT UNIQUE NOT NULL,
                        password_hash TEXT NOT NULL,
                        recommendations_enabled INTEGER DEFAULT 1,
                        created_at TEXT NOT NULL
                    );
                """);

                // 2. Categories Table (user_id is NULL for system categories)
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NULL,
                        name TEXT NOT NULL,
                        type TEXT CHECK (type IN ('INCOME','EXPENSE')),
                        is_system INTEGER DEFAULT 0,
                        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    );
                """);

                // 3. Recurring Rules Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS recurring_rules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        category_id INTEGER NOT NULL,
                        type TEXT CHECK (type IN ('INCOME','EXPENSE')),
                        amount_paise INTEGER NOT NULL CHECK (amount_paise > 0),
                        frequency TEXT CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY','YEARLY')),
                        start_date TEXT NOT NULL,
                        next_due_date TEXT NOT NULL,
                        end_date TEXT NULL,
                        status TEXT CHECK (status IN ('ACTIVE','PAUSED','EXPIRED')),
                        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY(category_id) REFERENCES categories(id)
                    );
                """);

                // 4. Transactions Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        category_id INTEGER NOT NULL,
                        type TEXT CHECK (type IN ('INCOME','EXPENSE')),
                        amount_paise INTEGER NOT NULL CHECK (amount_paise > 0),
                        txn_date TEXT NOT NULL,
                        payment_mode TEXT,
                        notes TEXT,
                        recurring_rule_id INTEGER NULL,
                        created_at TEXT NOT NULL,
                        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY(category_id) REFERENCES categories(id),
                        FOREIGN KEY(recurring_rule_id) REFERENCES recurring_rules(id) ON DELETE SET NULL
                    );
                """);

                // 5. Budgets Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS budgets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        category_id INTEGER NOT NULL,
                        year_month TEXT NOT NULL,
                        limit_paise INTEGER NOT NULL CHECK (limit_paise > 0),
                        UNIQUE(user_id, category_id, year_month),
                        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY(category_id) REFERENCES categories(id)
                    );
                """);

                // 6. Savings Goals Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS savings_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        target_paise INTEGER NOT NULL CHECK (target_paise > 0),
                        saved_paise INTEGER DEFAULT 0 CHECK (saved_paise >= 0),
                        deadline TEXT NOT NULL,
                        allocation_ratio INTEGER CHECK (allocation_ratio >= 0 AND allocation_ratio <= 100),
                        is_active INTEGER DEFAULT 1,
                        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    );
                """);

                // 7. Execution Audit Log Table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS execution_audit_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        rule_id INTEGER NOT NULL,
                        run_at TEXT NOT NULL,
                        result TEXT NOT NULL,
                        message TEXT,
                        FOREIGN KEY(rule_id) REFERENCES recurring_rules(id) ON DELETE CASCADE
                    );
                """);

                // 8. User Fixed Categories Table (per-user, excludes from recommendations)
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS user_fixed_categories (
                        user_id INTEGER NOT NULL,
                        category_id INTEGER NOT NULL,
                        PRIMARY KEY (user_id, category_id),
                        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE CASCADE
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS user_fixed_category_initializations (
                        user_id INTEGER PRIMARY KEY,
                        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    );
                """);

                // Indexes
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_txn_user_date ON transactions(user_id, txn_date);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_txn_category ON transactions(category_id);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_budget_user_month ON budgets(user_id, year_month);");

                conn.commit();
                LOGGER.info("Database schema tables and indexes verified successfully.");
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Error creating database schema tables, rolled back.", e);
                throw new DataAccessException("Failed to initialize database schema.", e);
            }

            // Seed default categories and demo user if needed
            seedSystemCategories(conn);
            seedDemoDataIfEmpty(conn);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Fatal error during database initialization.", e);
            throw new DataAccessException("Fatal error during database initialization.", e);
        }
    }

    private static void seedSystemCategories(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM categories WHERE is_system = 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Already seeded
            }
        }

        String insertSql = "INSERT INTO categories (user_id, name, type, is_system) VALUES (NULL, ?, ?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            conn.setAutoCommit(false);

            String[] expenseCategories = ConfigLoader.getProperty(
                    "default.categories.expense",
                    "Food & Dining,Transport,Rent,Utilities,Shopping,Health,Entertainment,Education,Other"
            ).split(",");

            for (String cat : expenseCategories) {
                ps.setString(1, cat.trim());
                ps.setString(2, TransactionType.EXPENSE.name());
                ps.addBatch();
            }

            String[] incomeCategories = ConfigLoader.getProperty(
                    "default.categories.income",
                    "Salary,Investment,Business,Gift,Other Income"
            ).split(",");

            for (String cat : incomeCategories) {
                ps.setString(1, cat.trim());
                ps.setString(2, TransactionType.INCOME.name());
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();
            LOGGER.info("System default categories seeded successfully.");
        } catch (SQLException e) {
            conn.rollback();
            LOGGER.log(Level.SEVERE, "Failed to seed default categories.", e);
            throw e;
        }
    }

    private static void seedDemoDataIfEmpty(Connection conn) throws SQLException {
        String checkUserSql = "SELECT COUNT(*) FROM users";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkUserSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Users already exist
            }
        }

        LOGGER.info("Seeding initial demo user 'demo' with realistic financial records...");
        conn.setAutoCommit(false);
        try {
            // 1. Insert Demo User
            String userSql = "INSERT INTO users (username, password_hash, recommendations_enabled, created_at) VALUES (?, ?, 1, ?)";
            long demoUserId;
            try (PreparedStatement ps = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, "demo");
                ps.setString(2, BCrypt.hashpw("demo123", BCrypt.gensalt(10)));
                ps.setString(3, LocalDate.now().minusMonths(2).toString() + "T10:00:00");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        demoUserId = keys.getLong(1);
                    } else {
                        throw new SQLException("Failed to retrieve generated key for demo user.");
                    }
                }
            }

            // Retrieve category IDs
            long salaryCatId = getCategoryId(conn, "Salary", TransactionType.INCOME);
            long foodCatId = getCategoryId(conn, "Food & Dining", TransactionType.EXPENSE);
            long rentCatId = getCategoryId(conn, "Rent", TransactionType.EXPENSE);
            long utilCatId = getCategoryId(conn, "Utilities", TransactionType.EXPENSE);
            long entertainmentCatId = getCategoryId(conn, "Entertainment", TransactionType.EXPENSE);
            long shoppingCatId = getCategoryId(conn, "Shopping", TransactionType.EXPENSE);

            LocalDate today = LocalDate.now();
            YearMonth thisMonth = YearMonth.now();
            YearMonth lastMonth = thisMonth.minusMonths(1);

            // 2. Insert Budgets for Current Month
            String budgetSql = "INSERT INTO budgets (user_id, category_id, year_month, limit_paise) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(budgetSql)) {
                // Food: ₹15,000 (1500000 paise)
                ps.setLong(1, demoUserId);
                ps.setLong(2, foodCatId);
                ps.setString(3, thisMonth.toString());
                ps.setLong(4, 1500000L);
                ps.addBatch();

                // Rent: ₹25,000 (2500000 paise)
                ps.setLong(1, demoUserId);
                ps.setLong(2, rentCatId);
                ps.setString(3, thisMonth.toString());
                ps.setLong(4, 2500000L);
                ps.addBatch();

                // Utilities: ₹5,000 (500000 paise)
                ps.setLong(1, demoUserId);
                ps.setLong(2, utilCatId);
                ps.setString(3, thisMonth.toString());
                ps.setLong(4, 500000L);
                ps.addBatch();

                // Entertainment: ₹6,000 (600000 paise)
                ps.setLong(1, demoUserId);
                ps.setLong(2, entertainmentCatId);
                ps.setString(3, thisMonth.toString());
                ps.setLong(4, 600000L);
                ps.addBatch();

                ps.executeBatch();
            }

            // 3. Insert Savings Goals (Ratios sum to 70% <= 100%)
            String goalSql = "INSERT INTO savings_goals (user_id, title, target_paise, saved_paise, deadline, allocation_ratio, is_active) VALUES (?, ?, ?, ?, ?, ?, 1)";
            try (PreparedStatement ps = conn.prepareStatement(goalSql)) {
                // Emergency Fund: Target ₹1,50,000, Saved ₹85,000, 40% ratio
                ps.setLong(1, demoUserId);
                ps.setString(2, "Emergency Fund");
                ps.setLong(3, 15000000L);
                ps.setLong(4, 8500000L);
                ps.setString(5, today.plusMonths(8).toString());
                ps.setInt(6, 40);
                ps.addBatch();

                // Laptop Upgrade: Target ₹80,000, Saved ₹40,000, 30% ratio
                ps.setLong(1, demoUserId);
                ps.setString(2, "Laptop Upgrade");
                ps.setLong(3, 8000000L);
                ps.setLong(4, 4000000L);
                ps.setString(5, today.plusMonths(4).toString());
                ps.setInt(6, 30);
                ps.addBatch();

                ps.executeBatch();
            }

            // 4. Insert Transactions for current and previous month
            String txnSql = "INSERT INTO transactions (user_id, category_id, type, amount_paise, txn_date, payment_mode, notes, recurring_rule_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NULL, ?)";
            try (PreparedStatement ps = conn.prepareStatement(txnSql)) {
                // Last Month Salary
                insertTxnBatch(ps, demoUserId, salaryCatId, TransactionType.INCOME, 7500000L, lastMonth.atDay(1).toString(), "Bank Transfer", "Monthly Salary", lastMonth.atDay(1).toString() + "T09:00:00");
                // Last Month Rent
                insertTxnBatch(ps, demoUserId, rentCatId, TransactionType.EXPENSE, 2500000L, lastMonth.atDay(2).toString(), "UPI", "House Rent", lastMonth.atDay(2).toString() + "T10:00:00");
                // Last Month Food
                insertTxnBatch(ps, demoUserId, foodCatId, TransactionType.EXPENSE, 1150000L, lastMonth.atDay(15).toString(), "Credit Card", "Groceries & Dining", lastMonth.atDay(15).toString() + "T18:00:00");

                // This Month Salary
                insertTxnBatch(ps, demoUserId, salaryCatId, TransactionType.INCOME, 7500000L, thisMonth.atDay(1).toString(), "Bank Transfer", "Monthly Salary", thisMonth.atDay(1).toString() + "T09:00:00");
                // This Month Rent
                insertTxnBatch(ps, demoUserId, rentCatId, TransactionType.EXPENSE, 2500000L, thisMonth.atDay(2).toString(), "UPI", "House Rent", thisMonth.atDay(2).toString() + "T10:00:00");
                // This Month Food (₹12,400 spent = ~82% warning threshold to demonstrate amber alert!)
                insertTxnBatch(ps, demoUserId, foodCatId, TransactionType.EXPENSE, 840000L, thisMonth.atDay(5).toString(), "Credit Card", "Supermarket Groceries", thisMonth.atDay(5).toString() + "T12:00:00");
                insertTxnBatch(ps, demoUserId, foodCatId, TransactionType.EXPENSE, 400000L, thisMonth.atDay(12).toString(), "UPI", "Team Dinner", thisMonth.atDay(12).toString() + "T20:30:00");
                // This Month Utilities
                insertTxnBatch(ps, demoUserId, utilCatId, TransactionType.EXPENSE, 320000L, thisMonth.atDay(8).toString(), "Net Banking", "Electricity & Fiber Internet", thisMonth.atDay(8).toString() + "T11:00:00");
                // This Month Shopping
                insertTxnBatch(ps, demoUserId, shoppingCatId, TransactionType.EXPENSE, 450000L, thisMonth.atDay(14).toString(), "Debit Card", "Clothing & Books", thisMonth.atDay(14).toString() + "T16:00:00");

                ps.executeBatch();
            }

            // 5. Insert a Recurring Rule
            String ruleSql = "INSERT INTO recurring_rules (user_id, category_id, type, amount_paise, frequency, start_date, next_due_date, end_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(ruleSql)) {
                ps.setLong(1, demoUserId);
                ps.setLong(2, utilCatId);
                ps.setString(3, TransactionType.EXPENSE.name());
                ps.setLong(4, 99900L); // ₹999
                ps.setString(5, Frequency.MONTHLY.name());
                ps.setString(6, thisMonth.atDay(1).toString());
                ps.setString(7, today.plusDays(3).toString());
                ps.setNull(8, Types.VARCHAR);
                ps.setString(9, RuleStatus.ACTIVE.name());
                ps.executeUpdate();
            }

            // 6. Seed default fixed categories (Rent, Utilities) for demo user
            JdbcUserFixedCategoryDao.initializeDefaultsIfNeeded(conn, demoUserId);

            conn.commit();
            LOGGER.info("Demo user 'demo' and sample data seeded successfully.");
        } catch (SQLException e) {
            conn.rollback();
            LOGGER.log(Level.SEVERE, "Failed to seed demo user data.", e);
            throw e;
        }
    }

    private static void insertTxnBatch(PreparedStatement ps, long userId, long catId, TransactionType type,
                                       long paise, String date, String mode, String notes, String createdAt) throws SQLException {
        ps.setLong(1, userId);
        ps.setLong(2, catId);
        ps.setString(3, type.name());
        ps.setLong(4, paise);
        ps.setString(5, date);
        ps.setString(6, mode);
        ps.setString(7, notes);
        ps.setString(8, createdAt);
        ps.addBatch();
    }

    private static long getCategoryId(Connection conn, String name, TransactionType type) throws SQLException {
        String sql = "SELECT id FROM categories WHERE name = ? AND type = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 1L; // Fallback
    }
}
