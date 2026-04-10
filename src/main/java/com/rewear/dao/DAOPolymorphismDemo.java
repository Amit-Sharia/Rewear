package com.rewear.dao;

import com.rewear.models.ChatMessage;
import com.rewear.models.Item;
import com.rewear.models.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Demonstration of Interface Polymorphism in ReWear Project
 *
 * This class shows how the IDataAccessObject interface enables polymorphism:
 * - Same interface methods
 * - Different implementations for different entity types
 * - Consistent API across all DAOs
 */
public class DAOPolymorphismDemo {

    public static void main(String[] args) {
        demonstratePolymorphism();
    }

    /**
     * Demonstrates polymorphism using the IDataAccessObject interface
     */
    public static void demonstratePolymorphism() {
        try {
            // Create different DAO implementations
            IDataAccessObject<User, Integer> userDao = new UserDAO();
            IDataAccessObject<Item, Integer> itemDao = new ItemDAO();
            IDataAccessObject<ChatMessage, Integer> chatDao = new ChatDAO();

            // Same interface methods, different behaviors:

            // 1. INSERT operations (polymorphic)
            System.out.println("=== INSERT Operations (Polymorphism) ===");
            // userDao.insert(user) would insert into USERS table
            // itemDao.insert(item) would insert into ITEMS table
            // chatDao.insert(message) would insert into EXCHANGE_MESSAGES table

            // 2. FIND operations (polymorphic)
            System.out.println("=== FIND Operations (Polymorphism) ===");
            Optional<User> user = userDao.findById(1);           // Finds from USERS
            Optional<Item> item = itemDao.findById(1);           // Finds from ITEMS
            Optional<ChatMessage> message = chatDao.findById(1); // Finds from EXCHANGE_MESSAGES

            // 3. FIND ALL operations (polymorphic)
            System.out.println("=== FIND ALL Operations (Polymorphism) ===");
            List<User> users = userDao.findAll();              // SELECT * FROM USERS
            List<Item> items = itemDao.findAll();              // SELECT * FROM ITEMS
            List<ChatMessage> messages = chatDao.findAll();    // SELECT * FROM EXCHANGE_MESSAGES

            // 4. UPDATE operations (polymorphic)
            System.out.println("=== UPDATE Operations (Polymorphism) ===");
            // userDao.update(user) would update USERS table
            // itemDao.update(item) would update ITEMS table
            // chatDao.update(message) would update EXCHANGE_MESSAGES table

            // 5. DELETE operations (polymorphic)
            System.out.println("=== DELETE Operations (Polymorphism) ===");
            // userDao.delete(1) would delete from USERS
            // itemDao.delete(1) would delete from ITEMS
            // chatDao.delete(1) would delete from EXCHANGE_MESSAGES

            System.out.println("✅ All DAO operations use the same interface!");
            System.out.println("✅ Polymorphism allows different implementations for different entities");

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    /**
     * Example of how polymorphism enables flexible DAO usage
     */
    public static void flexibleDAOUsage() {
        // You can write generic methods that work with any DAO
        processEntity(new UserDAO(), 1);      // Works with UserDAO
        processEntity(new ItemDAO(), 2);      // Works with ItemDAO
        processEntity(new ChatDAO(), 3);      // Works with ChatDAO
    }

    /**
     * Generic method that works with any DAO implementation
     * This demonstrates the power of interface polymorphism!
     */
    private static <T, ID> void processEntity(IDataAccessObject<T, ID> dao, ID id) {
        try {
            Optional<T> entity = dao.findById(id);
            if (entity.isPresent()) {
                System.out.println("Found entity: " + entity.get().getClass().getSimpleName());
            } else {
                System.out.println("Entity not found with ID: " + id);
            }
        } catch (SQLException e) {
            System.err.println("Error processing entity: " + e.getMessage());
        }
    }
}