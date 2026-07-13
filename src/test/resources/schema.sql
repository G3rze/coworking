-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS coworking;

-- Create tables
CREATE TABLE IF NOT EXISTS coworking.users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS coworking.spaces (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    capacity INTEGER NOT NULL,
    location VARCHAR(200) NOT NULL,
    price_per_hour NUMERIC(10, 2) NOT NULL,
    status VARCHAR(255) NOT NULL CHECK (status IN ('AVAILABLE', 'MAINTENANCE')),
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS coworking.reservations (
    id UUID PRIMARY KEY,
    space_id UUID NOT NULL,
    user_id UUID NOT NULL,
    date DATE NOT NULL,
    start_time TIME(6) NOT NULL,
    end_time TIME(6) NOT NULL,
    status VARCHAR(255) NOT NULL CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED')),
    total_price NUMERIC(10, 2),
    payment_reference VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_reservation_space FOREIGN KEY (space_id) REFERENCES coworking.spaces(id),
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES coworking.users(id)
);