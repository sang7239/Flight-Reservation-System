# Flight-Reservation-System

## Requirements
The Flight-Reservation-System is a CLI (Command Line Input) application that supports flight booking for different users. Following are the functional specifications for the flight service system:

- **create** takes in a new username, password, and initial account balance as input. It creates a new user account with the initial balance.
  Return an error if negative, or if the username already exists. Usernames and passwords are checked case-insensitively, and password is hashed with a salt.

- **login** takes in a username and password, and checks that the user exists in the database and that the password matches.
  Within a single session (that is, a single instance of your program), only one user should be logged in. 

- **search** takes as input an origin city (string), a destination city (string), a flag for only direct flights or not (0 or 1), the date (int), and the maximum number of itineraries to be returned (int).
  For the date, we only need the day of the month, since our dataset comes from July 2015. Return only flights that are not canceled, ignoring the capacity and number of seats available.
  If the user requests n itineraries to be returned, there are a number of possibilities:
    * direct=1: return up to n direct itineraries
    * direct=0: return up to n direct itineraries. If there are only k direct itineraries (where k < n), then return the k direct itineraries and up to (n-k) of the shortest indirect itineraries with the flight times. For one-hop flights, different carriers can be used for the flights. For the purpose of this assignment, an indirect itinerary means the first and second flight only must be on the same date (i.e., if flight 1 runs on the 3rd day of July, flight 2 runs on the 4th day of July, then you can't put these two flights in the same itinerary as they are not on the same day).

  <br />the returned results are sorted on total actual_time (ascending). If a tie occurs, a second sort is done on the fid value

    Below is an example of a single direct flight from Seattle to Boston.

    ```
    Itinerary 0: 1 flight(s), 297 minutes
    ID: 60454 Day: 1 Carrier: AS Number: 24 Origin: Seattle WA Dest: Boston MA Duration: 297 Capacity: 14 Price: 140
    ```

    Below is an example of two indirect flights from Seattle to Boston:

    ```
    Itinerary 0: 2 flight(s), 317 minutes
    ID: 704749 Day: 10 Carrier: AS Number: 16 Origin: Seattle WA Dest: Orlando FL Duration: 159 Capacity: 10 Price: 494
    ID: 726309 Day: 10 Carrier: B6 Number: 152 Origin: Orlando FL Dest: Boston MA Duration: 158 Capacity: 0 Price: 104
    Itinerary 1: 2 flight(s), 317 minutes
    ID: 704749 Day: 10 Carrier: AS Number: 16 Origin: Seattle WA Dest: Orlando FL Duration: 159 Capacity: 10 Price: 494
    ID: 726464 Day: 10 Carrier: B6 Number: 452 Origin: Orlando FL Dest: Boston MA Duration: 158 Capacity: 7 Price: 760
    ```

    For one-hop flights, the results are printed in the order of the itinerary, starting from the flight leaving the origin and ending with the flight arriving at the destination.

    The returned itineraries should start from 0 and increase by 1 up to n as shown above. If no itineraries match the search query, the system returns an informative error message.

    The user need not be logged in to search for flights.

    All flights in an indirect itinerary should be under the same itinerary ID. In other words, the user should only need to book once with the itinerary ID for direct or indirect trips.


- **book** lets a user book an itinerary by providing the itinerary number as returned by a previous search.
  The user must be logged in to book an itinerary, and must enter a valid itinerary id that was returned in the last search that was performed *within the same login session*.
  Once the user logs out (by quitting the application),
  logs in (if they previously were not logged in), or performs another search within the same login session,
  then all previously returned itineraries are invalidated and cannot be booked.

  A user cannot book a flight if the flight's maximum capacity would be exceeded. Each flight’s capacity is stored in the seperate Capacities table.

  If booking is successful, assigns a new reservation ID to the booked itinerary.
  1) each reservation can contain up to 2 flights (in the case of indirect flights),
  and 2) each reservation has a unique ID that incrementally increases by 1 for each successful booking.


- **pay** allows a user to pay for an existing unpaid reservation.
  It first checks whether the user has enough money to pay for all the flights in the given reservation. If successful, it updates the reservation to be paid.


- **reservations** lists all reservations for the currently logged-in user.
  Each reservation has ***a unique identifier (which is different for each itinerary) in the entire system***, starting from 1 and increasing by 1 after each reservation is made.


  The user must be logged in to view reservations. The itineraries are displayed using similar format as that used to display the search results, and they should be shown in increasing order of reservation ID under that username.
  Cancelled reservations are not displayed.


- **cancel** lets a user to cancel an existing uncanceled reservation. The user must be logged in to cancel reservations and must provide a valid reservation ID.
  In case of a successful cancellation, corresponding changes to the tables are made. (e.g., if a reservation is already paid, then the customer is refunded).


- **quit** leaves the interactive system and logs out the current user (if logged in).

## Data Model

The flight service system consists of the following logical entities.

- **Flights / Carriers / Months / Weekdays**: 

- **Users**: A user has a username (`varchar`), password (`varbinary`), and balance (`int`) in their account.

- **Itineraries**: An itinerary is either a direct flight (consisting of one flight: origin --> destination) or
  a one-hop flight (consisting of two flights: origin --> stopover city, stopover city --> destination). Itineraries are returned by the search command.

- **Reservations**: A booking for an itinerary, which may consist of one (direct) or two (one-hop) flights.
  Each reservation can either be paid or unpaid, cancelled or not, and has a unique ID.
  
  ## Data Schema
  
```sql
Flights(
    fid int,
    month_id int,
    day_of_month int,
    day_of_week_id int,
    carrier_id varchar(7),
    flight_num int,
    origin_city varchar(34),
    origin_state varchar(47),
    dest_city varchar(34),
    dest_state varchar(46),
    departure_delay int,
    taxi_out int,
    arrival_delay int,
    canceled int,
    actual_time int,
    distance int,
    capacity int,
    price int
  );

  Carriers(
    cid varchar(7),
    name varchar(83)
  );

  Weekdays(
    did int,
    day_of_week varchar(9)
  );

  Months(
    mid int,
    month varchar(9)
  );
  
  Users (
    username VARCHAR(20) PRIMARY KEY,
    password VARBINARY(20),
    balance int,
    salt VARBINARY(20),
  );


  Itineraries (
    it_id int PRIMARY KEY,
    price int,
    first_flight_id int,
    second_flight_id int,
  );


  Reservations (
    ID int IDENTITY(1, 1) PRIMARY KEY,
    itinerary int UNIQUE,
    date int,
    FOREIGN KEY (itinerary) REFERENCES Itineraries(it_id),
    username VARCHAR(20) NOT NULL,
    FOREIGN KEY (username) REFERENCES Users(username),
    paid int,
    cancelled int,
  );

  Capacities (
    fid int PRIMARY KEY,
    capacity int,
  );

```

## User Interface
```
*** Please enter one of the following commands ***
> create <username> <password> <initial amount>
> login <username> <password>
> search <origin city> <destination city> <direct> <day> <num itineraries>
> book <itinerary id>
> pay <reservation id>
> reservations
> cancel <reservation id>
> quit
```
  

