DROP TABLE IF EXISTS Reservations;
DROP TABLE IF EXISTS Users;
DROP TABLE IF EXISTS Itineraries;
DROP TABLE IF EXISTS Capacities;

CREATE TABLE Users (
    username VARCHAR(20) PRIMARY KEY,
    password VARBINARY(20),
    balance int,
    salt VARBINARY(20),
);


CREATE TABLE Itineraries (
    it_id int PRIMARY KEY,
    price int,
    first_flight_id int,
    second_flight_id int,
);


CREATE TABLE Reservations (
    ID int IDENTITY(1, 1) PRIMARY KEY,
    itinerary int UNIQUE,
    date int,
    FOREIGN KEY (itinerary) REFERENCES Itineraries(it_id),
    username VARCHAR(20) NOT NULL,
    FOREIGN KEY (username) REFERENCES Users(username),
    paid int,
    cancelled int,
);

CREATE TABLE Capacities (
    fid int PRIMARY KEY,
    capacity int,
);
