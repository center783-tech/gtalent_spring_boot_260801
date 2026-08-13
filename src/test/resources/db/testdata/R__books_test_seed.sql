CREATE TABLE IF NOT EXISTS books (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    price INT NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO books (name, price) VALUES ('Test Book A', 100);
INSERT INTO books (name, price) VALUES ('Test Book B', 200);
