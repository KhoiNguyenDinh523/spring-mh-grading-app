-- =============================================================
-- MySQL Setup Script for MH Solution Grading System
-- =============================================================
-- Run this script ONCE to set up the database and user.
--
-- Usage:
--   mysql -u root -p < init-db.sql
-- =============================================================

-- 1. Create the database
CREATE DATABASE IF NOT EXISTS grading_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Create a dedicated application user (DO NOT use root in production!)
--    Replace 'db_password_here' with a strong password.
--    Then update application.properties accordingly.
CREATE USER IF NOT EXISTS 'grading_user'@'localhost'
    IDENTIFIED BY 'db_password_here';

-- 3. Grant only the necessary privileges (Principle of Least Privilege)
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX
    ON grading_db.* TO 'grading_user'@'localhost';

FLUSH PRIVILEGES;

-- 4. Switch to the database
USE grading_db;

-- NOTE: Spring Boot with spring.jpa.hibernate.ddl-auto=update
-- will automatically CREATE and UPDATE the tables below.
-- This script just creates the DB and user.

-- Optionally verify:
SHOW DATABASES LIKE 'grading_db';
SELECT user, host FROM mysql.user WHERE user = 'grading_user';
