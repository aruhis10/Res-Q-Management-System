package com.example.demo; 

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional; 
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class ResQController {

    @Autowired
    private JdbcTemplate db;

    // ==========================================
    // 1. CUSTOMER PORTAL ENDPOINTS
    // ==========================================

    @GetMapping("/user/login")
    public Map<String, Object> userLogin(@RequestParam int userId, @RequestParam String password) {
        String sql = "SELECT * FROM USER WHERE UserID = ? AND PasswordHash = SHA2(?, 256)";
        List<Map<String, Object>> result = db.queryForList(sql, userId, password);
        return result.isEmpty() ? Map.of("error", "Invalid User ID or Password") : result.get(0);
    }

    @PostMapping("/user/register")
    public Map<String, Object> registerUser(
            @RequestParam String name, 
            @RequestParam String email,
            @RequestParam String phone, 
            @RequestParam String address, 
            @RequestParam String landmark, 
            @RequestParam String zone,
            @RequestParam String password) {

        // FIX: Correctly mapping UI zones to the 3 seeded DB Polygons
        int polygonId = 1; // Default North Delhi
        if (zone.equalsIgnoreCase("South Delhi")) polygonId = 2;
        else if (zone.equalsIgnoreCase("West Delhi") || zone.equalsIgnoreCase("East Delhi")) polygonId = 3;

        try {
            String sql = "CALL sp_RegisterUser(?, ?, ?, ?, ?, ?, ?, ?)";
            db.update(sql, name, email, phone, address, landmark, zone, polygonId, password);
            Integer newUserId = db.queryForObject("SELECT UserID FROM USER WHERE Email = ?", Integer.class, email);
            return Map.of("success", true, "userId", newUserId, "message", "Registration successful!");
        } catch (Exception e) {
            return Map.of("error", true, "message", e.getMessage());
        }
    }

    @GetMapping("/user/jobs")
    public List<Map<String, Object>> getActiveJobs(@RequestParam int userId) {
        String sql = "SELECT r.RequestID, r.IssueType, r.Status, r.Category, r.SpecificLocation, d.Status AS DispatchStatus, p.Amount, " +
                     "w.Name AS WorkerName, w.Contact_Number AS WorkerPhone, mh.LocationAddress AS HubLocation " +
                     "FROM EMERGENCY_REQUEST r " +
                     "LEFT JOIN DISPATCH_ASSIGNMENT d ON r.RequestID = d.RequestID " +
                     "LEFT JOIN WORKER w ON d.WorkerID = w.WorkerID " +
                     "LEFT JOIN MICRO_HUB mh ON w.HubID = mh.HubID " +
                     "LEFT JOIN PAYMENT p ON r.RequestID = p.RequestID " +
                     "WHERE r.UserID = ? AND r.Status NOT IN ('Completed', 'Cancelled')";
        return db.queryForList(sql, userId);
    }

    @PostMapping("/user/emergency")
    public Map<String, String> logEmergency(@RequestParam int userId, @RequestParam String issue, @RequestParam String category, @RequestParam String location) {
        String sql = "CALL sp_AutoDispatch(?, ?, ?, ?)";
        db.update(sql, userId, issue, category, location);
        return Map.of("message", "Emergency Dispatched!");
    }

    // FIX: Using the new Stored Procedure to bypass Trigger limits and safely free the worker
    @PostMapping("/user/pay")
    public Map<String, String> processPayment(@RequestParam int requestId, @RequestParam double amount) {
        try {
            db.update("CALL sp_ProcessPayment(?, ?)", requestId, amount);
            return Map.of("message", "Payment Successful!");
        } catch (Exception e) {
            return Map.of("error", "Amount mismatch or invalid request.");
        }
    }

    @PostMapping("/user/cancel")
    public Map<String, Object> cancelEmergency(@RequestParam int requestId, @RequestParam int userId) {
        try {
            String sql = "CALL sp_CancelRequest(?, ?)";
            db.update(sql, requestId, userId);
            return Map.of("success", true, "message", "Emergency Request Cancelled.");
        } catch (Exception e) {
            return Map.of("error", true, "message", "Failed to cancel request. It may already be in progress.");
        }
    }

    @GetMapping("/user/history")
    public List<Map<String, Object>> getUserHistory(@RequestParam int userId) {
        String sql = "SELECT r.RequestID, r.IssueType, r.Category, r.Timestamp, r.Status, p.Amount, w.Name AS WorkerName " +
                     "FROM EMERGENCY_REQUEST r " +
                     "LEFT JOIN PAYMENT p ON r.RequestID = p.RequestID " +
                     "LEFT JOIN DISPATCH_ASSIGNMENT d ON r.RequestID = d.RequestID " +
                     "LEFT JOIN WORKER w ON d.WorkerID = w.WorkerID " +
                     "WHERE r.UserID = ? AND r.Status IN ('Completed', 'Cancelled') " +
                     "ORDER BY r.Timestamp DESC";
        return db.queryForList(sql, userId);
    }

    @PostMapping("/user/update")
    public Map<String, Object> updateUserProfile(
            @RequestParam int userId, @RequestParam String phone,
            @RequestParam String address, @RequestParam String landmark, @RequestParam String zone) {

        try {
            String checkSql = "SELECT COUNT(*) FROM EMERGENCY_REQUEST WHERE UserID = ? AND Status NOT IN ('Completed', 'Cancelled')";
            Integer activeCount = db.queryForObject(checkSql, Integer.class, userId);

            if (activeCount != null && activeCount > 0) {
                return Map.of("error", true, "message", "Cannot update details while an emergency request or payment is active.");
            }

            int polygonId = 1; 
            if (zone.equalsIgnoreCase("South Delhi")) polygonId = 2;
            else if (zone.equalsIgnoreCase("West Delhi") || zone.equalsIgnoreCase("East Delhi")) polygonId = 3;

            String updateSql = "UPDATE USER SET Phone = ?, Address = ?, Landmark = ?, Zone = ?, Polygon_ID = ? WHERE UserID = ?";
            db.update(updateSql, phone, address, landmark, zone, polygonId, userId);
            return Map.of("success", true, "message", "Profile updated successfully!");
        } catch (Exception e) {
            return Map.of("error", true, "message", "Database error during profile update.");
        }
    }

    @GetMapping("/user/eligible-complaints")
    public List<Map<String, Object>> getEligibleComplaints(@RequestParam int userId) {
        String sql = "SELECT r.RequestID, r.IssueType, r.Category, r.Timestamp, w.Name AS WorkerName " +
                     "FROM EMERGENCY_REQUEST r " +
                     "LEFT JOIN DISPATCH_ASSIGNMENT d ON r.RequestID = d.RequestID " +
                     "LEFT JOIN WORKER w ON d.WorkerID = w.WorkerID " +
                     "WHERE r.UserID = ? AND r.Status = 'Completed' " +
                     "AND r.Timestamp >= NOW() - INTERVAL 7 DAY " +
                     "AND r.RequestID NOT IN (SELECT RequestID FROM COMPLAINT WHERE Role = 'Customer') " +
                     "ORDER BY r.Timestamp DESC";
        return db.queryForList(sql, userId);
    }

    @PostMapping("/user/complaint")
    public Map<String, Object> submitComplaint(
            @RequestParam int userId, @RequestParam int requestId, 
            @RequestParam String description, @RequestParam String medium) {
        try {
            String sql = "CALL sp_FileCustomerComplaint(?, ?, ?, ?)";
            db.update(sql, userId, requestId, description, medium);
            return Map.of("success", true, "message", "Complaint routed to your local Support Helper.");
        } catch (Exception e) {
            return Map.of("error", true, "message", "Failed to file complaint: " + e.getMessage());
        }
    }

    @GetMapping("/user/my-complaints")
    public List<Map<String, Object>> getUserComplaints(@RequestParam int userId) {
        String sql = "SELECT c.ComplaintID, c.RequestID, c.ComplaintText, c.Status, c.ProofMedium, c.CreatedAt, " +
                     "h.Name AS HelperName, h.Phone AS HelperPhone, h.Email AS HelperEmail " +
                     "FROM COMPLAINT c " +
                     "LEFT JOIN HELPER h ON c.HelperID = h.HelperID " +
                     "WHERE c.Role = 'Customer' AND c.UserID_or_WorkerID = ? " +
                     "ORDER BY c.CreatedAt DESC";
        return db.queryForList(sql, userId);
    }

    // ==========================================
    // 2. WORKER PORTAL ENDPOINTS
    // ==========================================

    @GetMapping("/worker/login")
    public Map<String, Object> workerLogin(@RequestParam int workerId, @RequestParam String password) {
        String sql = "SELECT * FROM WORKER WHERE WorkerID = ? AND PasswordHash = SHA2(?, 256)";
        List<Map<String, Object>> result = db.queryForList(sql, workerId, password);
        return result.isEmpty() ? Map.of("error", "Invalid Worker ID or Password") : result.get(0);
    }

    @GetMapping("/worker/job")
    public List<Map<String, Object>> getWorkerJob(@RequestParam int workerId) {
        String sql = "SELECT d.AssignmentID, r.RequestID, r.IssueType, r.Category, r.SpecificLocation, r.IsWarrantyRework, " +
                     "d.Status, u.Name AS CustomerName, u.Phone AS CustomerPhone, p.Amount " +
                     "FROM DISPATCH_ASSIGNMENT d " +
                     "JOIN EMERGENCY_REQUEST r ON d.RequestID = r.RequestID " +
                     "JOIN USER u ON r.UserID = u.UserID " +
                     "LEFT JOIN PAYMENT p ON r.RequestID = p.RequestID " +
                     "WHERE d.WorkerID = ? " +
                     "AND d.Status IN ('Assigned', 'EnRoute', 'Arrived', 'JobDone') " +
                     "AND r.Status != 'Completed'"; 
        return db.queryForList(sql, workerId);
    }
    
    @GetMapping("/worker/history")
    public List<Map<String, Object>> getWorkerHistory(@RequestParam int workerId) {
        String sql = "SELECT r.RequestID, r.Category, r.IssueType, p.Amount " +
                     "FROM DISPATCH_ASSIGNMENT d " +
                     "JOIN EMERGENCY_REQUEST r ON d.RequestID = r.RequestID " +
                     "LEFT JOIN PAYMENT p ON r.RequestID = p.RequestID " +
                     "WHERE d.WorkerID = ? AND d.Status IN ('JobDone', 'Completed')";
        return db.queryForList(sql, workerId);
    }

    // FIX: Connects to the new safe procedure for status changes
    @PostMapping("/worker/status")
    public Map<String, Object> updateWorkerStatus(@RequestParam int workerId, @RequestParam String status) {
        try {
            db.update("CALL sp_UpdateWorkerStatus(?, ?)", workerId, status);
            return Map.of("success", true, "message", "Status updated to " + status);
        } catch (Exception e) {
            return Map.of("error", true, "message", "Cannot change status: You are currently assigned to an active job.");
        }
    }

    // FIX: Using the procedure to safely keep Emergency Request status in sync
    @PostMapping("/worker/dispatch-status")
    public Map<String, String> updateDispatchStatus(@RequestParam int requestId, @RequestParam String status) {
        try {
            db.update("CALL sp_UpdateDispatchStatus(?, ?)", requestId, status);
            return Map.of("success", "true", "message", "Status updated to " + status);
        } catch (Exception e) {
            return Map.of("error", "true", "message", "Failed to update status.");
        }
    }

    @PostMapping("/worker/cancel")
    public Map<String, Object> workerCancelJob(@RequestParam int workerId, @RequestParam int requestId, @RequestParam String reason) {
        try {
            String sql = "CALL sp_WorkerCancelRequest(?, ?, ?)";
            db.update(sql, requestId, workerId, reason);
            return Map.of("success", true, "message", "Cancellation processed successfully.");
        } catch (Exception e) {
            return Map.of("error", true, "message", "Database rejected cancellation.");
        }
    }

    @PostMapping("/worker/restock-request")
    public Map<String, Object> requestRestock(@RequestParam int workerId, @RequestParam int partId) {
        try {
            String sql = "CALL sp_RequestRestock(?, ?)";
            db.update(sql, workerId, partId);
            return Map.of("success", true, "message", "Restock request sent to Admin.");
        } catch (Exception e) {
            return Map.of("error", true, "message", "Request failed: " + e.getMessage());
        }
    }

    // ==========================================
    // 3. ERP (INVENTORY & BILLING) ENDPOINTS
    // ==========================================

    @GetMapping("/worker/inventory")
    public List<Map<String, Object>> getWorkerHubInventory(@RequestParam int workerId) {
        String sql = "SELECT i.PartID, p.Name, p.Category, p.Price, i.Quantity " +
                     "FROM HUB_INVENTORY i " +
                     "JOIN PART p ON i.PartID = p.PartID " +
                     "JOIN WORKER w ON w.HubID = i.HubID " +
                     "WHERE w.WorkerID = ?";
        return db.queryForList(sql, workerId);
    }

    @PostMapping("/worker/use-part")
    public Map<String, Object> usePart(@RequestParam int workerId, @RequestParam int requestId, 
                                       @RequestParam int partId, @RequestParam int quantity) {
        try {
            String sql = "CALL sp_UsePart(?, ?, ?, ?)";
            db.update(sql, workerId, requestId, partId, quantity);
            return Map.of("success", true, "message", "Part logged to receipt!");
        } catch (Exception e) {
            return Map.of("error", true, "message", "Failed: Insufficient Hub Stock.");
        }
    }

    @PostMapping("/worker/generate-bill")
    public Map<String, Object> generateBill(@RequestParam int requestId) {
        try {
            String sql = "CALL sp_GenerateBill(?)";
            db.update(sql, requestId);
            return Map.of("success", true, "message", "Invoice generated successfully! Awaiting Customer Payment.");
        } catch (Exception e) {
            return Map.of("error", true, "message", "Billing Failed: " + e.getMessage());
        }
    }

    // ==========================================
    // 4. MASTER ADMIN ENDPOINTS
    // ==========================================

    @GetMapping("/admin/stats")
    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("activeEmergencies", db.queryForObject("SELECT COUNT(*) FROM EMERGENCY_REQUEST WHERE Status NOT IN ('Completed', 'Cancelled')", Integer.class));
            stats.put("standbyTechs", db.queryForObject("SELECT COUNT(*) FROM WORKER WHERE AvailabilityStatus = 'Standby'", Integer.class));
            stats.put("completedJobs", db.queryForObject("SELECT COUNT(*) FROM EMERGENCY_REQUEST WHERE Status = 'Completed'", Integer.class));
            Integer revenue = db.queryForObject("SELECT SUM(Amount) FROM PAYMENT WHERE TransactionStatus = 'Success'", Integer.class);
            stats.put("totalRevenue", revenue != null ? revenue : 0);
        } catch (Exception e) { }
        return stats;
    }

    @GetMapping("/admin/login")
    public Map<String, Object> adminLogin(@RequestParam String username, @RequestParam String password) {
        String sql = "SELECT * FROM ADMIN WHERE Username = ? AND PasswordHash = SHA2(?, 256)";
        List<Map<String, Object>> result = db.queryForList(sql, username, password);
        return result.isEmpty() ? Map.of("error", "Invalid Admin Credentials") : result.get(0);
    }

    @GetMapping("/admin/feed")
    public List<Map<String, Object>> getGlobalFeed() {
        String sql = "SELECT r.RequestID, u.Name AS CustomerName, r.Category, w.Name AS WorkerName, r.Status " +
                     "FROM EMERGENCY_REQUEST r " +
                     "JOIN USER u ON r.UserID = u.UserID " +
                     "LEFT JOIN DISPATCH_ASSIGNMENT d ON r.RequestID = d.RequestID " +
                     "LEFT JOIN WORKER w ON d.WorkerID = w.WorkerID " +
                     "ORDER BY r.RequestID DESC LIMIT 15";
        return db.queryForList(sql);
    }

    @GetMapping("/admin/workers")
    public List<Map<String, Object>> getAllWorkers(@RequestParam(required = false, defaultValue = "All") String status) {
        String sql = "SELECT w.WorkerID, w.Name, w.Pro_Speciality, w.AvailabilityStatus, m.LocationAddress, " +
                     "v.BaseSalary, v.TotalPenalties, v.PenaltyAmount, v.FinalMonthlyPay " +
                     "FROM WORKER w " +
                     "JOIN MICRO_HUB m ON w.HubID = m.HubID " +
                     "JOIN vw_WorkerMonthlyPay v ON w.WorkerID = v.WorkerID ";
        if (!status.equals("All")) {
            sql += "WHERE w.AvailabilityStatus = ? ORDER BY w.WorkerID ASC";
            return db.queryForList(sql, status);
        }
        return db.queryForList(sql + "ORDER BY w.WorkerID ASC");
    }

    @GetMapping("/admin/cancellations")
    public List<Map<String, Object>> getCancellations() {
        String sql = "SELECT c.LogID, c.RequestID, u.Name AS CustomerName, c.CancelledBy, w.Name AS WorkerName, c.Reason, c.CancelDate " +
                     "FROM CANCELLATION_AUDIT c " +
                     "JOIN USER u ON c.UserID = u.UserID " +
                     "LEFT JOIN WORKER w ON c.WorkerID = w.WorkerID " +
                     "ORDER BY c.CancelDate DESC";
        return db.queryForList(sql);
    }

    // FIX: Using the procedure to safely transition status
    @PostMapping("/admin/worker/force-status")
    public Map<String, Object> forceWorkerStatus(@RequestParam int workerId, @RequestParam String status) {
        try {
            db.update("CALL sp_UpdateWorkerStatus(?, ?)", workerId, status);
            return Map.of("success", true, "message", "Worker " + workerId + " shifted to " + status);
        } catch (Exception e) {
            return Map.of("error", true, "message", "Failed to force status update. They may be busy.");
        }
    }

    @GetMapping("/admin/restock-queue")
    public List<Map<String, Object>> getRestockQueue() {
        String sql = "SELECT r.RequestID, mh.LocationAddress, p.Name AS PartName, r.Status, r.RequestDate " +
                     "FROM RESTOCK_REQUEST r " +
                     "JOIN MICRO_HUB mh ON r.HubID = mh.HubID " +
                     "JOIN PART p ON r.PartID = p.PartID " +
                     "WHERE r.Status = 'Pending' " +
                     "ORDER BY r.RequestDate ASC";
        return db.queryForList(sql);
    }

    @PostMapping("/admin/restock-fulfill")
    public Map<String, Object> fulfillRestock(@RequestParam int requestId, @RequestParam int quantity) {
        try {
            String sql = "CALL sp_FulfillRestock(?, ?)";
            db.update(sql, requestId, quantity);
            return Map.of("success", true, "message", "Inventory updated and request closed.");
        } catch (Exception e) {
            return Map.of("error", true, "message", "Fulfillment failed: " + e.getMessage());
        }
    }

    @Transactional 
    @PostMapping("/admin/worker/add")
    public Map<String, Object> addWorker(
            @RequestParam String name, @RequestParam String contact, @RequestParam double salary,
            @RequestParam String speciality, @RequestParam int hubId, @RequestParam String vReg,      
            @RequestParam String vModel, @RequestParam boolean cleaning) { 
        try {
            String sqlWorker = "CALL sp_AddWorker(?, ?, ?, ?, ?, ?, ?, ?)";
            db.update(sqlWorker, name, contact, salary, speciality, hubId, vReg, vModel, cleaning);
            Integer newWorkerId = db.queryForObject("SELECT WorkerID FROM WORKER WHERE Contact_Number = ?", Integer.class, contact);
            return Map.of("success", true, "message", "Worker hired! Default password is: worker" + newWorkerId);
        } catch (Exception e) {
            return Map.of("error", true, "message", "Database Error: " + e.getMessage());
        }
    }

    @GetMapping("/admin/payments")
    public List<Map<String, Object>> getAllPayments(@RequestParam(required = false, defaultValue = "All") String status) {
        String sql = "SELECT p.PaymentID, p.RequestID, u.Name AS CustomerName, r.IssueType, p.Amount, p.TransactionStatus " +
                     "FROM PAYMENT p JOIN EMERGENCY_REQUEST r ON p.RequestID = r.RequestID " +
                     "JOIN USER u ON r.UserID = u.UserID ";
        if (!status.equals("All")) {
            sql += "WHERE p.TransactionStatus = ? ORDER BY p.PaymentID DESC";
            return db.queryForList(sql, status);
        }
        return db.queryForList(sql + "ORDER BY p.PaymentID DESC");
    }

    @GetMapping("/admin/inventory")
    public List<Map<String, Object>> getAdminHubInventory(
            @RequestParam(required = false, defaultValue = "All") String hubId, 
            @RequestParam(required = false, defaultValue = "") String partId) {
            
        String sql = "SELECT i.HubID, m.LocationAddress, i.PartID, p.Name AS PartName, p.Category, i.Quantity " +
                     "FROM HUB_INVENTORY i " +
                     "JOIN MICRO_HUB m ON i.HubID = m.HubID " +
                     "JOIN PART p ON i.PartID = p.PartID WHERE 1=1 ";
        
        List<Object> params = new ArrayList<>();
        
        if (!hubId.equals("All")) {
            sql += "AND i.HubID = ? ";
            params.add(Integer.parseInt(hubId));
        }
        if (!partId.isEmpty()) {
            sql += "AND i.PartID = ? ";
            params.add(Integer.parseInt(partId));
        }
        
        sql += "ORDER BY i.HubID ASC, i.PartID ASC";
        return db.queryForList(sql, params.toArray());
    }

    @GetMapping("/admin/complaints")
    public List<Map<String, Object>> getPendingComplaints() {
        String sql = "SELECT c.ComplaintID, c.RequestID, c.ComplaintText, c.CreatedAt, c.Status, " +
                     "u.Name AS CustomerName, r.IssueType, " +
                     "h.Name AS HelperName, h.HelperID " +
                     "FROM COMPLAINT c " +
                     "JOIN USER u ON c.UserID_or_WorkerID = u.UserID " +
                     "JOIN EMERGENCY_REQUEST r ON c.RequestID = r.RequestID " +
                     "LEFT JOIN HELPER h ON c.HelperID = h.HelperID " +
                     "WHERE c.Role = 'Customer' " +
                     "ORDER BY c.CreatedAt DESC";
        return db.queryForList(sql);
    }

    // ==========================================
    // 5. HELPER PORTAL ENDPOINTS
    // ==========================================

    @GetMapping("/helper/login")
    public Map<String, Object> helperLogin(@RequestParam int helperId, @RequestParam String password) {
        String sql = "SELECT * FROM HELPER WHERE HelperID = ? AND PasswordHash = SHA2(?, 256)";
        List<Map<String, Object>> result = db.queryForList(sql, helperId, password);
        return result.isEmpty() ? Map.of("error", "Invalid Helper ID or Password") : result.get(0);
    }

    @GetMapping("/helper/complaints")
    public List<Map<String, Object>> getHelperComplaints(@RequestParam int helperId) {
        String sql = "SELECT c.ComplaintID, c.RequestID, c.ComplaintText, c.ProofMedium, c.Status, c.CreatedAt, " +
                     "u.Name AS CustomerName, u.Phone AS CustomerPhone, u.Email AS CustomerEmail, r.IssueType " +
                     "FROM COMPLAINT c " +
                     "JOIN USER u ON c.UserID_or_WorkerID = u.UserID " +
                     "JOIN EMERGENCY_REQUEST r ON c.RequestID = r.RequestID " +
                     "WHERE c.HelperID = ? AND c.Status = 'Pending' " +
                     "ORDER BY c.CreatedAt ASC";
        return db.queryForList(sql, helperId);
    }

    @Transactional
    @PostMapping("/helper/complaint/approve")
    public Map<String, Object> helperApproveComplaint(@RequestParam int complaintId) {
        try {
            String sql = "CALL sp_ApproveAndDispatchRework(?)";
            db.update(sql, complaintId);
            return Map.of("success", true, "message", "Complaint Approved! Free rework dispatched.");
        } catch (Exception e) {
            // Catches the SQL 45000 error if another helper beat them to it
            return Map.of("error", true, "message", "Action Denied: " + e.getMessage());
        }
    }

   @PostMapping("/helper/complaint/reject")
    public Map<String, Object> helperRejectComplaint(@RequestParam int complaintId) {
        try {
            // Replaced the dangerous raw UPDATE with the safe Stored Procedure
            String sql = "CALL sp_RejectComplaint(?)";
            db.update(sql, complaintId);
            return Map.of("success", true, "message", "Complaint Rejected.");
        } catch (Exception e) {
            return Map.of("error", true, "message", "Action Denied: " + e.getMessage());
        }
    }
    
    @GetMapping("/check-cancellation")
    public Map<String, Object> checkCancellation(@RequestParam int requestId) {
        String sql = "SELECT CancelledBy, Reason FROM CANCELLATION_AUDIT WHERE RequestID = ?";
        List<Map<String, Object>> result = db.queryForList(sql, requestId);
        
        if (result.isEmpty()) {
            return Map.of("cancelled", false);
        }
        
        // If found, return the cancellation details
        Map<String, Object> response = new HashMap<>(result.get(0));
        response.put("cancelled", true);
        return response;
    }
}