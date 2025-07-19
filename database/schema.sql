-- This file is for documentation purposes only
-- Spring Boot will create these tables automatically

-- Create Database
CREATE DATABASE ambulance_service;

-- Create User (Run as superuser)
CREATE USER ambulance_user WITH ENCRYPTED PASSWORD 'ambulance_password';
GRANT ALL PRIVILEGES ON DATABASE ambulance_service TO ambulance_user;

-- Use ambulance_service database
\c ambulance_service;

-- Grant schema permissions
GRANT ALL ON SCHEMA public TO ambulance_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ambulance_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ambulance_user;

-- The tables will be created automatically by JPA/Hibernate
-- But here's the manual schema for reference:

CREATE TABLE ambulances (
    id BIGSERIAL PRIMARY KEY,
    current_location VARCHAR(255) NOT NULL,
    availability VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE requests (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    user_contact VARCHAR(20) NOT NULL,
    location VARCHAR(500) NOT NULL,
    emergency_description TEXT,
    request_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dispatch_time TIMESTAMP,
    ambulance_id BIGINT REFERENCES ambulances(id),
    status VARCHAR(50) DEFAULT 'PENDING'
);

CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact VARCHAR(20) NOT NULL,
    medical_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE service_history (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT REFERENCES requests(id),
    patient_id BIGINT REFERENCES patients(id),
    ambulance_id BIGINT REFERENCES ambulances(id),
    arrival_time TIMESTAMP,
    completion_time TIMESTAMP,
    status VARCHAR(50) DEFAULT 'IN_PROGRESS',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample data
INSERT INTO ambulances (current_location, availability) VALUES
('Downtown Medical Center', 'AVAILABLE'),
('Northside Hospital', 'AVAILABLE'),
('Emergency Station 1', 'MAINTENANCE'),
('City Center', 'AVAILABLE');

INSERT INTO patients (name, contact, medical_notes) VALUES
('John Doe', '+1234567890', 'No known allergies'),
('Jane Smith', '+1234567891', 'Diabetic patient'),
('Bob Johnson', '+1234567892', 'Hypertension');