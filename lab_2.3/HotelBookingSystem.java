import java.sql.*;
import java.util.Scanner;
import java.io.Console;

/**
 * DBVS Lab 2.3
 * 
 * Hotel Booking Management System
 * 
 * Implements:
 * - Search: Search for rooms by room number or price range
 * - Insert: Register new guest, create new booking with items (TRANSACTION)
 * - Update: Cancel booking (restores room availability via trigger)
 * - Delete: Delete guest (cascades to bookings and ratings)
 */
public class HotelBookingSystem {
  private static final Scanner scanner = new Scanner(System.in);
  private static final String DB_URL = "jdbc:postgresql://pgsql3.mif/studentu";

  public static void main(String[] args) {
    loadDriver();

    try (Connection conn = getConnection()) {
      if (conn != null) {
        runMenu(conn);
      }
    } catch (SQLException e) {
      System.err.println("DATABASE ERROR: " + e.getMessage());
    } finally {
      scanner.close();
    }
  }

  private static void loadDriver() {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      System.err.println("PostgreSQL JDBC driver not found.");
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static Connection getConnection() throws SQLException {
    Console console = System.console();

    if (console == null) {
      System.err.println("ERROR: Program must be run in terminal!");
      System.err.println("Use: java HotelBookingSystem");
      System.exit(1);
    }

    System.out.println("=== DATABASE CONNECTION ===");
    String user = console.readLine("Username: ");
    char[] password = console.readPassword("Password: ");

    Connection conn = null;
    try {
      conn = DriverManager.getConnection(DB_URL, user, new String(password));
      System.out.println("Connected successfully.\n");
    } catch (SQLException e) {
      System.err.println("Connection failed!");
      System.err.println(e.getMessage());
      throw e;
    } finally {
      java.util.Arrays.fill(password, ' ');
    }

    return conn;
  }

  private static void runMenu(Connection conn) throws SQLException {
    boolean running = true;

    while (running) {
      printMenu();

      if (!scanner.hasNextInt()) {
        System.out.println("Invalid input!");
        scanner.next();
        continue;
      }

      int choice = scanner.nextInt();
      scanner.nextLine();

      try {
        switch (choice) {
          case 1:
            searchRooms(conn);
            break;
          case 2:
            registerGuest(conn);
            break;
          case 3:
            createBookingWithItems(conn);
            break;
          case 4:
            cancelBooking(conn);
            break;
          case 5:
            deleteGuest(conn);
            break;
          case 6:
            addRoomRating(conn);
            break;
          case 7:
            viewSystemData(conn);
            break;
          case 0:
            running = false;
            break;
          default:
            System.out.println("Invalid choice!");
        }

        if (running && choice != 0) {
          System.out.print("\nPress ENTER to continue...");
          scanner.nextLine();
        }
      } catch (SQLException e) {
        System.err.println("\nERROR: " + e.getMessage());
        System.out.print("\nPress ENTER to continue...");
        scanner.nextLine();
      }
    }
  }

  private static void printMenu() {
    System.out.println("\n========================================");
    System.out.println("  HOTEL BOOKING MANAGEMENT SYSTEM");
    System.out.println("========================================");
    System.out.println("1. Search for rooms");
    System.out.println("2. Register new guest");
    System.out.println("3. Create booking (TRANSACTION)");
    System.out.println("4. Cancel booking");
    System.out.println("5. Delete guest");
    System.out.println("6. Add room rating");
    System.out.println("7. View system data");
    System.out.println("0. Exit");
    System.out.println("========================================");
    System.out.print("Choice: ");
  }

  /**
   * SEARCH - Search for rooms by room number or price range
   * Uses 2 related tables: ROOM and RATES
   */
  private static void searchRooms(Connection conn) throws SQLException {
    System.out.println("\n=== ROOM SEARCH ===");
    System.out.println("1. Search by room number");
    System.out.println("2. Search by price range");
    System.out.print("Choice: ");

    int searchType = scanner.nextInt();
    scanner.nextLine();

    if (searchType == 1) {
      System.out.print("Enter room number (or part): ");
      String roomNumber = scanner.nextLine();

      String sql = "SELECT r.room_id, r.room_number, r.price_per_night, " +
          "r.availability, r.description, " +
          "COUNT(rt.rating) as rating_count, " +
          "COALESCE(AVG(rt.rating), 0) as avg_rating " +
          "FROM ROOM r " +
          "LEFT JOIN RATES rt ON r.room_id = rt.room_id " +
          "WHERE LOWER(r.room_number) LIKE LOWER(?) " +
          "GROUP BY r.room_id, r.room_number, r.price_per_night, " +
          "r.availability, r.description " +
          "ORDER BY r.room_number";

      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, "%" + roomNumber + "%");
        ResultSet rs = pstmt.executeQuery();

        printRoomSearchResults(rs);
        rs.close();
      }

    } else if (searchType == 2) {
      System.out.print("Minimum price: ");
      double minPrice = scanner.nextDouble();
      System.out.print("Maximum price: ");
      double maxPrice = scanner.nextDouble();
      scanner.nextLine();

      String sql = "SELECT r.room_id, r.room_number, r.price_per_night, " +
          "r.availability, r.description, " +
          "COUNT(rt.rating) as rating_count, " +
          "COALESCE(AVG(rt.rating), 0) as avg_rating " +
          "FROM ROOM r " +
          "LEFT JOIN RATES rt ON r.room_id = rt.room_id " +
          "WHERE r.price_per_night BETWEEN ? AND ? " +
          "GROUP BY r.room_id, r.room_number, r.price_per_night, " +
          "r.availability, r.description " +
          "ORDER BY r.price_per_night";

      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setDouble(1, minPrice);
        pstmt.setDouble(2, maxPrice);
        ResultSet rs = pstmt.executeQuery();

        printRoomSearchResults(rs);
        rs.close();
      }
    }
  }

  private static void printRoomSearchResults(ResultSet rs) throws SQLException {
    boolean found = false;
    System.out.println("\n" + "-".repeat(100));
    System.out.printf("%-5s %-12s %12s %12s %8s %8s %-30s%n",
        "ID", "Room #", "Price/Night", "Available", "Ratings", "Avg Rate", "Description");
    System.out.println("-".repeat(100));

    while (rs.next()) {
      found = true;
      String description = rs.getString("description");
      if (description != null && description.length() > 30) {
        description = description.substring(0, 27) + "...";
      }

      System.out.printf("%-5d %-12s %12.2f %12d %8d %8.1f %-30s%n",
          rs.getInt("room_id"),
          rs.getString("room_number"),
          rs.getDouble("price_per_night"),
          rs.getInt("availability"),
          rs.getInt("rating_count"),
          rs.getDouble("avg_rating"),
          description);
    }

    System.out.println("-".repeat(100));
    if (!found) {
      System.out.println("No rooms found.");
    }
  }

  /**
   * INSERT - Register new guest
   * Shows existing guest IDs with their names before insertion
   */
  private static void registerGuest(Connection conn) throws SQLException {
    System.out.println("\n=== NEW GUEST REGISTRATION ===");

    // Show existing guests
    showAllGuests(conn);

    System.out.print("\nFirst name: ");
    String firstName = scanner.nextLine();

    System.out.print("Last name: ");
    String lastName = scanner.nextLine();

    System.out.print("Email: ");
    String email = scanner.nextLine();

    if (!email.matches(".+@.+\\..+")) {
      System.out.println("Invalid email format!");
      return;
    }

    String sql = "INSERT INTO GUEST (first_name, last_name, email) VALUES (?, ?, ?)";

    try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.setString(1, firstName);
      pstmt.setString(2, lastName);
      pstmt.setString(3, email);

      pstmt.executeUpdate();

      ResultSet keys = pstmt.getGeneratedKeys();
      if (keys.next()) {
        System.out.println("\nSUCCESS! Guest registered with ID: " + keys.getInt(1));
      }
      keys.close();
    } catch (SQLException e) {
      if ("23505".equals(e.getSQLState())) {
        System.out.println("Email already exists!");
      } else {
        throw e;
      }
    }
  }

  /**
   * INSERT - Create booking with items (TRANSACTION)
   * Uses multiple tables: GUEST, ROOM, BOOKING, BOOKING_ITEM
   * Either everything succeeds or everything is rolled back
   * Triggers automatically update total_price and decrease availability
   */
  private static void createBookingWithItems(Connection conn) throws SQLException {
    System.out.println("\n=== NEW BOOKING CREATION (TRANSACTION) ===");

    // Show guests
    showAllGuests(conn);

    System.out.print("\nGuest ID: ");
    int guestId = scanner.nextInt();
    scanner.nextLine();

    // Check if guest exists
    String checkGuest = "SELECT guest_id FROM GUEST WHERE guest_id = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(checkGuest)) {
      pstmt.setInt(1, guestId);
      ResultSet rs = pstmt.executeQuery();
      if (!rs.next()) {
        System.out.println("Guest not found!");
        rs.close();
        return;
      }
      rs.close();
    }

    // Address
    System.out.println("\nDelivery address:");
    System.out.print("Country: ");
    String country = scanner.nextLine();
    System.out.print("City: ");
    String city = scanner.nextLine();
    System.out.print("Postal code: ");
    String postalCode = scanner.nextLine();
    System.out.print("Address line: ");
    String addressLine = scanner.nextLine();

    // START TRANSACTION
    boolean autoCommit = conn.getAutoCommit();
    conn.setAutoCommit(false);

    try {
      // 1. Create booking
      String insertBooking = "INSERT INTO BOOKING (guest_id, status, country, city, postal_code, address_line) " +
          "VALUES (?, 'New', ?, ?, ?, ?) RETURNING booking_id";

      int bookingId;
      try (PreparedStatement pstmt = conn.prepareStatement(insertBooking)) {
        pstmt.setInt(1, guestId);
        pstmt.setString(2, country);
        pstmt.setString(3, city);
        pstmt.setString(4, postalCode);
        pstmt.setString(5, addressLine);

        ResultSet rs = pstmt.executeQuery();
        if (!rs.next()) {
          throw new SQLException("Failed to create booking");
        }
        bookingId = rs.getInt(1);
        rs.close();
      }

      System.out.println("\nBooking created with ID: " + bookingId);

      // 2. Add rooms
      boolean addingItems = true;
      int itemNumber = 1;

      while (addingItems) {
        System.out.println("\n--- Adding room ---");
        showAvailableRooms(conn);

        System.out.print("\nRoom ID (0 - finish): ");
        int roomId = scanner.nextInt();
        scanner.nextLine();

        if (roomId == 0) {
          if (itemNumber == 1) {
            throw new SQLException("Booking must have at least one room!");
          }
          addingItems = false;
          continue;
        }

        // Get room info
        String getRoom = "SELECT room_number, price_per_night, availability FROM ROOM WHERE room_id = ?";
        double price;
        int maxAvailability;
        String roomNumber;

        try (PreparedStatement pstmt = conn.prepareStatement(getRoom)) {
          pstmt.setInt(1, roomId);
          ResultSet rs = pstmt.executeQuery();

          if (!rs.next()) {
            System.out.println("Room not found!");
            rs.close();
            continue;
          }

          roomNumber = rs.getString("room_number");
          price = rs.getDouble("price_per_night");
          maxAvailability = rs.getInt("availability");
          rs.close();
        }

        System.out.println("Room: " + roomNumber);
        System.out.println("Price per night: " + price + " EUR");
        System.out.println("Available: " + maxAvailability);

        System.out.print("Number of nights: ");
        int nights = scanner.nextInt();
        scanner.nextLine();

        if (nights <= 0 || nights > maxAvailability) {
          System.out.println("Invalid number of nights!");
          continue;
        }

        // Insert booking item (trigger will decrease availability)
        String insertItem = "INSERT INTO BOOKING_ITEM (booking_id, item_number, room_id, nights, price) " +
            "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertItem)) {
          pstmt.setInt(1, bookingId);
          pstmt.setInt(2, itemNumber);
          pstmt.setInt(3, roomId);
          pstmt.setInt(4, nights);
          pstmt.setDouble(5, price);

          pstmt.executeUpdate();
        }

        System.out.println("Room added!");
        itemNumber++;
      }

      // COMMIT
      conn.commit();
      System.out.println("\n=== BOOKING SUCCESSFULLY CREATED ===");
      System.out.println("Total price calculated automatically by trigger.");

    } catch (SQLException e) {
      // ROLLBACK
      conn.rollback();
      System.out.println("\nBOOKING FAILED - All changes rolled back!");
      throw e;
    } finally {
      conn.setAutoCommit(autoCommit);
    }
  }

  /**
   * UPDATE - Cancel booking
   * Trigger automatically restores room availability
   */
  private static void cancelBooking(Connection conn) throws SQLException {
    System.out.println("\n=== BOOKING CANCELLATION ===");

    showActiveBookings(conn);

    System.out.print("\nBooking ID to cancel (0 - abort): ");
    int bookingId = scanner.nextInt();
    scanner.nextLine();

    if (bookingId == 0) {
      System.out.println("Cancelled.");
      return;
    }

    String sql = "UPDATE BOOKING SET status = 'Cancelled' " +
        "WHERE booking_id = ? AND status != 'Cancelled'";

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, bookingId);
      int rows = pstmt.executeUpdate();

      if (rows > 0) {
        System.out.println("\nSUCCESS! Booking cancelled.");
        System.out.println("Room availability restored automatically (trigger).");
      } else {
        System.out.println("Booking not found or already cancelled.");
      }
    }
  }

  /**
   * DELETE - Delete guest
   * Shows all guests with IDs before deletion
   * Cascades to bookings and ratings
   */
  private static void deleteGuest(Connection conn) throws SQLException {
    System.out.println("\n=== GUEST DELETION ===");

    showAllGuests(conn);

    System.out.print("\nGuest ID to delete (0 - abort): ");
    int guestId = scanner.nextInt();
    scanner.nextLine();

    if (guestId == 0) {
      System.out.println("Cancelled.");
      return;
    }

    System.out.print("Confirm deletion? (yes/no): ");
    String confirm = scanner.nextLine();

    if (!confirm.equalsIgnoreCase("yes")) {
      System.out.println("Cancelled.");
      return;
    }

    String sql = "DELETE FROM GUEST WHERE guest_id = ?";

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, guestId);
      int rows = pstmt.executeUpdate();

      if (rows > 0) {
        System.out.println("\nSUCCESS! Guest deleted.");
        System.out.println("Related bookings and ratings also deleted (CASCADE).");
      } else {
        System.out.println("Guest not found.");
      }
    }
  }

  /**
   * INSERT - Add room rating
   * Shows existing room IDs before insertion
   */
  private static void addRoomRating(Connection conn) throws SQLException {
    System.out.println("\n=== ADD ROOM RATING ===");

    showAllGuests(conn);
    System.out.print("\nGuest ID: ");
    int guestId = scanner.nextInt();
    scanner.nextLine();

    showAllRooms(conn);
    System.out.print("\nRoom ID: ");
    int roomId = scanner.nextInt();
    scanner.nextLine();

    System.out.print("Rating (1-5): ");
    int rating = scanner.nextInt();
    scanner.nextLine();

    System.out.print("Review (optional): ");
    String review = scanner.nextLine();

    String sql = "INSERT INTO RATES (guest_id, room_id, rating, review) VALUES (?, ?, ?, ?)";

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, guestId);
      pstmt.setInt(2, roomId);
      pstmt.setInt(3, rating);
      pstmt.setString(4, review.isEmpty() ? null : review);

      pstmt.executeUpdate();
      System.out.println("\nSUCCESS! Rating added.");
    } catch (SQLException e) {
      if ("23505".equals(e.getSQLState())) {
        System.out.println("This guest has already rated this room!");
      } else {
        throw e;
      }
    }
  }

  // Helper methods

  private static void showAllGuests(Connection conn) throws SQLException {
    String sql = "SELECT guest_id, first_name, last_name, email FROM GUEST ORDER BY guest_id";
    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      System.out.println("-".repeat(80));
      System.out.printf("%-5s %-20s %-20s %-30s%n", "ID", "First Name", "Last Name", "Email");
      System.out.println("-".repeat(80));

      while (rs.next()) {
        System.out.printf("%-5d %-20s %-20s %-30s%n",
            rs.getInt("guest_id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"));
      }
      System.out.println("-".repeat(80));
    }
  }

  private static void showAllRooms(Connection conn) throws SQLException {
    String sql = "SELECT room_id, room_number, price_per_night, availability FROM ROOM ORDER BY room_id";
    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      System.out.println("-".repeat(70));
      System.out.printf("%-5s %-12s %15s %15s%n", "ID", "Room #", "Price/Night", "Available");
      System.out.println("-".repeat(70));

      while (rs.next()) {
        System.out.printf("%-5d %-12s %15.2f %15d%n",
            rs.getInt("room_id"),
            rs.getString("room_number"),
            rs.getDouble("price_per_night"),
            rs.getInt("availability"));
      }
      System.out.println("-".repeat(70));
    }
  }

  private static void showAvailableRooms(Connection conn) throws SQLException {
    String sql = "SELECT room_id, room_number, price_per_night, availability " +
        "FROM ROOM WHERE availability > 0 ORDER BY room_id";
    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      System.out.println("-".repeat(70));
      System.out.printf("%-5s %-12s %15s %15s%n", "ID", "Room #", "Price/Night", "Available");
      System.out.println("-".repeat(70));

      while (rs.next()) {
        System.out.printf("%-5d %-12s %15.2f %15d%n",
            rs.getInt("room_id"),
            rs.getString("room_number"),
            rs.getDouble("price_per_night"),
            rs.getInt("availability"));
      }
      System.out.println("-".repeat(70));
    }
  }

  private static void showActiveBookings(Connection conn) throws SQLException {
    String sql = "SELECT b.booking_id, b.booking_date, b.status, b.total_price, " +
        "g.first_name, g.last_name " +
        "FROM BOOKING b " +
        "JOIN GUEST g ON b.guest_id = g.guest_id " +
        "WHERE b.status NOT IN ('Cancelled', 'CheckedOut') " +
        "ORDER BY b.booking_id DESC";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      System.out.println("-".repeat(90));
      System.out.printf("%-5s %-25s %-25s %-15s %12s%n",
          "ID", "Date", "Guest", "Status", "Total");
      System.out.println("-".repeat(90));

      while (rs.next()) {
        System.out.printf("%-5d %-25s %-25s %-15s %12.2f%n",
            rs.getInt("booking_id"),
            rs.getTimestamp("booking_date"),
            rs.getString("first_name") + " " + rs.getString("last_name"),
            rs.getString("status"),
            rs.getDouble("total_price"));
      }
      System.out.println("-".repeat(90));
    }
  }

  private static void viewSystemData(Connection conn) throws SQLException {
    boolean back = false;

    while (!back) {
      System.out.println("\n=== SYSTEM DATA VIEW ===");
      System.out.println("1. All guests");
      System.out.println("2. All rooms");
      System.out.println("3. All bookings");
      System.out.println("4. All ratings");
      System.out.println("5. Guest statistics (view)");
      System.out.println("6. Room statistics (view)");
      System.out.println("0. Back");
      System.out.print("Choice: ");

      int choice = scanner.nextInt();
      scanner.nextLine();

      switch (choice) {
        case 1 -> showAllGuests(conn);
        case 2 -> showAllRooms(conn);
        case 3 -> showAllBookings(conn);
        case 4 -> showAllRatings(conn);
        case 5 -> showGuestStatistics(conn);
        case 6 -> showRoomStatistics(conn);
        case 0 -> back = true;
        default -> System.out.println("Invalid choice!");
      }

      if (!back) {
        System.out.print("\nPress ENTER...");
        scanner.nextLine();
      }
    }
  }

  private static void showAllBookings(Connection conn) throws SQLException {
    String sql = "SELECT b.booking_id, b.booking_date, b.status, b.total_price, " +
        "g.first_name, g.last_name " +
        "FROM BOOKING b " +
        "JOIN GUEST g ON b.guest_id = g.guest_id " +
        "ORDER BY b.booking_date DESC";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      System.out.println("-".repeat(90));
      System.out.printf("%-5s %-25s %-25s %-15s %12s%n",
          "ID", "Date", "Guest", "Status", "Total");
      System.out.println("-".repeat(90));

      while (rs.next()) {
        System.out.printf("%-5d %-25s %-25s %-15s %12.2f%n",
            rs.getInt("booking_id"),
            rs.getTimestamp("booking_date"),
            rs.getString("first_name") + " " + rs.getString("last_name"),
            rs.getString("status"),
            rs.getDouble("total_price"));
      }
      System.out.println("-".repeat(90));
    }
  }

  private static void showAllRatings(Connection conn) throws SQLException {
    String sql = "SELECT g.first_name, g.last_name, r.room_number, " +
        "rt.rating, rt.review " +
        "FROM RATES rt " +
        "JOIN GUEST g ON rt.guest_id = g.guest_id " +
        "JOIN ROOM r ON rt.room_id = r.room_id " +
        "ORDER BY rt.rating DESC";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      System.out.println("-".repeat(100));
      System.out.printf("%-25s %-12s %-8s %-50s%n", "Guest", "Room #", "Rating", "Review");
      System.out.println("-".repeat(100));

      while (rs.next()) {
        String review = rs.getString("review");
        if (review != null && review.length() > 50) {
          review = review.substring(0, 47) + "...";
        }

        System.out.printf("%-25s %-12s %-8d %-50s%n",
            rs.getString("first_name") + " " + rs.getString("last_name"),
            rs.getString("room_number"),
            rs.getInt("rating"),
            review == null ? "" : review);
      }
      System.out.println("-".repeat(100));
    }
  }

  private static void showGuestStatistics(Connection conn) throws SQLException {
    String sql = "SELECT * FROM guest_booking_statistics ORDER BY total_spent DESC";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      System.out.println("-".repeat(100));
      System.out.printf("%-5s %-20s %-20s %10s %12s %10s%n",
          "ID", "First Name", "Last Name", "Bookings", "Total Spent", "Reviews");
      System.out.println("-".repeat(100));

      while (rs.next()) {
        System.out.printf("%-5d %-20s %-20s %10d %12.2f %10d%n",
            rs.getInt("guest_id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getInt("total_bookings"),
            rs.getDouble("total_spent"),
            rs.getInt("reviews_count"));
      }
      System.out.println("-".repeat(100));
    }
  }

  private static void showRoomStatistics(Connection conn) throws SQLException {
    String sql = "SELECT * FROM room_statistics ORDER BY revenue DESC";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      System.out.println("-".repeat(100));
      System.out.printf("%-5s %-12s %12s %12s %12s %12s %10s%n",
          "ID", "Room #", "Times Booked", "Total Nights", "Revenue", "Avg Rating", "Reviews");
      System.out.println("-".repeat(100));

      while (rs.next()) {
        System.out.printf("%-5d %-12s %12d %12d %12.2f %12.1f %10d%n",
            rs.getInt("room_id"),
            rs.getString("room_number"),
            rs.getInt("times_booked"),
            rs.getInt("total_nights_booked"),
            rs.getDouble("revenue"),
            rs.getDouble("average_rating"),
            rs.getInt("ratings_count"));
      }
      System.out.println("-".repeat(100));
    }
  }
}