package flightapp;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * Runs queries against a back-end database
 */
public class Query {
  // DB Connection
  private Connection conn;

  // Password hashing parameter constants
  private static final int HASH_STRENGTH = 65536;
  private static final int KEY_LENGTH = 128;

  private static final int PAID = 0;
  private static final int UNPAID = 1;
  private static final int CANCELED = 0;
  private static final int NOT_CANCELED = 1;

  private PreparedStatement checkFlightCapacityStatement;
  private PreparedStatement clearUsersStatement;
  private PreparedStatement clearItinerariesStatement;
  private PreparedStatement clearReservationsStatement;
  private PreparedStatement clearCapacitiesStatement;

  private PreparedStatement createUserStatement;
  private PreparedStatement getSaltStatement;
  private PreparedStatement loginUserStatement;

  private PreparedStatement searchDirectFlightStatement;
  private PreparedStatement searchIndirectFlightStatement;

  private PreparedStatement checkReservationStatusStatement;
  private PreparedStatement reserveItinerary;
  private PreparedStatement bookFlightStatement;
  private PreparedStatement checkOwnershipStatement;
  private PreparedStatement checkPriceStatement;
  private PreparedStatement checkBalanceStatement;
  private PreparedStatement updateBalanceStatement;
  private PreparedStatement updatePaymentStatement;
  private PreparedStatement retrieveReservationsStatement;
  private PreparedStatement retrieveFlightStatement;
  private PreparedStatement retrieveFlightDetailsStatement;
  private PreparedStatement retrieveReservationStatement;
  private PreparedStatement cancelReservationStatement;
  private PreparedStatement insertIfFlightNotExistsStatement;
  private PreparedStatement selectCapacityStatement;
  private PreparedStatement updateCapacityStatement;
  // For check dangling
  private PreparedStatement tranCountStatement;
  // keeps track of login status
  private boolean loggedIn = false;
  private String user = null;
  private Map<Integer, Itinerary> itinerariesMap;
  public Query() throws SQLException, IOException {
    this(null, null, null, null);
  }

  protected Query(String serverURL, String dbName, String adminName, String password)
      throws SQLException, IOException {
    conn = serverURL == null ? openConnectionFromDbConn()
        : openConnectionFromCredential(serverURL, dbName, adminName, password);
    itinerariesMap = new HashMap<>();
    prepareStatements();
  }

  /**
   * Return a connecion by using dbconn.properties file
   *
   * @throws SQLException
   * @throws IOException
   */
  public static Connection openConnectionFromDbConn() throws SQLException, IOException {
    // Connect to the database with the provided connection configuration
    Properties configProps = new Properties();
    configProps.load(new FileInputStream("dbconn.properties"));
    String serverURL = configProps.getProperty("flight_service.server_url");
    String dbName = configProps.getProperty("flight_service.database_name");
    String adminName = configProps.getProperty("flight_service.username");
    String password = configProps.getProperty("flight_service.password");
    return openConnectionFromCredential(serverURL, dbName, adminName, password);
  }

  /**
   * Return a connecion by using the provided parameter.
   *
   * @param serverURL example: example.database.widows.net
   * @param dbName    database name
   * @param adminName username to login server
   * @param password  password to login server
   *
   * @throws SQLException
   */
  protected static Connection openConnectionFromCredential(String serverURL, String dbName,
      String adminName, String password) throws SQLException {
    String connectionUrl =
        String.format("jdbc:sqlserver://%s:1433;databaseName=%s;user=%s;password=%s", serverURL,
            dbName, adminName, password);
    Connection conn = DriverManager.getConnection(connectionUrl);

    // By default, automatically commit after each statement
    conn.setAutoCommit(true);

    // By default, set the transaction isolation level to serializable
    conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

    return conn;
  }

  /**
   * Get underlying connection
   */
  public Connection getConnection() {
    return conn;
  }

  /**
   * Closes the application-to-database connection
   */
  public void closeConnection() throws SQLException {
    conn.close();
  }

  /**
   * Clear the data in any custom tables created.
   *
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      clearReservationsStatement.clearParameters();
      clearReservationsStatement.executeUpdate();
      clearUsersStatement.clearParameters();
      clearUsersStatement.executeUpdate();
      clearItinerariesStatement.clearParameters();
      clearItinerariesStatement.executeUpdate();
      clearCapacitiesStatement.clearParameters();
      clearCapacitiesStatement.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    checkFlightCapacityStatement = conn.prepareStatement(QueryStorage.CHECK_FLIGHT_CAPACITY);
    tranCountStatement = conn.prepareStatement(QueryStorage.TRANCOUNT_SQL);
    clearUsersStatement = conn.prepareStatement(QueryStorage.CLEAR_USERS);
    clearItinerariesStatement = conn.prepareStatement(QueryStorage.CLEAR_ITINERARIES);
    clearReservationsStatement = conn.prepareStatement(QueryStorage.CLEAR_RESERVATIONS);
    createUserStatement = conn.prepareStatement(QueryStorage.CREATE_USER);
    getSaltStatement = conn.prepareStatement(QueryStorage.GET_SALT);
    loginUserStatement = conn.prepareStatement(QueryStorage.LOGIN_USER);
    searchDirectFlightStatement = conn.prepareStatement(QueryStorage.SEARCH_DIRECT_FLIGHTS);
    searchIndirectFlightStatement = conn.prepareStatement(QueryStorage.SEARCH_INDIRECT_FLIGHTS);
    checkReservationStatusStatement = conn.prepareStatement(QueryStorage.CHECK_RESERVATION_STATUS);
    bookFlightStatement = conn.prepareStatement(QueryStorage.BOOK_FLIGHT, Statement.RETURN_GENERATED_KEYS);
    reserveItinerary = conn.prepareStatement(QueryStorage.RESERVE_ITINERARY);
    checkOwnershipStatement = conn.prepareStatement(QueryStorage.CHECK_OWNERSHIP_STATUS);
    checkPriceStatement = conn.prepareStatement(QueryStorage.CHECK_PRICE);
    checkBalanceStatement = conn.prepareStatement(QueryStorage.CHECK_BALANCE);
    updateBalanceStatement = conn.prepareStatement(QueryStorage.UPDATE_BALANCE);
    updatePaymentStatement = conn.prepareStatement(QueryStorage.UPDATE_PAYMENT_STATUS);
    retrieveReservationsStatement = conn.prepareStatement(QueryStorage.RETRIEVE_USER_RESERVATIONS);
    retrieveFlightStatement = conn.prepareStatement(QueryStorage.RETRIEVE_FLIGHTS);
    retrieveFlightDetailsStatement  = conn.prepareStatement(QueryStorage.RETRIEVE_FLIGHT_DETAILS);
    retrieveReservationStatement = conn.prepareStatement(QueryStorage.RETRIEVE_RESERVATION);
    cancelReservationStatement = conn.prepareStatement(QueryStorage.CANCEL_RESERVATION);
    insertIfFlightNotExistsStatement = conn.prepareStatement(QueryStorage.INSERT_IF_FLIGHT_NOT_EXISTS);
    selectCapacityStatement = conn.prepareStatement(QueryStorage.SELECT_CAPACITY);
    updateCapacityStatement = conn.prepareStatement(QueryStorage.UPDATE_CAPACITY);
    clearCapacitiesStatement = conn.prepareStatement(QueryStorage.CLEAR_CAPACITIES);
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username user's username
   * @param password user's password
   *
   * @return If someone has already logged in, then return "User already logged in\n" For all other
   *         errors, return "Login failed\n". Otherwise, return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password) {
    if (loggedIn) return "User already logged in\n";
    try {
      byte[] salt = findSaltValue(username);
      byte[] passwordHash = generateHash(password, salt);
      loginUserStatement.clearParameters();
      loginUserStatement.setString(1, username);
      loginUserStatement.setBytes(2, passwordHash);
      ResultSet result = loginUserStatement.executeQuery();
      if (result.next()) {
        this.loggedIn = true;
        this.user = username;
        return "Logged in as " + username + "\n";
      } else {
        return "Login failed\n";
      }
    } catch (SQLException ex) {
      return "Login failed\n";
    }
    finally {
      checkDanglingTransaction();
    }
  }

  private byte[] findSaltValue(String username) throws SQLException {
      getSaltStatement.clearParameters();
      getSaltStatement.setString(1, username);
      ResultSet result = getSaltStatement.executeQuery();
      if (result.next()) {
        return result.getBytes("salt");
      }
      return null;
  }

  /**
   * Implement the create user function.
   *
   * @param username   new user's username. User names are unique the system.
   * @param password   new user's password.
   * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure
   *                   otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
   */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    if (initAmount < 0) return "Failed to create user\n";
    try {
      byte[] salt = generateSalt();
      byte[] hash = generateHash(password, salt);
      createUserStatement.clearParameters();
      createUserStatement.setString(1, username);
      createUserStatement.setBytes(2, hash);
      createUserStatement.setInt(3, initAmount);
      createUserStatement.setBytes(4, salt);
      createUserStatement.executeUpdate();
      return "Created user " + username + "\n";
    } catch(SQLException e) {
      return "Failed to create user\n";
    }
    finally {
      checkDanglingTransaction();
    }
  }

  private byte[] generateSalt() {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    return salt;
  }

  private byte[] generateHash(String password, byte[] salt) {
    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_STRENGTH, KEY_LENGTH);
    SecretKeyFactory factory = null;
    byte[] hash = null;
    try {
      factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      hash = factory.generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
      throw new IllegalStateException();
    }
    return hash;
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination city, on the given day
   * of the month. If {@code directFlight} is true, it only searches for direct flights, otherwise
   * is searches for direct flights and flights with two "hops." Only searches for up to the number
   * of itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight        if true, then only search for direct flights, otherwise include
   *                            indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return
   *
   * @return If no itineraries were found, return "No flights match your selection\n". If an error
   *         occurs, then return "Failed to search\n".
   *
   *         Otherwise, the sorted itineraries printed in the following format:
   *
   *         Itinerary [itinerary number]: [number of flights] flight(s), [total flight time]
   *         minutes\n [first flight in itinerary]\n ... [last flight in itinerary]\n
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *         Itinerary numbers in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  public String transaction_search(String originCity, String destinationCity, boolean directFlight,
      int dayOfMonth, int numberOfItineraries) {
    List<Itinerary> itineraries = new ArrayList<>();
      int count = 0;
      try {
        // one hop itineraries
        searchDirectFlightStatement.clearParameters();
        searchDirectFlightStatement.setInt(1, numberOfItineraries);
        searchDirectFlightStatement.setString(2, originCity);
        searchDirectFlightStatement.setString(3, destinationCity);
        searchDirectFlightStatement.setInt(4, dayOfMonth);
        ResultSet oneHopResults = searchDirectFlightStatement.executeQuery();
        while (oneHopResults.next()) {
          int fid = oneHopResults.getInt("fid");
          int result_dayOfMonth = oneHopResults.getInt("day_of_month");
          String result_carrierId = oneHopResults.getString("carrier_id");
          String result_flightNum = oneHopResults.getString("flight_num");
          String result_originCity = oneHopResults.getString("origin_city");
          String result_destCity = oneHopResults.getString("dest_city");
          int result_time = oneHopResults.getInt("actual_time");
          int result_capacity = oneHopResults.getInt("capacity");
          int result_price = oneHopResults.getInt("price");
          itineraries.add(new Itinerary(new Flight(fid, result_dayOfMonth, result_carrierId, result_flightNum, result_originCity,
                    result_destCity, result_time, result_capacity, result_price)));
          count++;
        }
        oneHopResults.close();
        if (!directFlight && count < numberOfItineraries) {
          int numLeft = numberOfItineraries - itineraries.size();
          findIndirectFlights(itineraries, numLeft, originCity, destinationCity, dayOfMonth);
        }
      } catch (SQLException e) {
        e.printStackTrace();
      } finally {
      checkDanglingTransaction();
    }
    return listItinerary(itineraries);
  }

  private void findIndirectFlights(List<Itinerary> itineraries, int numLeft, String originCity, String destinationCity,
                                   int dayOfMonth) {
    try {
      searchIndirectFlightStatement.clearParameters();
      searchIndirectFlightStatement.setInt(1, numLeft);
      searchIndirectFlightStatement.setString(2, originCity);
      searchIndirectFlightStatement.setString(3, destinationCity);
      searchIndirectFlightStatement.setInt(4, dayOfMonth);
      ResultSet twoHopResults = searchIndirectFlightStatement.executeQuery();
      while (twoHopResults.next()) {
        int fid1 = twoHopResults.getInt("fid1");
        int result_dayOfMonth1 = twoHopResults.getInt("dom1");
        String result_carrierId1 = twoHopResults.getString("cid1");
        String result_flightNum1 = twoHopResults.getString("fn1");
        String result_originCity1 = twoHopResults.getString("oc1");
        String result_destCity1 = twoHopResults.getString("dc1");
        int result_time1 = twoHopResults.getInt("at1");
        int result_capacity1 = twoHopResults.getInt("c1");
        int result_price1 = twoHopResults.getInt("p1");

        int fid2 = twoHopResults.getInt("fid2");
        int result_dayOfMonth2 = twoHopResults.getInt("dom2");
        String result_carrierId2 = twoHopResults.getString("cid2");
        String result_flightNum2 = twoHopResults.getString("fn2");
        String result_originCity2 = twoHopResults.getString("oc2");
        String result_destCity2 = twoHopResults.getString("dc2");
        int result_time2 = twoHopResults.getInt("at2");
        int result_capacity2 = twoHopResults.getInt("c2");
        int result_price2 = twoHopResults.getInt("p2");
        Flight f1 = new Flight(fid1, result_dayOfMonth1, result_carrierId1, result_flightNum1, result_originCity1,
                result_destCity1, result_time1, result_capacity1, result_price1);
        Flight f2 = new Flight(fid2, result_dayOfMonth2, result_carrierId2, result_flightNum2, result_originCity2,
                result_destCity2, result_time2, result_capacity2, result_price2);
        itineraries.add(new Itinerary(f1, f2));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      checkDanglingTransaction();
    }
  }

  private String listItinerary(List<Itinerary> itineraries) {
    Collections.sort(itineraries);
    this.itinerariesMap.clear();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < itineraries.size(); i++) {
      Itinerary itinerary = itineraries.get(i);
      itinerary.setId(i);
      if (!itinerary.layover) {
        sb.append("Itinerary " + i + ": 1 flight(s), " + itinerary.totalTime + " minutes\n");
        sb.append(itinerary.f1.toString() + "\n");
      } else {
        sb.append("Itinerary " + i + ": 2 flight(s), " + itinerary.totalTime + " minutes\n");
        sb.append(itinerary.f1.toString() + "\n");
        sb.append(itinerary.f2.toString() + "\n");
      }
      this.itinerariesMap.put(i, itinerary);
    }
    return sb.toString();
  }

  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is returned by search in
   *                    the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations, not logged in\n".
   *         If the user is trying to book an itinerary with an invalid ID or without having done a
   *         search, then return "No such itinerary {@code itineraryId}\n". If the user already has
   *         a reservation on the same day as the one that they are trying to book now, then return
   *         "You cannot book two flights in the same day\n". For all other errors, return "Booking
   *         failed\n".
   *
   *         And if booking succeeded, return "Booked flight(s), reservation ID: [reservationId]\n"
   *         where reservationId is a unique number in the reservation system that starts from 1 and
   *         increments by 1 each time a successful reservation is made by any user in the system.
   */
  public String transaction_book(int itineraryId) {
    if (!loggedIn) return "Cannot book reservations, not logged in\n";
    if (!this.itinerariesMap.containsKey(itineraryId)) { return "No such itinerary " + itineraryId + "\n"; }
    boolean booked = false;
    int id = -1;
    try {
      Itinerary itinerary = this.itinerariesMap.get(itineraryId);
      int day = itinerary.f1.dayOfMonth;
      int itId = itinerary.hashCode();
      if (!validateReservation(day)) {
        return "You cannot book two flights in the same day\n";
      }
      beginTransaction();
      insertIntoCapacity(itinerary.f1.fid);
      if (itinerary.layover) {
        insertIntoCapacity(itinerary.f2.fid);
      }
      if(!updateCapacity(itinerary.f1.fid)) {
        rollBackTransaction();
        return "Booking failed\n";
      }
      if (itinerary.layover && !updateCapacity(itinerary.f2.fid) ) {
        rollBackTransaction();
        return "Booking failed\n";
      }
      if (!reserveTicket(itinerary, itId)) {
        rollBackTransaction();
        return "Booking failed\n";
      }
      bookFlightStatement.clearParameters();
      bookFlightStatement.setInt(1, itId);
      bookFlightStatement.setInt(2, day);
      bookFlightStatement.setString(3, user);
      bookFlightStatement.setInt(4, 1);
      bookFlightStatement.setInt(5, 1);
      bookFlightStatement.executeUpdate();
      ResultSet result = bookFlightStatement.getGeneratedKeys();
      booked = true;
      if (result.next()) {
        id = result.getInt(1);
      }
      commitTransaction();
    } catch (SQLException ex) {
      try {
        rollBackTransaction();
      } catch (SQLException e) {
        e.printStackTrace();
      }
      ex.printStackTrace();
    }
    finally {
      checkDanglingTransaction();
    }
    if (booked) {
      return "Booked flight(s), reservation ID: " + id + "\n";
    }
    return "Booking failed\n";
  }

  private void insertIntoCapacity(int fid) {
    try {
      int capacity = checkFlightCapacity(fid);
      insertIfFlightNotExistsStatement.clearParameters();
      insertIfFlightNotExistsStatement.setInt(1, fid);
      insertIfFlightNotExistsStatement.setInt(2, fid);
      insertIfFlightNotExistsStatement.setInt(3, capacity);
    } catch(SQLException ex) {
      ex.printStackTrace();
    }
  }

  private boolean updateCapacity(int fid) {
    try {
      selectCapacityStatement.clearParameters();
      selectCapacityStatement.setInt(1, fid);
      ResultSet resultSet = selectCapacityStatement.executeQuery();
      if (resultSet.next()) {
        int capacity = resultSet.getInt("capacity");
        if (capacity > 0) {
          updateCapacityStatement.setInt(1, capacity - 1);
          updateCapacityStatement.setInt(2, fid);
          updateCapacityStatement.executeUpdate();
          return true;
        } else {
          return false;
        }
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
    }
    return true;
  }

  private boolean reserveTicket(Itinerary itinerary, int itId) {
    try {
      reserveItinerary.clearParameters();
      reserveItinerary.setInt(1, itId);
      reserveItinerary.setInt(2, itinerary.price);
      reserveItinerary.setInt(3, itinerary.f1.fid);
      if (itinerary.layover) {
        reserveItinerary.setInt(4, itinerary.f2.fid);
      } else {
        reserveItinerary.setInt(4, -1);
      }
      reserveItinerary.executeUpdate();
      return true;
    } catch(SQLException ex) {
      return false;
    }
  }

  private boolean validateReservation(int day) {
    try {
      checkReservationStatusStatement.clearParameters();
      checkReservationStatusStatement.setString(1, user);
      checkReservationStatusStatement.setInt(2, day);
      ResultSet result = checkReservationStatusStatement.executeQuery();
      if (result.next()) {
        return result.getInt("count") == 0;
      }
    } catch(SQLException e) {
      return false;
    }
    return false;
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n" If the reservation
   *         is not found / not under the logged in user's name, then return "Cannot find unpaid
   *         reservation [reservationId] under user: [username]\n" If the user does not have enough
   *         money in their account, then return "User has only [balance] in account but itinerary
   *         costs [cost]\n" For all other errors, return "Failed to pay for reservation
   *         [reservationId]\n"
   *
   *         If successful, return "Paid reservation: [reservationId] remaining balance:
   *         [balance]\n" where [balance] is the remaining balance in the user's account.
   */
  public String transaction_pay(int reservationId) {
    if (!loggedIn) return "Cannot pay, not logged in\n";
    try {
      checkOwnershipStatement.clearParameters();
      checkOwnershipStatement.setInt(1, reservationId);
      checkOwnershipStatement.setString(2, user);
      checkOwnershipStatement.setInt(3, UNPAID);
      ResultSet resultSet = checkOwnershipStatement.executeQuery();
      if (resultSet.next()) {
        int itId = resultSet.getInt("itinerary");
        int price = getItineraryPrice(itId);
        int balance = getUserBalance();
        if (price > balance) return "User has only " + balance + " in account but itinerary costs " + price + "\n";
        else {
          int remaining = balance - price;
          updateBalanceStatement.clearParameters();
          updateBalanceStatement.setInt(1, remaining);
          updateBalanceStatement.setString(2, user);
          updateBalanceStatement.executeUpdate();
          updatePaymentStatement.clearParameters();
          updatePaymentStatement.setInt(1, PAID);
          updatePaymentStatement.setInt(2, itId);
          updatePaymentStatement.executeUpdate();
          return "Paid reservation: " + reservationId + " remaining balance: " + remaining + "\n";
        }
      } else {
        return "Cannot find unpaid reservation " + reservationId + " under user: " + user + "\n";
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    finally {
      checkDanglingTransaction();
    }
    return "Failed to pay for reservation " + reservationId + "\n";
  }

  private int getItineraryPrice(int itId) {
    try {
      checkPriceStatement.clearParameters();
      checkPriceStatement.setInt(1, itId);
      ResultSet resultSet = checkPriceStatement.executeQuery();
      if (resultSet.next()) {
        return resultSet.getInt("price");
      }
    } catch(SQLException e) {
      e.printStackTrace();
    }
    return -1;
  }

  private int getUserBalance() {
    try {
      checkBalanceStatement.clearParameters();
      checkBalanceStatement.setString(1, user);
      ResultSet resultSet = checkBalanceStatement.executeQuery();
      if (resultSet.next()) {
        return resultSet.getInt("balance");
      }
    } catch(SQLException e) {
      e.printStackTrace();
    }
    return -1;
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not logged in\n" If
   *         the user has no reservations, then return "No reservations found\n" For all other
   *         errors, return "Failed to retrieve reservations\n"
   *
   *         Otherwise return the reservations in the following format:
   *
   *         Reservation [reservation ID] paid: [true or false]:\n [flight 1 under the
   *         reservation]\n [flight 2 under the reservation]\n Reservation [reservation ID] paid:
   *         [true or false]:\n [flight 1 under the reservation]\n [flight 2 under the
   *         reservation]\n ...
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations() {
    if (!loggedIn) return "Cannot view reservations, not logged in \n";
    try {
      retrieveReservationsStatement.clearParameters();
      retrieveReservationsStatement.setString(1, user);
      ResultSet resultSet = retrieveReservationsStatement.executeQuery();
      StringBuilder sb = new StringBuilder();
      while (resultSet.next()) {
          int id = resultSet.getInt("ID");
          String paid = resultSet.getInt("paid") == 0 ? "true" : "false";
          int itinerary = resultSet.getInt("itinerary");
          String flightInfo = getFlightInfo(itinerary);
          sb.append("Reservation " + id + " paid: " + paid + ":" + "\n");
          sb.append(flightInfo);
      }
      return sb.toString();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    finally {
      checkDanglingTransaction();
    }
    return "Failed to retrieve reservations\n";
  }

  private String getFlightInfo(int itinerary) {
    StringBuilder sb = new StringBuilder();
    try {
      retrieveFlightStatement.clearParameters();
      retrieveFlightStatement.setInt(1, itinerary);
      ResultSet resultSet = retrieveFlightStatement.executeQuery();
      if (resultSet.next()) {
        int fid1 = resultSet.getInt("first_flight_id");
        int fid2 = resultSet.getInt("second_flight_id");
        sb.append(getFlightDetails(fid1));
        if (fid2 != -1) {
          sb.append(getFlightDetails(fid2));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      checkDanglingTransaction();
    }
    return sb.toString();
  }

  private String getFlightDetails(int fid) {
    StringBuilder sb = new StringBuilder();
    try {
      retrieveFlightDetailsStatement.clearParameters();
      retrieveFlightDetailsStatement.setInt(1, fid);
      ResultSet resultSet = retrieveFlightDetailsStatement.executeQuery();
      if (resultSet.next()) {
        int id = resultSet.getInt("fid");
        int result_dayOfMonth = resultSet.getInt("day_of_month");
        String result_carrierId = resultSet.getString("carrier_id");
        String result_flightNum = resultSet.getString("flight_num");
        String result_originCity = resultSet.getString("origin_city");
        String result_destCity = resultSet.getString("dest_city");
        int result_time = resultSet.getInt("actual_time");
        int result_capacity = resultSet.getInt("capacity");
        int result_price = resultSet.getInt("price");
        int cancelled = resultSet.getInt("canceled");
        if (cancelled == 0) {
          Flight flight = new Flight(id, result_dayOfMonth, result_carrierId, result_flightNum, result_originCity,
                  result_destCity, result_time, result_capacity, result_price);
          sb.append(flight.toString() + "\n");
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      checkDanglingTransaction();
    }
    return sb.toString();
  }

  /**
   * Implements the cancel operation.
   *
   * @param reservationId the reservation ID to cancel
   *
   * @return If no user has logged in, then return "Cannot cancel reservations, not logged in\n" For
   *         all other errors, return "Failed to cancel reservation [reservationId]\n"
   *
   *         If successful, return "Canceled reservation [reservationId]\n"
   *
   *         Even though a reservation has been canceled, its ID should not be reused by the system.
   */
  public String transaction_cancel(int reservationId) {
    if (!loggedIn) return "Cannot cancel reservations, not logged in\n";
    boolean canceled = false;
    try {
      beginTransaction();
      retrieveReservationStatement.clearParameters();
      retrieveReservationStatement.setInt(1, reservationId);
      retrieveReservationStatement.setString(2, user);
      retrieveReservationStatement.setInt(3, NOT_CANCELED);
      ResultSet resultSet = retrieveReservationStatement.executeQuery();
      if (resultSet.next()) {
        int paid = resultSet.getInt("paid");
        int itId = resultSet.getInt("itinerary");
        int price = getItineraryPrice(itId);
        int balance = getUserBalance();
        if (paid == 0) {
          updateBalanceStatement.clearParameters();
          updateBalanceStatement.setInt(1, balance + price);
          updateBalanceStatement.setString(2, user);
          updateBalanceStatement.executeUpdate();
          updatePaymentStatement.clearParameters();
          updatePaymentStatement.setInt(1, UNPAID);
          updatePaymentStatement.setInt(2, itId);
          updatePaymentStatement.executeUpdate();
        }
        cancelReservationStatement.clearParameters();
        cancelReservationStatement.setInt(1, CANCELED);
        cancelReservationStatement.setInt(2, reservationId);
        cancelReservationStatement.executeUpdate();
        canceled = true;
      }
      commitTransaction();
    } catch(SQLException ex) {
      try {
        rollBackTransaction();
      } catch (SQLException e) {
        e.printStackTrace();
      }
      ex.printStackTrace();
    } finally {
      checkDanglingTransaction();
    }
    if (canceled) {
      return "Canceled reservation " + reservationId + "\n";
    }
    return "Failed to cancel reservation " + reservationId + "\n";
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    checkFlightCapacityStatement.clearParameters();
    checkFlightCapacityStatement.setInt(1, fid);
    ResultSet results = checkFlightCapacityStatement.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();
    return capacity;
  }

  /**
   * Throw IllegalStateException if transaction not completely complete, rollback.
   *
   */
  private void checkDanglingTransaction() {
    try {
      try (ResultSet rs = tranCountStatement.executeQuery()) {
        rs.next();
        int count = rs.getInt("tran_count");
        if (count > 0) {
          throw new IllegalStateException(
              "Transaction not fully commit/rollback. Number of transaction in process: " + count);
        }
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Database error", e);
    }
  }

  private static boolean isDeadLock(SQLException ex) {
    return ex.getErrorCode() == 1205;
  }

  private void beginTransaction() throws SQLException {
    conn.setAutoCommit(false);
  }

  private void commitTransaction() throws SQLException {
    conn.commit();
    conn.setAutoCommit(true);
  }

  private void rollBackTransaction() throws SQLException {
    conn.rollback();
    conn.setAutoCommit(true);
  }
}