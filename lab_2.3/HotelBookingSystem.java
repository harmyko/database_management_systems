import java.sql.*;
import java.util.Scanner;
import java.io.Console;

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

    System.out.println("+------------------------------------------+");
    System.out.println("|        Database Authentication           |");
    System.out.println("+------------------------------------------+");
    String user = console.readLine("Enter username: ");
    char[] password = console.readPassword("Enter password: ");

    Connection conn = null;
    try {
      conn = DriverManager.getConnection(DB_URL, user, new String(password));
      System.out.println("\n>> Connection established successfully\n");
    } catch (SQLException e) {
      System.err.println("\n!! Connection failed - check credentials");
      System.err.println("!! Error: " + e.getMessage());
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
        System.out.println("!! Invalid input - please enter a number");
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
            System.out.println("!! Option not recognized");
        }

        if (running && choice != 0) {
          System.out.print("\n[Press ENTER to continue]");
          scanner.nextLine();
        }
      } catch (SQLException e) {
        System.err.println("\n!! Database error occurred: " + e.getMessage());
        System.out.print("\n[Press ENTER to continue]");
        scanner.nextLine();
      }
    }
  }

  private static void printMenu() {
    System.out.println("\n+------------------------------------------+");
    System.out.println("|     Hotel Reservation System v1.0        |");
    System.out.println("+------------------------------------------+");
    System.out.println("| [1] Find available rooms                 |");
    System.out.println("| [2] Add new guest                        |");
    System.out.println("| [3] Make reservation (TRANSACTION)       |");
    System.out.println("| [4] Cancel reservation                   |");
    System.out.println("| [5] Remove guest                         |");
    System.out.println("| [6] Submit room review                   |");
    System.out.println("| [7] Browse database records              |");
    System.out.println("| [0] Quit program                         |");
    System.out.println("+------------------------------------------+");
    System.out.print(">> Select option: ");
  }

  private static void searchRooms(Connection conn) throws SQLException {
    System.out.println("\n+--- Room Search Module ---+");
    System.out.println("  [1] Search by room number");
    System.out.println("  [2] Search by price range");
    System.out.print(">> ");

    int searchType = scanner.nextInt();
    scanner.nextLine();

    if (searchType == 1) {
      System.out.print("\nRoom number (full or partial): ");
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
      System.out.print("\nMin price (EUR): ");
      double minPrice = scanner.nextDouble();
      System.out.print("Max price (EUR): ");
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
    System.out.println("\n" + "=".repeat(100));
    System.out.printf("| %-3s | %-10s | %10s | %10s | %6s | %6s | %-28s |%n",
        "ID", "Room No.", "Price/Ngt", "Available", "Rates", "AvgR", "Description");
    System.out.println("=".repeat(100));

    while (rs.next()) {
      found = true;
      String description = rs.getString("description");
      if (description != null && description.length() > 28) {
        description = description.substring(0, 25) + "...";
      }

      System.out.printf("| %-3d | %-10s | %10.2f | %10d | %6d | %6.1f | %-28s |%n",
          rs.getInt("room_id"),
          rs.getString("room_number"),
          rs.getDouble("price_per_night"),
          rs.getInt("availability"),
          rs.getInt("rating_count"),
          rs.getDouble("avg_rating"),
          description);
    }

    System.out.println("=".repeat(100));
    if (!found) {
      System.out.println(">> No matching rooms found");
    }
  }

  private static void registerGuest(Connection conn) throws SQLException {
    System.out.println("\n+--- Guest Registration Form ---+");

    showAllGuests(conn);

    System.out.print("\nFirst name: ");
    String firstName = scanner.nextLine();

    System.out.print("Last name: ");
    String lastName = scanner.nextLine();

    System.out.print("Email address: ");
    String email = scanner.nextLine();

    if (!email.matches(".+@.+\\..+")) {
      System.out.println("!! Email format is invalid");
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
        System.out.println("\n>> Guest registered successfully! ID: " + keys.getInt(1));
      }
      keys.close();
    } catch (SQLException e) {
      if ("23505".equals(e.getSQLState())) {
        System.out.println("!! Email is already registered in the system");
      } else {
        throw e;
      }
    }
  }

  private static void createBookingWithItems(Connection conn) throws SQLException {
    System.out.println("\n+------------------------------------------+");
    System.out.println("|   New Reservation (TRANSACTION MODE)     |");
    System.out.println("+------------------------------------------+");

    showAllGuests(conn);

    System.out.print("\nGuest ID: ");
    int guestId = scanner.nextInt();
    scanner.nextLine();

    String checkGuest = "SELECT guest_id FROM GUEST WHERE guest_id = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(checkGuest)) {
      pstmt.setInt(1, guestId);
      ResultSet rs = pstmt.executeQuery();
      if (!rs.next()) {
        System.out.println("!! Guest ID not found in database");
        rs.close();
        return;
      }
      rs.close();
    }

    System.out.println("\n-- Delivery Address Information --");
    System.out.print("Country: ");
    String country = scanner.nextLine();
    System.out.print("City: ");
    String city = scanner.nextLine();
    System.out.print("Postal code: ");
    String postalCode = scanner.nextLine();
    System.out.print("Street address: ");
    String addressLine = scanner.nextLine();

    boolean autoCommit = conn.getAutoCommit();
    conn.setAutoCommit(false);

    try {
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
          throw new SQLException("Booking creation failed");
        }
        bookingId = rs.getInt(1);
        rs.close();
      }

      System.out.println("\n>> Reservation created with ID: " + bookingId);

      boolean addingItems = true;
      int itemNumber = 1;

      while (addingItems) {
        System.out.println("\n-- Adding Room to Reservation --");
        showAvailableRooms(conn);

        System.out.print("\nRoom ID (enter 0 to finish): ");
        int roomId = scanner.nextInt();
        scanner.nextLine();

        if (roomId == 0) {
          if (itemNumber == 1) {
            throw new SQLException("Reservation must contain at least one room");
          }
          addingItems = false;
          continue;
        }

        String getRoom = "SELECT room_number, price_per_night, availability FROM ROOM WHERE room_id = ?";
        double price;
        int maxAvailability;
        String roomNumber;

        try (PreparedStatement pstmt = conn.prepareStatement(getRoom)) {
          pstmt.setInt(1, roomId);
          ResultSet rs = pstmt.executeQuery();

          if (!rs.next()) {
            System.out.println("!! Room not found");
            rs.close();
            continue;
          }

          roomNumber = rs.getString("room_number");
          price = rs.getDouble("price_per_night");
          maxAvailability = rs.getInt("availability");
          rs.close();
        }

        System.out.println("Selected room: " + roomNumber);
        System.out.println("Rate per night: " + price + " EUR");
        System.out.println("Available units: " + maxAvailability);

        System.out.print("Number of nights: ");
        int nights = scanner.nextInt();
        scanner.nextLine();

        if (nights <= 0 || nights > maxAvailability) {
          System.out.println("!! Invalid nights quantity");
          continue;
        }

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

        System.out.println(">> Room added to reservation");
        itemNumber++;
      }

      conn.commit();
      System.out.println("\n+------------------------------------------+");
      System.out.println("|   Reservation completed successfully!    |");
      System.out.println("+------------------------------------------+");
      System.out.println(">> Total price calculated by trigger");

    } catch (SQLException e) {
      conn.rollback();
      System.out.println("\n!! Reservation failed - transaction rolled back");
      throw e;
    } finally {
      conn.setAutoCommit(autoCommit);
    }
  }

  private static void cancelBooking(Connection conn) throws SQLException {
    System.out.println("\n+--- Reservation Cancellation ---+");

    showActiveBookings(conn);

    System.out.print("\nReservation ID (0 to abort): ");
    int bookingId = scanner.nextInt();
    scanner.nextLine();

    if (bookingId == 0) {
      System.out.println(">> Operation aborted");
      return;
    }

    String sql = "UPDATE BOOKING SET status = 'Cancelled' " +
        "WHERE booking_id = ? AND status != 'Cancelled'";

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, bookingId);
      int rows = pstmt.executeUpdate();

      if (rows > 0) {
        System.out.println("\n>> Reservation cancelled successfully");
        System.out.println(">> Room availability restored (via trigger)");
      } else {
        System.out.println("!! Reservation not found or already cancelled");
      }
    }
  }

  private static void deleteGuest(Connection conn) throws SQLException {
    System.out.println("\n+--- Guest Removal ---+");

    showAllGuests(conn);

    System.out.print("\nGuest ID to remove (0 to abort): ");
    int guestId = scanner.nextInt();
    scanner.nextLine();

    if (guestId == 0) {
      System.out.println(">> Operation aborted");
      return;
    }

    System.out.print("Confirm deletion? (yes/no): ");
    String confirm = scanner.nextLine();

    if (!confirm.equalsIgnoreCase("yes")) {
      System.out.println(">> Operation aborted");
      return;
    }

    String sql = "DELETE FROM GUEST WHERE guest_id = ?";

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, guestId);
      int rows = pstmt.executeUpdate();

      if (rows > 0) {
        System.out.println("\n>> Guest removed from database");
        System.out.println(">> Associated reservations and reviews also deleted (CASCADE)");
      } else {
        System.out.println("!! Guest not found");
      }
    }
  }

  private static void addRoomRating(Connection conn) throws SQLException {
    System.out.println("\n+--- Submit Room Review ---+");

    showAllGuests(conn);
    System.out.print("\nYour guest ID: ");
    int guestId = scanner.nextInt();
    scanner.nextLine();

    showAllRooms(conn);
    System.out.print("\nRoom ID to review: ");
    int roomId = scanner.nextInt();
    scanner.nextLine();

    System.out.print("Rating (1-5 stars): ");
    int rating = scanner.nextInt();
    scanner.nextLine();

    System.out.print("Written review (optional, press ENTER to skip): ");
    String review = scanner.nextLine();

    String sql = "INSERT INTO RATES (guest_id, room_id, rating, review) VALUES (?, ?, ?, ?)";

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, guestId);
      pstmt.setInt(2, roomId);
      pstmt.setInt(3, rating);
      pstmt.setString(4, review.isEmpty() ? null : review);

      pstmt.executeUpdate();
      System.out.println("\n>> Review submitted successfully");
    } catch (SQLException e) {
      if ("23505".equals(e.getSQLState())) {
        System.out.println("!! You have already reviewed this room");
      } else {
        throw e;
      }
    }
  }

  private static void showAllGuests(Connection conn) throws SQLException {
    String sql = "SELECT guest_id, first_name, last_name, email FROM GUEST ORDER BY guest_id";
    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      System.out.println("~".repeat(85));
      System.out.printf("| %-4s | %-18s | %-18s | %-30s |%n", "ID", "First Name", "Last Name", "Email");
      System.out.println("~".repeat(85));

      while (rs.next()) {
        System.out.printf("| %-4d | %-18s | %-18s | %-30s |%n",
            rs.getInt("guest_id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"));
      }
      System.out.println("~".repeat(85));
    }
  }

  private static void showAllRooms(Connection conn) throws SQLException {
    String sql = "SELECT room_id, room_number, price_per_night, availability FROM ROOM ORDER BY room_id";
    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      System.out.println("~".repeat(68));
      System.out.printf("| %-4s | %-10s | %13s | %13s |%n", "ID", "Room No.", "Price/Night", "Available");
      System.out.println("~".repeat(68));

      while (rs.next()) {
        System.out.printf("| %-4d | %-10s | %13.2f | %13d |%n",
            rs.getInt("room_id"),
            rs.getString("room_number"),
            rs.getDouble("price_per_night"),
            rs.getInt("availability"));
      }
      System.out.println("~".repeat(68));
    }
  }

  private static void showAvailableRooms(Connection conn) throws SQLException {
    String sql = "SELECT room_id, room_number, price_per_night, availability " +
        "FROM ROOM WHERE availability > 0 ORDER BY room_id";
    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      System.out.println("~".repeat(68));
      System.out.printf("| %-4s | %-10s | %13s | %13s |%n", "ID", "Room No.", "Price/Night", "Available");
      System.out.println("~".repeat(68));

      while (rs.next()) {
        System.out.printf("| %-4d | %-10s | %13.2f | %13d |%n",
            rs.getInt("room_id"),
            rs.getString("room_number"),
            rs.getDouble("price_per_night"),
            rs.getInt("availability"));
      }
      System.out.println("~".repeat(68));
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

      System.out.println("~".repeat(93));
      System.out.printf("| %-4s | %-23s | %-23s | %-13s | %10s |%n",
          "ID", "Timestamp", "Guest Name", "Status", "Total");
      System.out.println("~".repeat(93));

      while (rs.next()) {
        System.out.printf("| %-4d | %-23s | %-23s | %-13s | %10.2f |%n",
            rs.getInt("booking_id"),
            rs.getTimestamp("booking_date"),
            rs.getString("first_name") + " " + rs.getString("last_name"),
            rs.getString("status"),
            rs.getDouble("total_price"));
      }
      System.out.println("~".repeat(93));
    }
  }

  private static void viewSystemData(Connection conn) throws SQLException {
    boolean back = false;

    while (!back) {
      System.out.println("\n+--- Database Records Browser ---+");
      System.out.println("  [1] Guest list");
      System.out.println("  [2] Room catalog");
      System.out.println("  [3] All reservations");
      System.out.println("  [4] All reviews");
      System.out.println("  [5] Guest statistics view");
      System.out.println("  [6] Room statistics view");
      System.out.println("  [0] Return to main menu");
      System.out.print(">> ");

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
        default -> System.out.println("!! Invalid selection");
      }

      if (!back) {
        System.out.print("\n[Press ENTER]");
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

      System.out.println("~".repeat(93));
      System.out.printf("| %-4s | %-23s | %-23s | %-13s | %10s |%n",
          "ID", "Timestamp", "Guest Name", "Status", "Total");
      System.out.println("~".repeat(93));

      while (rs.next()) {
        System.out.printf("| %-4d | %-23s | %-23s | %-13s | %10.2f |%n",
            rs.getInt("booking_id"),
            rs.getTimestamp("booking_date"),
            rs.getString("first_name") + " " + rs.getString("last_name"),
            rs.getString("status"),
            rs.getDouble("total_price"));
      }
      System.out.println("~".repeat(93));
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

      System.out.println("~".repeat(103));
      System.out.printf("| %-23s | %-10s | %-6s | %-48s |%n", "Guest Name", "Room No.", "Stars", "Review Text");
      System.out.println("~".repeat(103));

      while (rs.next()) {
        String review = rs.getString("review");
        if (review != null && review.length() > 48) {
          review = review.substring(0, 45) + "...";
        }

        System.out.printf("| %-23s | %-10s | %-6d | %-48s |%n",
            rs.getString("first_name") + " " + rs.getString("last_name"),
            rs.getString("room_number"),
            rs.getInt("rating"),
            review == null ? "" : review);
      }
      System.out.println("~".repeat(103));
    }
  }

  private static void showGuestStatistics(Connection conn) throws SQLException {
    String sql = "SELECT * FROM guest_booking_statistics ORDER BY total_spent DESC";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      System.out.println("~".repeat(98));
      System.out.printf("| %-4s | %-18s | %-18s | %9s | %11s | %8s |%n",
          "ID", "First Name", "Last Name", "Bookings", "Total Spent", "Reviews");
      System.out.println("~".repeat(98));

      while (rs.next()) {
        System.out.printf("| %-4d | %-18s | %-18s | %9d | %11.2f | %8d |%n",
            rs.getInt("guest_id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getInt("total_bookings"),
            rs.getDouble("total_spent"),
            rs.getInt("reviews_count"));
      }
      System.out.println("~".repeat(98));
    }
  }

  private static void showRoomStatistics(Connection conn) throws SQLException {
    String sql = "SELECT * FROM room_statistics ORDER BY revenue DESC";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      System.out.println("~".repeat(102));
      System.out.printf("| %-4s | %-10s | %11s | %11s | %11s | %10s | %8s |%n",
          "ID", "Room No.", "Times Used", "Total Ngt", "Revenue", "Avg Rating", "Reviews");
      System.out.println("~".repeat(102));

      while (rs.next()) {
        System.out.printf("| %-4d | %-10s | %11d | %11d | %11.2f | %10.1f | %8d |%n",
            rs.getInt("room_id"),
            rs.getString("room_number"),
            rs.getInt("times_booked"),
            rs.getInt("total_nights_booked"),
            rs.getDouble("revenue"),
            rs.getDouble("average_rating"),
            rs.getInt("ratings_count"));
      }
      System.out.println("~".repeat(102));
    }
  }
}