import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class UserApp extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainContainer;

    // --- SESSION DATA ---
    private int currentUserId = -1;
    private String currentUserName = "";
    private int currentUserPolygon = -1;

    // --- REGISTRATION COMPONENTS ---
    private JTextField regName, regEmail, regPhone;
    private JComboBox<String> regZoneDrop;
    private JLabel locationDisplay;
    private double fetchedLat = 0.0, fetchedLon = 0.0;
    private boolean locationFetched = false;

    // --- DASHBOARD COMPONENTS ---
    private JLabel welcomeLabel;
    private DefaultTableModel trackModel, historyModel;
    private JTextField issueInput;
    private JComboBox<String> categoryDrop;
    
    // --- PAYMENT COMPONENTS ---
    private JTextField payReqId, payAmount;
    private JTextField locationInput; 
    public UserApp() {
        setTitle("Res-Q : Customer Portal");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // Add our three main screens
        mainContainer.add(createLoginPanel(), "LOGIN");
        mainContainer.add(createRegisterPanel(), "REGISTER");
        mainContainer.add(createDashboardPanel(), "DASHBOARD");

        add(mainContainer);
        cardLayout.show(mainContainer, "LOGIN");
        setVisible(true);
    }

    // ==========================================
    // SCREEN 1: LOGIN
    // ==========================================
    private JPanel createLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(236, 240, 241));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;

        JLabel title = new JLabel("Welcome to Res-Q");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        p.add(title, gbc);

        gbc.gridy++; gbc.gridwidth = 1;
        p.add(new JLabel("Enter User ID:"), gbc);

        gbc.gridx = 1;
        JTextField loginIdField = new JTextField(10);
        p.add(loginIdField, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        JButton btnLogin = new JButton("Secure Login");
        btnLogin.setBackground(new Color(41, 128, 185));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.addActionListener(e -> attemptLogin(loginIdField.getText()));
        p.add(btnLogin, gbc);

        gbc.gridy++;
        JButton btnGoRegister = new JButton("New User? Register Here");
        btnGoRegister.addActionListener(e -> cardLayout.show(mainContainer, "REGISTER"));
        p.add(btnGoRegister, gbc);

        return p;
    }

    // ==========================================
    // SCREEN 2: REGISTRATION (With Zones & GPS)
    // ==========================================
    private JPanel createRegisterPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel title = new JLabel("New Customer Registration");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        p.add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy++;
        gbc.gridx = 0; p.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1; regName = new JTextField(15); p.add(regName, gbc);

        gbc.gridy++;
        gbc.gridx = 0; p.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; regEmail = new JTextField(15); p.add(regEmail, gbc);

        gbc.gridy++;
        gbc.gridx = 0; p.add(new JLabel("Phone (10 digits):"), gbc);
        gbc.gridx = 1; regPhone = new JTextField(15); p.add(regPhone, gbc);

        gbc.gridy++;
        gbc.gridx = 0; p.add(new JLabel("Select Zone:"), gbc);
        gbc.gridx = 1; 
        // Mapping exactly to Polygon IDs 1, 2, 3, 4
        String[] zones = {
            "1 - North Delhi Zone", 
            "2 - South Delhi Zone", 
            "3 - West Delhi Zone", 
            "4 - East Delhi Zone"
        };
        regZoneDrop = new JComboBox<>(zones);
        p.add(regZoneDrop, gbc);

        gbc.gridy++;
        gbc.gridx = 0; 
        JButton btnFetchLoc = new JButton("📍 Fetch Current GPS Location");
        btnFetchLoc.setBackground(new Color(241, 196, 15));
        btnFetchLoc.addActionListener(e -> generateLocation());
        p.add(btnFetchLoc, gbc);
        gbc.gridx = 1; 
        locationDisplay = new JLabel("Location not set");
        locationDisplay.setForeground(Color.RED);
        p.add(locationDisplay, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        JButton btnRegister = new JButton("Register & Auto-Login");
        btnRegister.setBackground(new Color(39, 174, 96));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.addActionListener(e -> handleRegistration());
        p.add(btnRegister, gbc);

        gbc.gridy++;
        JButton btnBack = new JButton("Back to Login");
        btnBack.addActionListener(e -> cardLayout.show(mainContainer, "LOGIN"));
        p.add(btnBack, gbc);

        return p;
    }

    // ==========================================
    // SCREEN 3: DASHBOARD (Requests & Payments)
    // ==========================================
    private JPanel createDashboardPanel() {
        JPanel p = new JPanel(new BorderLayout());

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBackground(new Color(44, 62, 80));
        welcomeLabel = new JLabel("Welcome, User!");
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        header.add(welcomeLabel);

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> {
            currentUserId = -1;
            cardLayout.show(mainContainer, "LOGIN");
        });
        header.add(btnLogout);
        p.add(header, BorderLayout.NORTH);

        JTabbedPane userTabs = new JTabbedPane();
        userTabs.setFont(new Font("SansSerif", Font.BOLD, 14));

        // TAB 1: Log New Request
        // TAB 1: Log New Request
        JPanel reqPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        reqPanel.add(new JLabel("Category:"));
        categoryDrop = new JComboBox<>(new String[]{"Plumbing", "Electrical", "Both"});
        reqPanel.add(categoryDrop);
        
        reqPanel.add(new JLabel("Describe Issue:"));
        issueInput = new JTextField(15);
        reqPanel.add(issueInput);
        
        // NEW LOCATION FIELD
        reqPanel.add(new JLabel("Exact Location (e.g. 2nd Floor Bath):"));
        locationInput = new JTextField(15);
        reqPanel.add(locationInput);
        
        JButton btnSubmitReq = new JButton("🚨 Request Immediate Help");
        btnSubmitReq.setBackground(new Color(231, 76, 60));
        btnSubmitReq.setForeground(Color.WHITE);
        btnSubmitReq.addActionListener(e -> logNewEmergency());
        reqPanel.add(btnSubmitReq);
        userTabs.addTab("Log New Emergency", reqPanel);
        // TAB 2: Live Tracking & Payment Portal
        JPanel trackPanel = new JPanel(new BorderLayout());
        trackModel = new DefaultTableModel();
        JTable trackTable = new JTable(trackModel);
        trackTable.setRowHeight(30);
        trackPanel.add(new JScrollPane(trackTable), BorderLayout.CENTER);
        
        // Payment Sub-Panel
        JPanel payPanel = new JPanel(new FlowLayout());
        payPanel.setBorder(BorderFactory.createTitledBorder("Pending Bills & Payments"));
        payPanel.add(new JLabel("ReqID to Pay:"));
        payReqId = new JTextField(4); payPanel.add(payReqId);
        payPanel.add(new JLabel("Amount (₹):"));
        payAmount = new JTextField(6); payPanel.add(payAmount);
        
        JButton btnPay = new JButton("Pay & Complete Job");
        btnPay.setBackground(new Color(39, 174, 96));
        btnPay.setForeground(Color.WHITE);
        btnPay.addActionListener(e -> processPayment());
        payPanel.add(btnPay);
        
        JButton btnRefreshTrack = new JButton("🔄 Refresh Tracker & Bills");
        btnRefreshTrack.addActionListener(e -> fetchTrackingData());
        payPanel.add(btnRefreshTrack);
        
        trackPanel.add(payPanel, BorderLayout.SOUTH);
        userTabs.addTab("📡 Track Requests & Pay", trackPanel);

        // TAB 3: History
        JPanel histPanel = new JPanel(new BorderLayout());
        historyModel = new DefaultTableModel();
        histPanel.add(new JScrollPane(new JTable(historyModel)), BorderLayout.CENTER);
        JButton btnRefreshHist = new JButton("🔄 Refresh History");
        btnRefreshHist.addActionListener(e -> fetchHistoryData());
        histPanel.add(btnRefreshHist, BorderLayout.SOUTH);
        userTabs.addTab("📜 Past Requests", histPanel);

        p.add(userTabs, BorderLayout.CENTER);
        return p;
    }

    // ==========================================
    // LOGIC & DATABASE EXECUTION
    // ==========================================

    private void generateLocation() {
        // Generates random distinct Delhi coordinates
        fetchedLat = 28.5 + (Math.random() * (28.8 - 28.5)); 
        fetchedLon = 77.0 + (Math.random() * (77.3 - 77.0));
        locationFetched = true;
        locationDisplay.setText(String.format("GPS Locked: Lat %.4f, Lon %.4f", fetchedLat, fetchedLon));
        locationDisplay.setForeground(new Color(39, 174, 96));
    }

    private void attemptLogin(String idStr) {
        if (idStr.isEmpty()) return;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT UserID, Name, Polygon_ID FROM USER WHERE UserID = ?")) {
            pstmt.setInt(1, Integer.parseInt(idStr));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                currentUserId = rs.getInt("UserID");
                currentUserName = rs.getString("Name");
                currentUserPolygon = rs.getInt("Polygon_ID");
                
                welcomeLabel.setText("Welcome, " + currentUserName + " | Zone ID: " + currentUserPolygon);
                fetchTrackingData();
                fetchHistoryData();
                cardLayout.show(mainContainer, "DASHBOARD");
            } else {
                JOptionPane.showMessageDialog(this, "User ID not found! Please register.");
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Login Error: " + e.getMessage()); }
    }

    private void handleRegistration() {
        if (!locationFetched) {
            JOptionPane.showMessageDialog(this, "Please click 'Fetch Current GPS Location' first!");
            return;
        }

        String sql = "INSERT INTO USER (Name, Email, Phone, Latitude, Longitude, Polygon_ID) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, regName.getText());
            pstmt.setString(2, regEmail.getText());
            pstmt.setString(3, regPhone.getText());
            pstmt.setDouble(4, fetchedLat);
            pstmt.setDouble(5, fetchedLon);
            
            // Extract Zone ID (1, 2, 3, or 4) from the dropdown string
            String selectedZone = (String) regZoneDrop.getSelectedItem();
            int polyId = Integer.parseInt(selectedZone.substring(0, 1));
            pstmt.setInt(6, polyId);

            pstmt.executeUpdate();

            // Auto-login logic using the generated UserID
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int newId = rs.getInt(1);
                JOptionPane.showMessageDialog(this, "Success! Your new UserID is: " + newId + "\nPlease remember this for future logins.");
                attemptLogin(String.valueOf(newId)); 
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Registration Error: " + e.getMessage()); }
    }

    private void logNewEmergency() {
        if (issueInput.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please describe the issue.");
            return;
        }
        
        // Now calling with 4 parameters!
        String sql = "{call sp_AutoDispatch(?, ?, ?, ?)}";
        try (Connection conn = DBConnection.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {
            
            cstmt.setInt(1, currentUserId);
            cstmt.setString(2, issueInput.getText());
            cstmt.setString(3, (String) categoryDrop.getSelectedItem());
            
            // Pass the location, or a default string if they left it blank
            String loc = locationInput.getText().isEmpty() ? "Registered Address" : locationInput.getText();
            cstmt.setString(4, loc);
            
            cstmt.execute();
            
            JOptionPane.showMessageDialog(this, "Emergency logged! Check the Tracking tab for updates.");
            issueInput.setText("");
            locationInput.setText(""); // Clear it
            fetchTrackingData(); 
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Dispatch Error: " + e.getMessage()); }
    }

    private void processPayment() {
        if (payReqId.getText().isEmpty() || payAmount.getText().isEmpty()) return;
        
        // This query checks if the amount matches the billed amount and updates it to Success
        String sql = "UPDATE PAYMENT SET TransactionStatus = 'Success' WHERE RequestID = ? AND Amount = ? AND TransactionStatus = 'Pending'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(payReqId.getText()));
            pstmt.setDouble(2, Double.parseDouble(payAmount.getText()));
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Payment Successful! \nJob marked as Completed and Worker released.");
                payReqId.setText(""); payAmount.setText("");
                fetchTrackingData();
                fetchHistoryData();
            } else {
                JOptionPane.showMessageDialog(this, "Payment Failed. Please check the Request ID and Amount match the Pending Bill exactly.");
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Payment Error: " + e.getMessage()); }
    }

    private void fetchTrackingData() {
        // Shows the Request Status, Assigned Worker (if any), and Pending Bill Amount
        String sql = "SELECT er.RequestID, er.Category, er.IssueType, er.Status AS ReqStatus, " +
                     "w.Name AS Technician, p.Amount AS Pending_Bill_Amount, p.TransactionStatus AS Bill_Status " +
                     "FROM EMERGENCY_REQUEST er " +
                     "LEFT JOIN DISPATCH_ASSIGNMENT da ON er.RequestID = da.RequestID " +
                     "LEFT JOIN WORKER w ON da.WorkerID = w.WorkerID " +
                     "LEFT JOIN PAYMENT p ON er.RequestID = p.RequestID " +
                     "WHERE er.UserID = ? AND er.Status IN ('Pending', 'EnRoute')";
        populateTable(sql, trackModel);
    }

    private void fetchHistoryData() {
        String sql = "SELECT er.RequestID, er.Category, er.IssueType, er.Timestamp, er.Status, p.Amount AS Paid_Amount " +
                     "FROM EMERGENCY_REQUEST er " +
                     "LEFT JOIN PAYMENT p ON er.RequestID = p.RequestID " +
                     "WHERE er.UserID = ? AND er.Status IN ('Completed', 'Cancelled') ORDER BY er.Timestamp DESC";
        populateTable(sql, historyModel);
    }

    private void populateTable(String sql, DefaultTableModel model) {
        if (currentUserId == -1) return;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            ResultSet rs = pstmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            
            model.setRowCount(0); model.setColumnCount(0);
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                model.addColumn(meta.getColumnLabel(i));
            }
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    Object val = rs.getObject(i);
                    row.add(val == null ? "N/A" : val);
                }
                model.addRow(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UserApp::new);
    }
}