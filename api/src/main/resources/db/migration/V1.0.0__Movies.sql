CREATE TABLE actors (
    name VARCHAR(50) PRIMARY KEY,
    start_year_of_work INTEGER,
    end_year_of_work INTEGER,
    gender VARCHAR(6),
    year_of_birth INTEGER
);

CREATE TABLE movies (
    title VARCHAR(300) PRIMARY KEY,
    year_released INTEGER NOT NULL,
    director_name VARCHAR(50),
    producer_name VARCHAR(50),
    studio_name VARCHAR(50),
    color_process VARCHAR(50),
    genre VARCHAR(50)
);

CREATE TABLE movie_casts (
    movie_title VARCHAR(300) REFERENCES movies(title) ON DELETE CASCADE,
    actor_name VARCHAR(50) REFERENCES actors(name) ON DELETE CASCADE,
    role VARCHAR(50),
    PRIMARY KEY (movie_title, actor_name)
);