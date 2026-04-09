import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ResQDatabaseController {

    // ==========================================================
    // GROUP 1: USER & ZONE OPERATIONS
    // ==========================================================

    public static boolean registerNewCustomer(String name, String email, String phone, double lat, double lon, int polygonId) {
        String sql = "INSERT INTO USER (Name, Email, Phone, Latitude, Longitude, Polygon_ID) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name); stmt.setString(2, email); stmt.setString(3, phone);
            stmt.setDouble(4, lat); stmt.setDouble(5, lon); stmt.setInt(6, polygonId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); return false; }
    }

    public static void fetchCustomerDetails(int userId) {
        String sql = "SELECT Name, Email, Phone FROM USER WHERE UserID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) System.out.println("Customer: " + rs.getString("Name") + " | " + rs.getString("Phone"));
            }
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    public static boolean updateCustomerContact(int userId, String phone, String email) {
        String sql = "UPDATE USER SET Phone = ?, Email = ? WHERE UserID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone); stmt.setString(2, email); stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); return false; }
    }

    public static void viewCoverageZones() {
        String sql = "SELECT PolygonID, ZoneName FROM POLYGON";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) System.out.println("Zone " + rs.getInt("PolygonID") + ": " + rs.getString("ZoneName"));
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    // ==========================================================
    // GROUP 2: THE WORKFORCE (HR & Logistics)
    // ==========================================================

    public static boolean onboardNewTechnician(String name, String phone, double salary, int hubId) {
        String sql = "INSERT INTO WORKER (Name, Contact_Number, Salary, HubID) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name); stmt.setString(2, phone); stmt.setDouble(3, salary); stmt.setInt(4, hubId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); return false; }
    }

    public static boolean verifyWorkerBackground(int workerId) {
        String sql = "UPDATE WORKER SET VerificationStatus = 'Verified' WHERE WorkerID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, workerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); return false; }
    }

    public static boolean registerWorkerVehicle(int workerId, String reg, String model, boolean cleaningDuty) {
        String sql = "INSERT INTO CONTRACT (WorkerID, VehicleReg, VehicleModel, CleaningDutyClause) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, workerId); stmt.setString(2, reg); stmt.setString(3, model); stmt.setBoolean(4, cleaningDuty);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); return false; }
    }

    public static void fetchWorkerFleetDetails(int hubId) {
        String sql = "SELECT w.Name, c.VehicleReg, c.VehicleModel FROM WORKER w JOIN CONTRACT c ON w.WorkerID = c.WorkerID WHERE w.HubID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, hubId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) System.out.println(rs.getString("Name") + " rides a " + rs.getString("VehicleModel") + " (" + rs.getString("VehicleReg") + ")");
            }
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    public static void viewAvailableTechnicians() {
        String sql = "SELECT WorkerID, Name, Contact_Number FROM WORKER WHERE AvailabilityStatus = 'Standby'";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) System.out.println("ID " + rs.getInt("WorkerID") + ": " + rs.getString("Name") + " (Standby)");
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    // ==========================================================
    // GROUP 3: WAREHOUSE & INVENTORY
    // ==========================================================

    public static void viewAllMicroHubs() {
        String sql = "SELECT HubID, LocationAddress FROM MICRO_HUB";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) System.out.println("Hub " + rs.getInt("HubID") + ": " + rs.getString("LocationAddress"));
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    public static void browsePartsCatalog(String category) {
        String sql = "SELECT PartID, Name, Price FROM PART WHERE Category = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) System.out.println(rs.getString("Name") + " | ₹" + rs.getDouble("Price"));
            }
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    public static void checkHubStockLevel(int hubId) {
        String sql = "SELECT p.Name, h.Quantity FROM HUB_INVENTORY h JOIN PART p ON h.PartID = p.PartID WHERE h.HubID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, hubId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) System.out.println(rs.getString("Name") + " | Qty: " + rs.getInt("Quantity"));
            }
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    public static boolean deductInventoryPostJob(int hubId, int partId, int quantityUsed) {
        String sql = "UPDATE HUB_INVENTORY SET Quantity = Quantity - ? WHERE HubID = ? AND PartID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantityUsed); stmt.setInt(2, hubId); stmt.setInt(3, partId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); return false; }
    }

    // ==========================================================
    // GROUP 4: THE CORE ENGINE (Emergencies & Dispatch)
    // ==========================================================

    public static boolean logEmergencyRequest(int userId, String issueType) {
        String sql = "INSERT INTO EMERGENCY_REQUEST (UserID, IssueType, Status) VALUES (?, ?, 'Pending')";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId); stmt.setString(2, issueType);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); return false; }
    }

    public static void viewPendingEmergencies() {
        String sql = "SELECT RequestID, UserID, IssueType, Timestamp FROM EMERGENCY_REQUEST WHERE Status = 'Pending'";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) System.out.println("Req #" + rs.getInt("RequestID") + " | User " + rs.getInt("UserID") + " | Issue: " + rs.getString("IssueType"));
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    public static boolean dispatchTechnician(int requestId, int workerId) {
        String sql = "INSERT INTO DISPATCH_ASSIGNMENT (RequestID, WorkerID, Status) VALUES (?, ?, 'EnRoute')";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requestId); stmt.setInt(2, workerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); return false; }
    }

    public static boolean updateDispatchTracking(int assignmentId, String status) {
        String sql = "UPDATE DISPATCH_ASSIGNMENT SET Status = ? WHERE AssignmentID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status); stmt.setInt(2, assignmentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); return false; }
    }

    public static boolean cancelEmergency(int requestId) {
        String sql = "UPDATE EMERGENCY_REQUEST SET Status = 'Cancelled' WHERE RequestID = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requestId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); return false; }
    }

    // ==========================================================
    // GROUP 5: FINANCE & ANALYTICS
    // ==========================================================

    public static boolean processFinalPayment(int requestId, double amount, String transactionStatus) {
        String sql = "INSERT INTO PAYMENT (RequestID, Amount, TransactionStatus) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requestId); stmt.setDouble(2, amount); stmt.setString(3, transactionStatus);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); return false; }
    }

    public static void viewFullSystemStatus() {
        String sql = "SELECT er.RequestID, u.Name AS Customer, er.IssueType, er.Status, w.Name AS Tech, p.TransactionStatus " +
                     "FROM EMERGENCY_REQUEST er LEFT JOIN USER u ON er.UserID = u.UserID " +
                     "LEFT JOIN DISPATCH_ASSIGNMENT da ON er.RequestID = da.RequestID " +
                     "LEFT JOIN WORKER w ON da.WorkerID = w.WorkerID " +
                     "LEFT JOIN PAYMENT p ON er.RequestID = p.RequestID ORDER BY er.Timestamp DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                System.out.printf("Req #%d | %s | %s | Status: %s | Tech: %s | Pay: %s\n",
                        rs.getInt("RequestID"), rs.getString("Customer"), rs.getString("IssueType"),
                        rs.getString("Status"), rs.getString("Tech") != null ? rs.getString("Tech") : "None",
                        rs.getString("TransactionStatus") != null ? rs.getString("TransactionStatus") : "Unpaid");
            }
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    // ---------------------------------------------------------
    // MAIN TEST METHOD
    // ---------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("--- Testing Res-Q System Connection ---\n");
        viewFullSystemStatus();
    }
}