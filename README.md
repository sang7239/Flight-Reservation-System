# Flight-Reservation-System
```sql
CREATE EXTERNAL TABLE Flights(
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
  ) WITH (DATA_SOURCE = CSE344_EXTERNAL);

  CREATE EXTERNAL TABLE Carriers(
    cid varchar(7),
    name varchar(83)
  ) WITH (DATA_SOURCE = CSE344_EXTERNAL);

  CREATE EXTERNAL TABLE Weekdays(
    did int,
    day_of_week varchar(9)
  ) WITH (DATA_SOURCE = CSE344_EXTERNAL);

  CREATE EXTERNAL TABLE Months
  (
    mid int,
    month varchar(9)
  )
```
