package flightapp;

public class QueryStorage {

    // Canned queries
    public static final String CHECK_FLIGHT_CAPACITY = "SELECT capacity FROM Flights WHERE fid = ?";
    // queries
    // transact sql query
    public static final String TRANCOUNT_SQL = "SELECT @@TRANCOUNT AS tran_count";
    // clear table queries
    public static final String CLEAR_USERS = "DELETE FROM Users";
    public static final String CLEAR_ITINERARIES = "DELETE FROM Itineraries";
    public static final String CLEAR_RESERVATIONS = "TRUNCATE TABLE Reservations";
    public static final String CLEAR_CAPACITIES = "DELETE FROM Capacities";

    // user creation query
    public static final String CREATE_USER = "INSERT INTO Users(username, password, balance, salt) VALUES (?, ?, ?, ?)";
    public static final String GET_SALT = "SELECT salt FROM Users WHERE username = ?";
    // check username and password query
    public static final String LOGIN_USER = "SELECT * FROM Users WHERE username = ? AND password = ?";

    // search direct flights
    public static final String SEARCH_DIRECT_FLIGHTS = "SELECT TOP (?) fid, day_of_month, carrier_id, flight_num, origin_city, "
            + "dest_city, actual_time, capacity, price, canceled FROM Flights WHERE origin_city = ? AND dest_city = ? AND "
            + "day_of_month = ? AND canceled = 0 ORDER BY actual_time ASC, fid";

    // search indirect flights
    public static final String SEARCH_INDIRECT_FLIGHTS =
            "SELECT TOP (?) f1.fid as fid1, f1.day_of_month as dom1, f1.carrier_id as cid1, f1.flight_num as fn1, "
                    + "f1.origin_city as oc1, f1.dest_city as dc1, f1.actual_time as at1, f1.capacity as c1, f1.price as p1, "
                    + "f2.fid as fid2, f2.day_of_month as dom2, f2.carrier_id as cid2, f2.flight_num as fn2, f2.origin_city as oc2, "
                    + "f2.dest_city as dc2, f2.actual_time at2, f2.capacity as c2, f2.price as p2 "
                    + "FROM Flights f1 LEFT JOIN Flights f2 on "
                    + "f1.dest_city = f2.origin_city AND f1.day_of_month = f2.day_of_month WHERE f1.origin_city = ? AND f2.dest_city = ? "
                    + "AND f1.day_of_month = ? AND f1.canceled = 0 AND f2.canceled = 0 ORDER BY (f1.actual_time + f2.actual_time)";

    public static final String CHECK_RESERVATION_STATUS =
            "SELECT COUNT(*) as count FROM Reservations r, Users u WHERE u.username = r.username AND u.username = ? AND r.date = ?";

    public static final String RESERVE_ITINERARY = "INSERT INTO Itineraries VALUES (?, ?, ?, ?)";
    public static final String BOOK_FLIGHT = "INSERT INTO Reservations VALUES (?, ?, ?, ?, ?);";

    public static final String CHECK_OWNERSHIP_STATUS = "SELECT * FROM Reservations WHERE ID = ? AND username = ? AND paid = ?";

    public static final String CHECK_PRICE = "SELECT price FROM Itineraries WHERE it_id = ?";
    public static final String CHECK_BALANCE = "SELECT balance FROM Users WHERE username = ?";

    public static final String UPDATE_BALANCE = "UPDATE Users SET balance = ? WHERE username = ?";
    public static final String UPDATE_PAYMENT_STATUS = "UPDATE Reservations SET paid = ? WHERE itinerary = ?";

    public static final String RETRIEVE_USER_RESERVATIONS = "SELECT * FROM Reservations WHERE username = ?";
    public static final String RETRIEVE_FLIGHTS = "SELECT * FROM Itineraries WHERE it_id = ?";
    public static final String RETRIEVE_FLIGHT_DETAILS = "SELECT * FROM Flights Where fid = ?";

    public static final String RETRIEVE_RESERVATION = "SELECT * FROM Reservations WHERE ID = ? AND username = ? AND cancelled = ?";
    public static final String CANCEL_RESERVATION = "UPDATE Reservations SET cancelled = ? WHERE ID = ?";

    public static final String INSERT_IF_FLIGHT_NOT_EXISTS = "IF NOT EXISTS (SELECT * FROM Capacities WHERE fid = ?) INSERT INTO Capacities VALUES (?, ?);";

    public static final String SELECT_CAPACITY = "SELECT capacity FROM Capacities WHERE fid = ?";
    public static final String UPDATE_CAPACITY = "UPDATE Capacities SET capacity = ? WHERE fid = ?";

}