-- This file is for documentation purposes only
-- Spring Boot will create these tables automatically

-- To create the database (run this manually in pgAdmin or psql):
CREATE DATABASE ambulance_service;

-- Tables that will be created by Spring Boot:
-- 1. ambulances (id, current_location, availability)
-- 2. requests (id, user_name, user_contact, location, request_time, dispatch_time, ambulance_id, status)
-- 3. patients (id, name, contact, medical_notes, request_id)

-- Sample data that will be inserted by DataInitializer class:
-- INSERT INTO ambulances (current_location, availability) VALUES 
-- ('Station A - Downtown', true),
-- ('Station B - North Side', true),
-- ('Station C - South District', true);