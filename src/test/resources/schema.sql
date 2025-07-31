CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    age INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
