package com.example.controller;

import com.example.entity.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * 初始化测试数据
     */
    @PostMapping("/init")
    @Transactional
    public String initTestData() {
        // 创建测试表
        createTestTables();
        
        // 插入初始测试数据
        insertInitialData();
        
        return "测试数据初始化成功";
    }
    
    /**
     * 添加用户数据
     */
    @PostMapping("/users")
    @Transactional
    public String addUsers(@RequestParam(defaultValue = "5") int count) {
        for (int i = 0; i < count; i++) {
            UserInfo user = new UserInfo();
            user.setUsername("user_" + System.currentTimeMillis() + "_" + i);
            user.setEmail("user_" + i + "@example.com");
            entityManager.persist(user);
        }
        return "成功添加 " + count + " 个用户";
    }
    
    /**
     * 添加订单数据
     */
    @PostMapping("/orders")
    @Transactional
    public String addOrders(@RequestParam(defaultValue = "10") int count) {
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update(
                "INSERT INTO order_info (order_no, user_id, amount, created_time) VALUES (?, ?, ?, NOW())",
                "ORDER_" + System.currentTimeMillis() + "_" + i,
                (i % 10) + 1,
                100.0 + (i * 10)
            );
        }
        return "成功添加 " + count + " 个订单";
    }
    
    /**
     * 获取所有表
     */
    @GetMapping("/tables")
    public List<Map<String, Object>> getTables() {
        return jdbcTemplate.queryForList("SHOW TABLES");
    }
    
    /**
     * 获取用户数量
     */
    @GetMapping("/users/count")
    public Map<String, Object> getUserCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_info", Long.class);
        return Map.of("table", "user_info", "count", count);
    }
    
    /**
     * 获取订单数量
     */
    @GetMapping("/orders/count")
    public Map<String, Object> getOrderCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_info", Long.class);
        return Map.of("table", "order_info", "count", count);
    }
    
    /**
     * 创建测试表
     */
    private void createTestTables() {
        // 创建用户表（已通过JPA自动创建）
        
        // 创建订单表
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS order_info (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "order_no VARCHAR(100) NOT NULL, " +
            "user_id BIGINT NOT NULL, " +
            "amount DECIMAL(10,2) NOT NULL, " +
            "created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );
        
        // 创建产品表
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS product_info (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "product_name VARCHAR(200) NOT NULL, " +
            "price DECIMAL(10,2) NOT NULL, " +
            "category VARCHAR(100), " +
            "created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );
    }
    
    /**
     * 插入初始数据
     */
    private void insertInitialData() {
        // 插入用户数据
        for (int i = 1; i <= 10; i++) {
            UserInfo user = new UserInfo();
            user.setUsername("user_" + i);
            user.setEmail("user" + i + "@example.com");
            entityManager.persist(user);
        }
        
        // 插入订单数据
        for (int i = 1; i <= 20; i++) {
            jdbcTemplate.update(
                "INSERT INTO order_info (order_no, user_id, amount, created_time) VALUES (?, ?, ?, NOW())",
                "ORDER_" + String.format("%04d", i),
                (i % 10) + 1,
                100.0 + (i * 5)
            );
        }
        
        // 插入产品数据
        String[] products = {"iPhone", "Samsung Galaxy", "MacBook", "Dell Laptop", "iPad"};
        for (int i = 0; i < products.length; i++) {
            jdbcTemplate.update(
                "INSERT INTO product_info (product_name, price, category, created_time) VALUES (?, ?, ?, NOW())",
                products[i] + " " + (i + 1),
                999.99 + (i * 100),
                "Electronics"
            );
        }
    }
}