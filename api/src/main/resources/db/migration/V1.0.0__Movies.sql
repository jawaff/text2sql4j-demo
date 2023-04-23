CREATE TABLE actors (
    actor_name VARCHAR(300),
    start_year_of_work INTEGER,
    end_year_of_work INTEGER,
    gender VARCHAR(6),
    year_of_birth INTEGER,
    CONSTRAINT actors_primary_key PRIMARY KEY (actor_name)
);

CREATE TABLE movies (
    movie_title VARCHAR(300),
    year_released INTEGER NOT NULL,
    director_name VARCHAR(100),
    producer_name VARCHAR(100),
    studio_name VARCHAR(100),
    color_process VARCHAR(50),
    genre VARCHAR(50),
    CONSTRAINT movies_primary_key PRIMARY KEY (movie_title)
);

CREATE TABLE movie_casts (
    movie_title VARCHAR(300) REFERENCES movies(movie_title) ON DELETE CASCADE,
    actor_name VARCHAR(300) REFERENCES actors(actor_name) ON DELETE CASCADE,
    role VARCHAR(50),
    CONSTRAINT movie_casts_primary_key PRIMARY KEY (movie_title, actor_name)
);