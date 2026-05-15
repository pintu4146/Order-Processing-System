-- =============================================
-- V2: Add total_amount column to orders table
-- =============================================

ALTER TABLE orders ADD COLUMN total_amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00;
